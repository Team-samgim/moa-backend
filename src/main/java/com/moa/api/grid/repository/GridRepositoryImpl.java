package com.moa.api.grid.repository;

import com.moa.api.grid.util.QueryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class GridRepositoryImpl implements GridRepositoryCustom {

    private final JdbcTemplate jdbcTemplate;
    private final QueryBuilder queryBuilder;

    /** ✅ 메인 그리드 데이터 조회 */
    @Override
    public List<Map<String, Object>> getGridData(String layer, String sortField, String sortDirection,
                                                 String filterModel, int offset, int limit) {
        String sql = queryBuilder.buildSelectSQL(layer, sortField, sortDirection, filterModel, offset, limit);
        log.info("[GridRepositoryImpl] Executing SQL: {}", sql);
        return jdbcTemplate.queryForList(sql);
    }

    /** ✅ DISTINCT 필터용 값 조회 (다른 필터 반영) */
    @Override
    public List<String> getDistinctValues(String layer, String column, String filterModel) {
        String innerQuery = queryBuilder.buildSelectSQL(layer, null, null, filterModel, 0, 0)
                .replaceFirst("SELECT \\*", "SELECT *");

        String sql = String.format("""
            SELECT DISTINCT "%s"
            FROM (%s) AS filtered
            WHERE "%s" IS NOT NULL
        """, column, innerQuery, column);

        log.info("[GridRepositoryImpl] Distinct SQL with filter: {}", sql);
        return jdbcTemplate.queryForList(sql, String.class);
    }

    /** ✅ 컬럼 목록 조회 */
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
}
