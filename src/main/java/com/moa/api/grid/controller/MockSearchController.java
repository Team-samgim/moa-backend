package com.moa.api.grid.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import org.postgresql.util.PGobject;

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
            @RequestParam(required = false) String sortDirection
    ) {
        // layer별 테이블 이름 매핑
        String tableName = switch (layer.toLowerCase()) {
            case "http_page" -> "page_sample";
            default -> "ethernet_sample";
        };

        // 정렬 파라미터 처리
        String orderSql = "";
        if (sortField != null && !sortField.isBlank()) {
            String direction = "ASC".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";
            orderSql = " ORDER BY " + sortField + " " + direction;
        }

        // 실제 데이터 조회
        String sql = String.format(
                "SELECT * FROM %s %s OFFSET ? LIMIT ?",
                tableName,
                (sortField != null && !sortField.isEmpty()
                        ? "ORDER BY " + sortField + " " + (sortDirection != null ? sortDirection.toUpperCase() : "ASC")
                        : "")
        );
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, offset, limit);

        // 타입 평탄화 (object → value)
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

        // 최종 응답
        return Map.of(
                "layer", layer,
                "columns", columns,
                "rows", rows
        );
    }
}
