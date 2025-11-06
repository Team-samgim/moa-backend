package com.moa.api.grid.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Stream;

import static com.moa.api.grid.util.LayerTableResolver.resolveDataTable;

@Slf4j
@Component
public class QueryBuilder {

    private final ObjectMapper mapper = new ObjectMapper();
    private static final String APP_TZ = "Asia/Seoul";
    private static final boolean TS_WO_TZ_IS_UTC = true;

    private String q(String col) {
        return "\"" + col.replace("\"", "\"\"") + "\"";
    }

    private String esc(String s) {
        return s == null ? "" : s.replace("'", "''");
    }

    private String normalizeJson(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
            try {
                return mapper.readValue(t, String.class);
            } catch (Exception ignore) {
            }
        }
        return t;
    }

    private String sanitizeOrderBy(String sortField, Map<String, String> typeMap, String fallback) {
        if (sortField == null || sortField.isBlank()) return fallback;
        String raw = sortField.contains("-") ? sortField.split("-")[0] : sortField;
        return (typeMap != null && typeMap.containsKey(raw)) ? raw : fallback;
    }

    private String sanitizeDir(String dir) {
        return "ASC".equalsIgnoreCase(dir) ? "ASC" : "DESC";
    }

    public String buildSelectSQLForExport(String layer,
                                          List<String> columns,
                                          String sortField,
                                          String sortDirection,
                                          String filterModel,
                                          String baseSpecJson,
                                          Map<String, String> typeMap,
                                          Map<String, String> rawTemporalKindMap) {

        String table = resolveTableName(layer);
        if (columns == null || columns.isEmpty()) throw new IllegalArgumentException("columns is required");

        String selectCols = String.join(", ", columns.stream()
                .map(c -> q(c.contains("-") ? c.split("-")[0] : c))
                .toList());

        StringBuilder sql = new StringBuilder("SELECT ").append(selectCols)
                .append(" FROM ").append(table).append(" t");

        // ✅ WHERE = baseSpec(시간+조건) AND 활성필터
        String whereBase = buildWhereFromBaseSpec(baseSpecJson, typeMap, rawTemporalKindMap);
        String whereFilter = buildWhereClause(filterModel, typeMap, rawTemporalKindMap);

        String where = Stream.of(whereBase, whereFilter)
                .filter(s -> s != null && !s.isBlank())
                .collect(java.util.stream.Collectors.joining(" AND "));

        if (!where.isEmpty()) sql.append(" WHERE ").append(where);

        String safeSort = sanitizeOrderBy(sortField, typeMap, "ts_server_nsec");
        String safeDir = sanitizeDir(sortDirection); // null이면 DESC 반환
        log.info("[QueryBuilder] Export 정렬: {} {}", safeSort, safeDir);
        sql.append(" ORDER BY ");
        if ("ts_server_nsec".equalsIgnoreCase(safeSort)) {
            sql.append("\"ts_server_nsec\"::numeric ").append(safeDir);
        } else {
            sql.append(q(safeSort)).append(" ").append(safeDir);
        }
        log.info("[QueryBuilder] Export SQL: {}", sql);
        return sql.toString();
    }

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

    /**
     * 테이블명 매핑
     */
    public String resolveTableName(String layer) {
        return resolveDataTable(layer);
    }

    // WHERE
    public String buildWhereClause(String filterModel,
                                   Map<String, String> typeMap,
                                   Map<String, String> rawTemporalKindMap) {
        if (filterModel == null || filterModel.isEmpty()) return "";
        List<String> whereClauses = new ArrayList<>();
        try {
            JsonNode filters = mapper.readTree(filterModel);
            for (Iterator<String> it = filters.fieldNames(); it.hasNext(); ) {
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
                        String op = cond.path("op").asText("");
                        String rawTemporalKind = rawTemporalKindMap.getOrDefault(safeField, "timestamp");

                        String expr = null;

                        // between 은 별도 처리 (val1/val2, min/max, from/to 지원)
                        if ("between".equalsIgnoreCase(op)) {
                            if ("date".equalsIgnoreCase(type)) {
                                String v1 = cond.path("val1").asText(null);
                                String v2 = cond.path("val2").asText(null);
                                if (v1 == null || v2 == null) {
                                    v1 = cond.path("from").asText(null);
                                    v2 = cond.path("to").asText(null);
                                }
                                if (v1 != null && v2 != null) {
                                    String kstDate = toKstDateExpr(safeField, rawTemporalKind);
                                    expr = kstDate + " BETWEEN DATE '" + esc(v1) + "' AND DATE '" + esc(v2) + "'";
                                }
                            } else if ("number".equalsIgnoreCase(type)) {
                                String a = cond.path("min").asText(null);
                                String b = cond.path("max").asText(null);
                                if (a == null || b == null) {
                                    a = cond.path("val1").asText(null);
                                    b = cond.path("val2").asText(null);
                                }
                                if (a != null && b != null) {
                                    expr = q(safeField) + " BETWEEN " + a + " AND " + b;
                                }
                            } else {
                                String a = cond.path("val1").asText(null);
                                String b = cond.path("val2").asText(null);
                                if (a != null && b != null) {
                                    expr = q(safeField) + "::text BETWEEN '" + esc(a) + "' AND '" + esc(b) + "'";
                                }
                            }
                        } else {
                            // 단일 값 연산자 처리 (contains/equals/startsWith/endsWith, 비교연산 등)
                            String val = cond.path("val").asText(null);
                            if (val != null && !val.isBlank()) {
                                expr = buildConditionExpression(safeField, op, val, type, rawTemporalKind);
                            }
                            // 필요 시 IS_NULL/IS_NOT_NULL 같은 무값 연산자 분기 추가 가능
                        }

                        if (expr != null && !expr.isBlank()) exprs.add(expr);

                        // 다음 식의 조인 연산자는 마지막에 한꺼번에 적용 (아래에서)
                    }

                    if (!exprs.isEmpty()) {
                        // 첫 항 뒤로 logicOps[i-1]를 끼워 넣음
                        StringBuilder whereExpr = new StringBuilder();
                        for (int i = 0; i < exprs.size(); i++) {
                            if (i == 0) {
                                whereExpr.append(exprs.get(i));
                            } else {
                                String logic = (logicOps != null && logicOps.size() > i - 1)
                                        ? logicOps.get(i - 1).asText("AND") : "AND";
                                whereExpr.append(" ").append(logic).append(" ").append(exprs.get(i));
                            }
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

    /**
     * 조건식 생성 (string/number/date/ip/mac/boolean/json)
     */
    private String buildConditionExpression(String field, String op, String val,
                                            String type, String rawTemporalKind) {
        String t = (type == null || type.isBlank()) ? "string" : type.toLowerCase();

        if (t.equals("ip") || t.equals("mac")) {
            return switch (op) {
                case "equals" -> "\"" + field + "\"::text = '" + val + "'";
                case "startsWith" -> "\"" + field + "\"::text ILIKE '" + val + "%'";
                case "endsWith" -> "\"" + field + "\"::text ILIKE '%" + val + "'";
                case "contains" -> "\"" + field + "\"::text ILIKE '%" + val + "%'";
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
                case "equals" -> kstDate + " = DATE '" + val + "'";
                case "before" -> kstDate + " < DATE '" + val + "'";
                case "after" -> kstDate + " > DATE '" + val + "'";
                case "between" -> {
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
            case "contains" -> "\"" + field + "\"::text ILIKE '%" + val + "%'";
            case "equals" -> "\"" + field + "\"::text = '" + val + "'";
            case "startsWith" -> "\"" + field + "\"::text ILIKE '" + val + "%'";
            case "endsWith" -> "\"" + field + "\"::text ILIKE '%" + val + "'";
            default -> null;
        };
    }

    // 타입 결정: 프론트 전달(type) > DB맵 > 기본 string
    private String resolveTypeForField(String field, JsonNode node, Map<String, String> typeMap) {
        String t = node.path("type").asText(null);
        if (t != null && !t.isBlank()) return t.toLowerCase();
        String safeField = field.contains("-") ? field.split("-")[0] : field;
        String fromMap = typeMap != null ? typeMap.get(safeField) : null;
        if (fromMap != null && !fromMap.isBlank()) return fromMap.toLowerCase();
        return "string";
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
                                                  Map<String, String> typeMap,
                                                  Map<String, String> rawTemporalKindMap) {
        if (filterModel == null || filterModel.isEmpty()) return "";

        List<String> whereClauses = new ArrayList<>();
        try {
            JsonNode filters = mapper.readTree(filterModel);

            // exclude 비교를 위해 raw/SAFE 둘 다 대비
            String excludeSafe = excludeField.contains("-") ? excludeField.split("-")[0] : excludeField;

            for (Iterator<String> it = filters.fieldNames(); it.hasNext(); ) {
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
                            whereClauses.add(buildDateInClause(safeField, values, rawKind)); // KST DATE IN(...)
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
                        String op = cond.path("op").asText();
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

    // IN 절 (타입 고려)
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

    public String buildDistinctPagedSQLOrdered(
            String layer, String column, String filterModel, boolean includeSelf,
            String search, int offset, int limit,
            String orderBy, String order, String baseSpecJson,
            Map<String, String> typeMap, Map<String, String> rawTemporalKindMap) {

        String table = resolveTableName(layer);

        // 안전한 컬럼명/정렬 보정
        String safeOrderBy = (orderBy != null && typeMap.containsKey(orderBy)) ? orderBy : "ts_server_nsec";
        String safeOrder = "ASC".equalsIgnoreCase(order) ? "ASC" : "DESC";

        // 1) 그리드와 동일 WHERE 만들기
        //    - baseSpec(time/conditions)  +  activeFilters(filterModel)
        String whereBase = buildWhereFromBaseSpec(baseSpecJson, typeMap, rawTemporalKindMap);
        String whereFilter = includeSelf
                ? buildWhereClause(filterModel, typeMap, rawTemporalKindMap)
                : buildWhereClauseExcludingField(filterModel, column, typeMap, rawTemporalKindMap);

        String where = Stream.of(whereBase, whereFilter)
                .filter(s -> s != null && !s.isBlank())
                .collect(java.util.stream.Collectors.joining(" AND "));

        // 2) 표시값/NULL제외/검색(prefix) 처리
        String colType = Optional.ofNullable(typeMap.get(column)).orElse("").toLowerCase();
        String rawKind = Optional.ofNullable(rawTemporalKindMap.get(column)).orElse("timestamp");

        String selectExpr = colType.equals("date")
                ? toKstDateExpr(column, rawKind) + "::text"
                : "\"" + column + "\"::text";
        String notNullExpr = colType.equals("date")
                ? toKstDateExpr(column, rawKind)
                : "\"" + column + "\"";

        StringBuilder sb = new StringBuilder();
        sb.append("WITH base AS (")
                .append(" SELECT ").append(selectExpr).append(" AS v, ")
                .append("t.").append(q(safeOrderBy)).append(" AS ord")
                .append(" FROM ").append(table).append(" t ");

        if (where != null && !where.isBlank()) sb.append(" WHERE ").append(where).append(" AND ");
        else sb.append(" WHERE ");
        sb.append(notNullExpr).append(" IS NOT NULL ");

        if (search != null && !search.isBlank()) {
            sb.append(" AND ").append(selectExpr)
                    .append(" ILIKE '").append(esc(search)).append("%' ");
        }
        sb.append(")")
                .append(", dedup AS (")
                .append(" SELECT v, MIN(ord) AS first_ord")
                .append(" FROM base GROUP BY v")
                .append(")")
                .append(" SELECT v FROM dedup")
                .append(" ORDER BY first_ord ").append(safeOrder)
                .append(" OFFSET ").append(Math.max(0, offset))
                .append(" LIMIT ").append(Math.max(1, limit));

        log.info("[Distinct WHERE] base={}, filters={}", whereBase, whereFilter);

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public String buildWhereFromBaseSpec(
            String baseSpecJson,
            Map<String, String> typeMap,
            Map<String, String> rawTemporalKindMap
    ) {
        if (baseSpecJson == null || baseSpecJson.isBlank()) return "";
        try {
            // 이중 stringify 대비
            String norm = normalizeJson(baseSpecJson);
            JsonNode root = mapper.readTree(norm);
            List<String> parts = new ArrayList<>();

            // 1) time: { field, fromEpoch, toEpoch, inclusive? }
            JsonNode time = root.path("time");
            if (time.isObject()) {
                String field = time.path("field").asText(null);
                Long from = time.hasNonNull("fromEpoch") ? time.get("fromEpoch").asLong() : null;
                Long to = time.hasNonNull("toEpoch") ? time.get("toEpoch").asLong() : null;
                boolean inclusive = time.path("inclusive").asBoolean(true);
                if (field != null && from != null && to != null) {
                    parts.add(buildTimeRangeClause(field, from, to, inclusive, typeMap, rawTemporalKindMap));
                }
            }

            // 2) conditions: [{join, field, op, values, dataType}]
            JsonNode conds = root.path("conditions");
            if (conds.isArray() && conds.size() > 0) {
                List<String> cParts = new ArrayList<>();
                for (int i = 0; i < conds.size(); i++) {
                    JsonNode c = conds.get(i);
                    String field = c.path("field").asText(null);
                    String op = c.path("op").asText(null);
                    String dt = c.path("dataType").asText(null);
                    if (field == null || op == null) continue;

                    String dataType = (dt == null || dt.isBlank())
                            ? Optional.ofNullable(typeMap.get(field)).orElse("TEXT").toUpperCase()
                            : dt.toUpperCase();

                    List<String> vals = new ArrayList<>();
                    JsonNode vArr = c.path("values");
                    if (vArr.isArray()) for (JsonNode v : vArr) vals.add(v.isNull() ? null : v.asText());

                    String expr = mapSearchDTOCondition(field, op.toUpperCase(), dataType, vals, rawTemporalKindMap);
                    if (expr == null || expr.isBlank()) continue;

                    String join = (i == 0) ? null : c.path("join").asText("AND").toUpperCase();
                    if (join != null) cParts.add(join);
                    cParts.add("(" + expr + ")");
                }
                if (!cParts.isEmpty()) {
                    String condExpr = String.join(" ", cParts);
                    boolean not = root.path("not").asBoolean(false); // SearchExecuteService의 global NOT
                    parts.add(not ? "NOT (" + condExpr + ")" : "(" + condExpr + ")");
                }
            }

            return String.join(" AND ", parts);
        } catch (Exception e) {
            log.warn("[QueryBuilder] buildWhereFromBaseSpec parse error: {}", e.getMessage());
            return "";
        }
    }

    public String buildTimeRangeClause(
            String field,
            long fromEpoch,
            long toEpoch,
            boolean inclusive,
            Map<String, String> typeMap,
            Map<String, String> rawTemporalKindMap
    ) {
        if (field == null) return "";
        String fType = typeMap != null ? typeMap.getOrDefault(field, "") : "";
        String raw = rawTemporalKindMap != null ? rawTemporalKindMap.getOrDefault(field, "") : "";

        String col = "t." + q(field);
        String ge = inclusive ? " >= " : " > ";
        String le = inclusive ? " <= " : " < ";

        if ("number".equalsIgnoreCase(fType)) {
            return "(" + col + ge + fromEpoch + " AND " + col + le + toEpoch + ")";
        }

        if ("timestamptz".equalsIgnoreCase(raw)) {
            return "(" + col + ge + "to_timestamp(" + fromEpoch + ") AND "
                    + col + le + "to_timestamp(" + toEpoch + "))";
        }
        if ("timestamp".equalsIgnoreCase(raw)) {
            return "((" + col + " AT TIME ZONE 'UTC')" + ge + "to_timestamp(" + fromEpoch + ") AND " +
                    "(" + col + " AT TIME ZONE 'UTC')" + le + "to_timestamp(" + toEpoch + "))";
        }
        if ("date".equalsIgnoreCase(fType)) {
            return "(" + col + ge + "to_timestamp(" + fromEpoch + ")::date AND "
                    + col + le + "to_timestamp(" + toEpoch + ")::date)";
        }
        return "";
    }

    private String mapSearchDTOCondition(String field, String op, String dataType,
                                         List<String> values, Map<String, String> rawTemporalKindMap) {
        String f = "\"" + field + "\"";
        switch (dataType) {
            case "TEXT":
            case "IP":
            case "MAC": {
                String val = values.isEmpty() ? "" : esc(values.get(0));
                return switch (op) {
                    case "LIKE" -> f + "::text ILIKE '%" + val + "%'";
                    case "STARTS_WITH" -> f + "::text ILIKE '" + val + "%'";
                    case "ENDS_WITH" -> f + "::text ILIKE '%" + val + "'";
                    case "EQ" -> f + " = '" + val + "'";
                    case "NE" -> f + " <> '" + val + "'";
                    case "IN" -> inListText(f, values);
                    case "IS_NULL" -> f + " IS NULL";
                    case "IS_NOT_NULL" -> f + " IS NOT NULL";
                    default -> null;
                };
            }
            case "NUMBER": {
                String v1 = values.isEmpty() ? null : values.get(0);
                String v2 = values.size() > 1 ? values.get(1) : null;
                return switch (op) {
                    case "EQ" -> f + " = " + v1;
                    case "NE" -> f + " <> " + v1;
                    case "GT" -> f + " > " + v1;
                    case "GTE" -> f + " >= " + v1;
                    case "LT" -> f + " < " + v1;
                    case "LTE" -> f + " <= " + v1;
                    case "BETWEEN" -> f + " BETWEEN " + v1 + " AND " + v2;
                    case "IN" -> inListNumber(f, values);
                    case "IS_NULL" -> f + " IS NULL";
                    case "IS_NOT_NULL" -> f + " IS NOT NULL";
                    default -> null;
                };
            }
            case "DATETIME": {
                String kind = rawTemporalKindMap.getOrDefault(field, "timestamp");
                String cast = "to_timestamp";
                String t1 = values.isEmpty() ? null : cast + "(" + values.get(0) + ")";
                String t2 = values.size() > 1 ? cast + "(" + values.get(1) + ")" : null;
                return switch (op) {
                    case "GTE" -> f + " >= " + t1;
                    case "LT" -> f + " < " + t1;
                    case "BETWEEN" -> f + " BETWEEN " + t1 + " AND " + t2;
                    case "IN" ->
                            f + " IN (" + values.stream().map(v -> cast + "(" + v + ")").collect(java.util.stream.Collectors.joining(", ")) + ")";
                    case "IS_NULL" -> f + " IS NULL";
                    case "IS_NOT_NULL" -> f + " IS NOT NULL";
                    default -> null;
                };
            }
            case "BOOLEAN": {
                return switch (op) {
                    case "IS_NULL" -> f + " IS NULL";
                    case "IS_NOT_NULL" -> f + " IS NOT NULL";
                    case "EQ" -> f + " = " + (values.isEmpty() ? "false" : values.get(0));
                    default -> null;
                };
            }
            default:
                return null;
        }
    }

    private String inListText(String f, List<String> values) {
        String joined = values.stream()
                .filter(Objects::nonNull)
                .map(v -> "'" + esc(v) + "'")
                .collect(java.util.stream.Collectors.joining(", "));
        return f + " IN (" + (joined.isBlank() ? "''" : joined) + ")";
    }

    private String inListNumber(String f, List<String> values) {
        String joined = values.stream()
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.joining(", "));
        return f + " IN (" + (joined.isBlank() ? "0" : joined) + ")";
    }
}
