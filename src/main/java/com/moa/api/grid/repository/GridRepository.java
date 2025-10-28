package com.moa.api.grid.repository;

import com.moa.api.grid.dto.FilterRequestDTO;
import com.moa.api.grid.util.QueryBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class GridRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> findRows(FilterRequestDTO request) {
        // QueryBuilder를 이용해 SQL 생성
        String sql = QueryBuilder.buildSelectSQL(
                request.getLayer(),
                request.getSortField(),
                request.getSortDirection(),
                request.getFilterModel()
        );

        // 페이징
        sql += " OFFSET ? LIMIT ?";

        // 실제 쿼리 실행
        return jdbcTemplate.queryForList(sql, request.getOffset(), request.getLimit());
    }

    public List<String> getColumns(String layer) {
        String tableName = switch (layer.toLowerCase()) {
            case "http_page" -> "page_sample";
            default -> "ethernet_sample";
        };

        String colSql = """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_name = ?
                ORDER BY ordinal_position
                """;
        return jdbcTemplate.queryForList(colSql, String.class, tableName);
    }

    public Map<String, Object> getDistinctValues(String layer, String field) {
        String tableName = switch (layer.toLowerCase()) {
            case "http_page" -> "page_sample";
            default -> "ethernet_sample";
        };
        String sql = String.format("SELECT DISTINCT \"%s\" FROM %s WHERE \"%s\" IS NOT NULL", field, tableName, field);
        List<Object> values = jdbcTemplate.queryForList(sql, Object.class);
        return Map.of("field", field, "values", values);
    }
}

