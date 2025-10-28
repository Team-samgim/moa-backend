package com.moa.api.grid.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.postgresql.util.PGobject;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class MockSearchController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/mock-search")
    public Map<String, Object> getData(
            @RequestParam(defaultValue = "ethernet") String layer,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String filterModel
    ) {
        // layer별 테이블 매핑
        String tableName = switch (layer.toLowerCase()) {
            case "http_page" -> "page_sample";
            default -> "ethernet_sample";
        };

        StringBuilder sql = new StringBuilder("SELECT * FROM " + tableName);
        List<Object> params = new ArrayList<>();

        // 필터링 처리
        if (filterModel != null && !filterModel.isBlank()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Map<String, Object>> filters = mapper.readValue(filterModel, Map.class);
                List<String> conditions = new ArrayList<>();

                for (String col : filters.keySet()) {
                    Map<String, Object> filter = filters.get(col);
                    String type = (String) filter.get("type");

                    if ("set".equalsIgnoreCase(type)) {
                        List<?> values = (List<?>) filter.get("values");
                        if (values != null && !values.isEmpty()) {
                            String inClause = values.stream()
                                    .map(Object::toString)
                                    .map(v -> "'" + v + "'")
                                    .collect(Collectors.joining(","));
                            conditions.add("\"" + col + "\" IN (" + inClause + ")");
                        }
                    } else {
                        String filterValue = (String) filter.get("filter");
                        if (filterValue == null || filterValue.isBlank()) continue;
                        switch (type) {
                            case "contains" -> conditions.add("\"" + col + "\" ILIKE '%" + filterValue + "%'");
                            case "equals" -> conditions.add("\"" + col + "\" = '" + filterValue + "'");
                            case "startsWith" -> conditions.add("\"" + col + "\" ILIKE '" + filterValue + "%'");
                            case "endsWith" -> conditions.add("\"" + col + "\" ILIKE '%" + filterValue + "'");
                        }
                    }
                }
                if (!conditions.isEmpty()) {
                    sql.append(" WHERE ").append(String.join(" AND ", conditions));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 정렬
        if (sortField != null && !sortField.isBlank()) {
            String direction = "DESC".equalsIgnoreCase(sortDirection) ? "DESC" : "ASC";
            sql.append(" ORDER BY \"" + sortField + "\" ").append(direction);
        }

        // 페이징
        sql.append(" OFFSET ? LIMIT ?");
        params.add(offset);
        params.add(limit);

        // 쿼리 실행
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params.toArray());

        // 타입 평탄화
        rows.forEach(row -> {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                Object v = entry.getValue();
                if (v instanceof PGobject pgObj) {
                    String val = pgObj.getValue();
                    if (val != null && val.startsWith("{") && val.contains("\"value\"")) {
                        val = val.replaceAll(".*\"value\"\\s*:\\s*\"([^\"]+)\".*", "$1");
                    }
                    entry.setValue(val);
                } else if (v instanceof String str && str.startsWith("{") && str.contains("\"value\"")) {
                    String extracted = str.replaceAll(".*\"value\"\\s*:\\s*\"([^\"]+)\".*", "$1");
                    entry.setValue(extracted);
                } else if (v instanceof Map<?, ?> map && map.containsKey("value")) {
                    entry.setValue(map.get("value"));
                }
            }
        });

        // 컬럼 목록 조회
        String colSql = """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_name = ?
                ORDER BY ordinal_position
                """;
        List<String> columns = jdbcTemplate.queryForList(colSql, String.class, tableName);

        return Map.of(
                "layer", layer,
                "columns", columns,
                "rows", rows
        );
    }

    /** 체크박스 필터용 distinct 값 조회 */
    @GetMapping("/filtering")
    public Map<String, Object> getDistinctValues(
            @RequestParam(defaultValue = "ethernet") String layer,
            @RequestParam String field
    ) {
        String tableName = switch (layer.toLowerCase()) {
            case "http_page" -> "page_sample";
            default -> "ethernet_sample";
        };

        // 안전한 컬럼 검증
        String colSql = """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_name = ?
            """;
        List<String> validColumns = jdbcTemplate.queryForList(colSql, String.class, tableName);

        if (!validColumns.contains(field)) {
            return Map.of(
                    "field", field,
                    "values", List.of(),
                    "error", "Invalid field name"
            );
        }

        // DISTINCT 조회
        String sql = String.format("SELECT DISTINCT \"%s\" FROM %s WHERE \"%s\" IS NOT NULL", field, tableName, field);
        List<Object> values = jdbcTemplate.queryForList(sql, Object.class);

        // PGobject → String 변환
        List<Object> processedValues = new ArrayList<>();
        for (Object v : values) {
            if (v instanceof PGobject pgObj) {
                processedValues.add(pgObj.getValue());
            } else {
                processedValues.add(v);
            }
        }

        return Map.of(
                "field", field,
                "values", processedValues
        );
    }
}
