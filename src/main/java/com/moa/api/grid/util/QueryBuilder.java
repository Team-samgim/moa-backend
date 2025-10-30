package com.moa.api.grid.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class QueryBuilder {

    private final ObjectMapper mapper = new ObjectMapper();
    private static final String APP_TZ = "Asia/Seoul";
    private static final boolean TS_WO_TZ_IS_UTC = true;

    // 컬럼별 원시 DB타입 맵: "timestamptz" | "timestamp" | "date" ...
    private String toKstDateExpr(String field, String rawTemporalKind) {
        String q = "\"" + field + "\"";
        if ("timestamptz".equals(rawTemporalKind)) {
            // timestamptz → 지역시간으로 바꾼 뒤 date
            return "(" + q + " AT TIME ZONE '" + APP_TZ + "')::date";
        }
        if ("timestamp".equals(rawTemporalKind)) {
            // tz 없는 timestamp를 UTC로 저장했다고 가정하는 경우
            if (TS_WO_TZ_IS_UTC) {
                return "((" + q + " AT TIME ZONE 'UTC') AT TIME ZONE '" + APP_TZ + "')::date";
            } else {
                // 로컬 의미의 naive timestamp라면 단순 ::date 도 가능 (권장 X)
                return q + "::date";
            }
        }
        // date 타입이면 그대로
        return q + "::date";
    }

    /** ✅ 테이블명 매핑 */
    public String resolveTableName(String layer) {
        return switch (layer.toLowerCase()) {
            case "http_page" -> "page_sample";
            case "http_uri"  -> "uri_sample";
            case "l4_tcp"    -> "tcp_sample";
            default          -> "ethernet_sample";
        };
    }

    // 타입맵 + 원시시간타입맵 받기
    public String buildSelectSQL(String layer, String sortField, String sortDirection,
                                 String filterModel, int offset, int limit,
                                 Map<String, String> typeMap,
                                 Map<String, String> rawTemporalKindMap) {

        String tableName = resolveTableName(layer);
        StringBuilder sql = new StringBuilder("SELECT * FROM " + tableName);

        String where = buildWhereClause(filterModel, typeMap, rawTemporalKindMap);
        if (!where.isEmpty()) sql.append(" WHERE ").append(where);

        if (sortField != null && !sortField.isBlank() && sortDirection != null && !sortDirection.isBlank()) {
            String safeSortField = sortField.contains("-") ? sortField.split("-")[0] : sortField;
            sql.append(" ORDER BY ").append("\"").append(safeSortField).append("\" ").append(sortDirection);
        }

        if (limit > 0) sql.append(" OFFSET ").append(offset).append(" LIMIT ").append(limit);
        log.info("[QueryBuilder] ✅ SQL generated: {}", sql);
        return sql.toString();
    }

    // =========================
    // WHERE
    // =========================
    public String buildWhereClause(String filterModel,
                                   Map<String,String> typeMap,
                                   Map<String,String> rawTemporalKindMap) {
        if (filterModel == null || filterModel.isEmpty()) return "";
        List<String> whereClauses = new ArrayList<>();
        try {
            JsonNode filters = mapper.readTree(filterModel);
            for (Iterator<String> it = filters.fieldNames(); it.hasNext();) {
                String field = it.next();
                JsonNode node = filters.get(field);
                String mode = node.path("mode").asText("");
                String safeField = field.contains("-") ? field.split("-")[0] : field;

                String type = resolveTypeForField(field, node, typeMap);

                if ("checkbox".equals(mode)) {
                    ArrayNode values = (ArrayNode) node.get("values");
                    if (values != null && !values.isEmpty()) {
                        if ("date".equalsIgnoreCase(type)) {
                            String rawKind = Optional.ofNullable(rawTemporalKindMap.get(safeField)).orElse("timestamp");
                            whereClauses.add(buildDateInClause(safeField, values, rawKind));
                        } else {
                            whereClauses.add(buildInClauseWithType(safeField, values, type));
                        }
                    }
                } else if ("condition".equals(mode)) {
                    ArrayNode conditions = (ArrayNode) node.get("conditions");
                    ArrayNode logicOps = (ArrayNode) node.get("logicOps");
                    List<String> exprs = new ArrayList<>();

                    for (int i = 0; i < conditions.size(); i++) {
                        JsonNode cond = conditions.get(i);
                        String op  = cond.path("op").asText();
                        String val = cond.path("val").asText();
                        if (val == null || val.isBlank()) continue;

                        String expr = buildConditionExpression(safeField, op, val, type,
                                rawTemporalKindMap.getOrDefault(safeField, "timestamp"));
                        if (expr != null) exprs.add(expr);
                    }
                    if (!exprs.isEmpty()) {
                        String whereExpr = exprs.get(0);
                        for (int i = 1; i < exprs.size(); i++) {
                            String logic = logicOps != null && logicOps.size() > i - 1
                                    ? logicOps.get(i - 1).asText("AND") : "AND";
                            whereExpr += " " + logic + " " + exprs.get(i);
                        }
                        whereClauses.add("(" + whereExpr + ")");
                    }
                }
            }
        } catch (Exception e) {
            log.error("[QueryBuilder] WHERE 절 생성 오류", e);
        }
        return String.join(" AND ", whereClauses);
    }

    /** ✅ 조건식 생성 (string/number/date/ip/mac/boolean/json) */
    private String buildConditionExpression(String field, String op, String val,
                                            String type, String rawTemporalKind) {
        String t = (type == null || type.isBlank()) ? "string" : type.toLowerCase();

        if (t.equals("ip") || t.equals("mac")) {
            return switch (op) {
                case "equals"     -> "\"" + field + "\"::text = '" + val + "'";
                case "startsWith" -> "\"" + field + "\"::text ILIKE '" + val + "%'";
                case "endsWith"   -> "\"" + field + "\"::text ILIKE '%" + val + "'";
                case "contains"   -> "\"" + field + "\"::text ILIKE '%" + val + "%'";
                default -> null;
            };
        }

        if (t.equals("number")) {
            return switch (op) {
                case "=", "equals" -> "\"" + field + "\" = " + val;
                case ">", "<", ">=", "<=" -> "\"" + field + "\" " + op + " " + val;
                case "contains" -> "\"" + field + "\"::text ILIKE '%" + val + "%'";
                default -> null;
            };
        }

        if (t.equals("date")) {
            String kstDate = toKstDateExpr(field, rawTemporalKind);
            return switch (op) {
                case "equals"   -> kstDate + " = DATE '" + val + "'";
                case "before"   -> kstDate + " < DATE '" + val + "'";
                case "after"    -> kstDate + " > DATE '" + val + "'";
                case "between"  -> {
                    String[] parts = val.split(",");
                    yield (parts.length == 2)
                            ? kstDate + " BETWEEN DATE '" + parts[0].trim() + "' AND DATE '" + parts[1].trim() + "'"
                            : null;
                }
                case "contains" -> "\"" + field + "\"::text ILIKE '%" + val + "%'";
                default -> null;
            };
        }

        // string 기본
        return switch (op) {
            case "contains"   -> "\"" + field + "\"::text ILIKE '%" + val + "%'";
            case "equals"     -> "\"" + field + "\"::text = '" + val + "'";
            case "startsWith" -> "\"" + field + "\"::text ILIKE '" + val + "%'";
            case "endsWith"   -> "\"" + field + "\"::text ILIKE '%" + val + "'";
            default -> null;
        };
    }

    // 타입 결정: 프론트 전달(type) > DB맵 > 기본 string
    private String resolveTypeForField(String field, JsonNode node, Map<String,String> typeMap) {
        String t = node.path("type").asText(null);
        if (t != null && !t.isBlank()) return t.toLowerCase();
        String safeField = field.contains("-") ? field.split("-")[0] : field;
        String fromMap = typeMap != null ? typeMap.get(safeField) : null;
        if (fromMap != null && !fromMap.isBlank()) return fromMap.toLowerCase();
        return "string";
    }

    // =========================
    // DISTINCT
    // =========================
    public String buildDistinctSQL(String layer, String column, String filterModel,
                                   boolean includeSelf,
                                   Map<String,String> typeMap,
                                   Map<String,String> rawTemporalKindMap) {
        String tableName = resolveTableName(layer);

        String whereClause = includeSelf
                ? buildWhereClause(filterModel, typeMap, rawTemporalKindMap)
                : buildWhereClauseExcludingField(filterModel, column, typeMap, rawTemporalKindMap);

        String colType = Optional.ofNullable(typeMap.get(column)).orElse("").toLowerCase();
        String rawKind  = Optional.ofNullable(rawTemporalKindMap.get(column)).orElse("timestamp");

        // ✅ 날짜형이면 KST DATE 표현식으로 선택
        String selectExpr = colType.equals("date")
                ? toKstDateExpr(column, rawKind) + "::text"
                : "\"" + column + "\"::text";

        String notNullExpr = colType.equals("date")
                ? toKstDateExpr(column, rawKind)
                : "\"" + column + "\"";

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT ").append(selectExpr).append(" AS value FROM ").append(tableName);

        if (whereClause != null && !whereClause.isBlank()) {
            sql.append(" WHERE ").append(whereClause).append(" AND ");
        } else {
            sql.append(" WHERE ");
        }

        sql.append(notNullExpr).append(" IS NOT NULL ");
        sql.append(" ORDER BY value ASC");

        log.info("[QueryBuilder] ✅ DISTINCT SQL generated: {}", sql);
        return sql.toString();
    }

    private String buildDateInClause(String field, ArrayNode values, String rawTemporalKind) {
        String kstDate = toKstDateExpr(field, rawTemporalKind);
        StringBuilder sb = new StringBuilder();
        sb.append(kstDate).append(" IN (");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("DATE '").append(values.get(i).asText()).append("'");
        }
        sb.append(")");
        return sb.toString();
    }

    private String buildWhereClauseExcludingField(String filterModel,
                                                  String excludeField,
                                                  Map<String,String> typeMap,
                                                  Map<String,String> rawTemporalKindMap) {
        if (filterModel == null || filterModel.isEmpty()) return "";

        List<String> whereClauses = new ArrayList<>();
        try {
            JsonNode filters = mapper.readTree(filterModel);

            // exclude 비교를 위해 raw/SAFE 둘 다 대비
            String excludeSafe = excludeField.contains("-") ? excludeField.split("-")[0] : excludeField;

            for (Iterator<String> it = filters.fieldNames(); it.hasNext();) {
                String field = it.next();
                String safeField = field.contains("-") ? field.split("-")[0] : field;

                // 자기 자신(같은 필드)이면 스킵
                if (safeField.equals(excludeSafe)) continue;

                JsonNode node = filters.get(field);
                String mode = node.path("mode").asText("");

                // 타입 결정: 프론트 전달(type) > DB맵 > 기본 string
                String type = resolveTypeForField(field, node, typeMap);

                if ("checkbox".equals(mode)) {
                    ArrayNode values = (ArrayNode) node.get("values");
                    if (values != null && values.size() > 0) {
                        if ("date".equalsIgnoreCase(type)) {
                            String rawKind = Optional.ofNullable(rawTemporalKindMap.get(safeField)).orElse("timestamp");
                            whereClauses.add(buildDateInClause(safeField, values, rawKind)); // ✅ KST DATE IN(...)
                        } else {
                            whereClauses.add(buildInClauseWithType(safeField, values, type));
                        }
                    }
                } else if ("condition".equals(mode)) {
                    ArrayNode conditions = (ArrayNode) node.get("conditions");
                    ArrayNode logicOps   = (ArrayNode) node.get("logicOps");
                    List<String> exprs   = new ArrayList<>();

                    for (int i = 0; i < conditions.size(); i++) {
                        JsonNode cond = conditions.get(i);
                        String op  = cond.path("op").asText();
                        String val = cond.path("val").asText();
                        if (val == null || val.isBlank()) continue;

                        String rawTemporalKind = rawTemporalKindMap.getOrDefault(safeField, "timestamp");
                        String expr = buildConditionExpression(safeField, op, val, type, rawTemporalKind);
                        if (expr != null) exprs.add(expr);
                    }

                    if (!exprs.isEmpty()) {
                        String whereExpr = exprs.get(0);
                        for (int i = 1; i < exprs.size(); i++) {
                            String logic = (logicOps != null && logicOps.size() > i - 1)
                                    ? logicOps.get(i - 1).asText("AND") : "AND";
                            whereExpr += " " + logic + " " + exprs.get(i);
                        }
                        whereClauses.add("(" + whereExpr + ")");
                    }
                }
            }
        } catch (Exception e) {
            log.error("[QueryBuilder] buildWhereClauseExcludingField() 오류", e);
        }

        return String.join(" AND ", whereClauses);
    }

    // =========================
    // IN 절 (타입 고려)
    // =========================
    private String buildInClauseWithType(String field, ArrayNode values, String type) {
        String t = (type == null || type.isBlank()) ? "string" : type.toLowerCase();
        StringBuilder sb = new StringBuilder();

        if (t.equals("number")) {
            sb.append("\"").append(field).append("\" IN (");
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(values.get(i).asText()); // 숫자 그대로
            }
            sb.append(")");
            return sb.toString();
        }

        // 문자열/날짜/IP/MAC/기타 → ::text + quote
        sb.append("\"").append(field).append("\"::text IN (");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("'").append(values.get(i).asText()).append("'");
        }
        sb.append(")");
        return sb.toString();
    }
}
