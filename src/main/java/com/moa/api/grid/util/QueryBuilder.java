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

    /** 숫자형 컬럼 자동 캐스팅용 */
    private static final Set<String> NUMERIC_COLUMNS = Set.of(
            "vlan_id", "port", "seq", "src_port", "dst_port", "size", "id"
    );

    /** ✅ 메인 SELECT SQL 생성 */
    public String buildSelectSQL(String layer,
                                 String sortField,
                                 String sortDirection,
                                 String filterModel,
                                 int offset,
                                 int limit) {

        String tableName = resolveTableName(layer);
        StringBuilder sql = new StringBuilder("SELECT * FROM " + tableName);

        String where = buildWhereClause(filterModel);
        if (!where.isEmpty()) sql.append(" WHERE ").append(where);

        if (sortField != null && !sortField.isBlank() && sortDirection != null && !sortDirection.isBlank()) {
            String safeSortField = sortField.contains("-") ? sortField.split("-")[0] : sortField;
            sql.append(" ORDER BY ").append("\"").append(safeSortField).append("\" ").append(sortDirection);
        }

        if (limit > 0) sql.append(" OFFSET ").append(offset).append(" LIMIT ").append(limit);

        log.info("[QueryBuilder] ✅ SQL generated: {}", sql);
        return sql.toString();
    }

    /** ✅ 테이블명 매핑 */
    public String resolveTableName(String layer) {
        return switch (layer.toLowerCase()) {
            case "http_page" -> "page_sample";
            case "http_uri" -> "uri_sample";
            case "l4_tcp" -> "tcp_sample";
            default -> "ethernet_sample";
        };
    }

    /** ✅ WHERE 절 생성 */
    public String buildWhereClause(String filterModel) {
        if (filterModel == null || filterModel.isEmpty()) return "";

        List<String> whereClauses = new ArrayList<>();
        try {
            JsonNode filters = mapper.readTree(filterModel);

            for (Iterator<String> it = filters.fieldNames(); it.hasNext();) {
                String field = it.next();
                JsonNode node = filters.get(field);
                String mode = node.path("mode").asText("");
                String safeField = field.contains("-") ? field.split("-")[0] : field;

                // ✅ 체크박스 필터
                if ("checkbox".equals(mode)) {
                    ArrayNode values = (ArrayNode) node.get("values");
                    if (values != null && !values.isEmpty()) {
                        whereClauses.add("\"" + safeField + "\" IN " + buildInClause(values));
                    }
                }

                // ✅ 조건식 필터
                else if ("condition".equals(mode)) {
                    ArrayNode conditions = (ArrayNode) node.get("conditions");
                    ArrayNode logicOps = (ArrayNode) node.get("logicOps");
                    List<String> exprs = new ArrayList<>();

                    for (int i = 0; i < conditions.size(); i++) {
                        JsonNode cond = conditions.get(i);
                        String op = cond.path("op").asText();
                        String val = cond.path("val").asText();
                        if (val == null || val.isBlank()) continue;

                        String expr = buildConditionExpression(safeField, op, val);
                        if (expr != null) exprs.add(expr);
                    }

                    // ✅ 조건 연결 (AND / OR)
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
            log.error("[QueryBuilder] WHERE 절 생성 중 오류 발생", e);
        }

        return String.join(" AND ", whereClauses);
    }

    /** ✅ 조건식 생성 (string / number / date / ip / mac) */
    private String buildConditionExpression(String field, String op, String val) {
        String lower = field.toLowerCase();

        // IP, MAC 처리
        if (lower.contains("ip") || lower.contains("mac")) {
            return switch (op) {
                case "equals" -> "\"" + field + "\"::text = '" + val + "'";
                case "startsWith" -> "\"" + field + "\"::text ILIKE '" + val + "%'";
                case "endsWith" -> "\"" + field + "\"::text ILIKE '%" + val + "'";
                case "contains" -> "\"" + field + "\"::text ILIKE '%" + val + "%'";
                default -> null;
            };
        }

        // 숫자형
        if (NUMERIC_COLUMNS.contains(lower)) {
            return switch (op) {
                case "=", "equals" -> "\"" + field + "\" = " + val;
                case ">", "<", ">=", "<=" -> "\"" + field + "\" " + op + " " + val;
                case "contains" -> "\"" + field + "\"::text ILIKE '%" + val + "%'";
                default -> null;
            };
        }

        // 날짜형
        if (lower.contains("date") || lower.contains("time")) {
            return switch (op) {
                case "equals" -> "\"" + field + "\"::date = DATE '" + val + "'";
                case "before" -> "\"" + field + "\"::date < DATE '" + val + "'";
                case "after" -> "\"" + field + "\"::date > DATE '" + val + "'";
                case "between" -> buildBetweenExpression(field, val);
                default -> null;
            };
        }

        // 문자열형
        return switch (op) {
            case "contains" -> "\"" + field + "\" ILIKE '%" + val + "%'";
            case "equals" -> "\"" + field + "\" = '" + val + "'";
            case "startsWith" -> "\"" + field + "\" ILIKE '" + val + "%'";
            case "endsWith" -> "\"" + field + "\" ILIKE '%" + val + "'";
            default -> null;
        };
    }

    /** ✅ between 구문 처리 (날짜 범위) */
    private String buildBetweenExpression(String field, String val) {
        // val은 '2024-01-01,2024-01-31' 형태라고 가정
        String[] parts = val.split(",");
        if (parts.length == 2) {
            String start = parts[0].trim();
            String end = parts[1].trim();
            return "\"" + field + "\"::date BETWEEN DATE '" + start + "' AND DATE '" + end + "'";
        }
        return null;
    }

    // 기존 4인자 메서드는 그대로 두고,
    public String buildDistinctSQL(String layer, String column, String filterModel, boolean includeSelf) {
        String tableName = resolveTableName(layer);

        String whereClause = includeSelf
                ? buildWhereClause(filterModel)                // ✅ 자기자신 포함
                : buildWhereClauseExcludingField(filterModel, column); // 예전 동작(자기자신 제외)

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT \"").append(column).append("\"::text AS value FROM ").append(tableName);

        if (whereClause != null && !whereClause.isBlank()) {
            sql.append(" WHERE ").append(whereClause).append(" AND ");
        } else {
            sql.append(" WHERE ");
        }

        sql.append("\"").append(column).append("\" IS NOT NULL ");
        sql.append(" ORDER BY value ASC");

        log.info("[QueryBuilder] ✅ DISTINCT SQL generated: {}", sql);
        return sql.toString();
    }

    // ✅ 새로 추가: 뒤호환용 3인자 오버로드
    public String buildDistinctSQL(String layer, String column, String filterModel) {
        // 기본값을 includeSelf=true로: 조건필터 적용 시 자기자신 조건도 반영하여 목록 축소
        return buildDistinctSQL(layer, column, filterModel, true);
    }

    /** ✅ 자기 자신을 제외하고 WHERE 절 생성 */
    private String buildWhereClauseExcludingField(String filterModel, String excludeField) {
        if (filterModel == null || filterModel.isEmpty()) return "";

        List<String> whereClauses = new ArrayList<>();

        try {
            JsonNode filters = mapper.readTree(filterModel);

            for (Iterator<String> it = filters.fieldNames(); it.hasNext();) {
                String field = it.next();
                if (field.equals(excludeField)) continue; // ✅ 자기 자신 제외

                JsonNode node = filters.get(field);
                String mode = node.path("mode").asText("");
                String safeField = field.contains("-") ? field.split("-")[0] : field;

                if ("checkbox".equals(mode)) {
                    ArrayNode values = (ArrayNode) node.get("values");
                    if (values != null && !values.isEmpty()) {
                        whereClauses.add("\"" + safeField + "\" IN " + buildInClause(values));
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

                        // ✅ 숫자형 필드면 numeric 비교, 나머지는 문자열 비교
                        boolean isNumeric = safeField.matches(".*(id|port|seq|size|count).*");

                        String target = "\"" + safeField + "\"";
                        if (isNumeric) target += "::numeric";
                        else target += "::text";

                        switch (op) {
                            case "contains" -> exprs.add(target + " ILIKE '%" + val + "%'");
                            case "equals" -> exprs.add(target + " = '" + val + "'");
                            case "startsWith" -> exprs.add(target + " ILIKE '" + val + "%'");
                            case "endsWith" -> exprs.add(target + " ILIKE '%" + val + "'");
                            case ">" -> exprs.add(target + " > " + val);
                            case "<" -> exprs.add(target + " < " + val);
                            case ">=" -> exprs.add(target + " >= " + val);
                            case "<=" -> exprs.add(target + " <= " + val);
                            default -> log.warn("Unsupported operator: {}", op);
                        }
                    }

                    if (!exprs.isEmpty()) {
                        String whereExpr = exprs.get(0);
                        for (int i = 1; i < exprs.size(); i++) {
                            String logic = logicOps != null && logicOps.size() > i - 1
                                    ? logicOps.get(i - 1).asText("AND")
                                    : "AND";
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

    /** ✅ IN 절 생성 */
    private String buildInClause(ArrayNode values) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("'").append(values.get(i).asText()).append("'");
        }
        sb.append(")");
        return sb.toString();
    }

}
