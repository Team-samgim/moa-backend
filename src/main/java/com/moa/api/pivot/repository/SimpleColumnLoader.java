// 작성자: 최이서
package com.moa.api.pivot.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SimpleColumnLoader {

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * information_schema.columns에서 컬럼 이름 리스트 조회
     */
    public List<String> columns(String tableName) {
        String sql = """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name   = :table
            ORDER BY ordinal_position
        """;

        MapSqlParameterSource ps = new MapSqlParameterSource()
                .addValue("table", tableName);

        return jdbc.query(sql, ps, (rs, i) -> rs.getString("column_name"));
    }
}
