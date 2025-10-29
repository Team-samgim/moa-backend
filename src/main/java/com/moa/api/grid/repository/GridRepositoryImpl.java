package com.moa.api.grid.repository;

import com.moa.api.grid.dto.SearchResponseDTO;
import com.moa.api.grid.util.QueryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class GridRepositoryImpl implements GridRepositoryCustom {

    private final JdbcTemplate jdbcTemplate;
    private final QueryBuilder queryBuilder;

    /** 메인 그리드 데이터 조회 */
    @Override
    public List<Map<String, Object>> getGridData(String layer,
                                                 String sortField,
                                                 String sortDirection,
                                                 String filterModel,
                                                 int offset,
                                                 int limit) {
        String sql = queryBuilder.buildSelectSQL(layer, sortField, sortDirection, filterModel, offset, limit);
        log.info("[GridRepositoryImpl] Executing SQL: {}", sql);

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> row = new HashMap<>();
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = meta.getColumnLabel(i);
                Object value = rs.getObject(i);

                if (value instanceof org.postgresql.util.PGobject pgObj) {
                    row.put(columnName, pgObj.getValue());
                } else {
                    row.put(columnName, value);
                }
            }
            return row;
        });
    }


    /** DISTINCT 필터용 값 조회 */
    @Override
    public List<String> getDistinctValues(String layer, String column, String filterModel) {
        // ✅ QueryBuilder에서 필터 조건이 반영된 DISTINCT SQL 생성
        String sql = queryBuilder.buildDistinctSQL(layer, column, filterModel);
        log.info("[GridRepositoryImpl] DISTINCT SQL: {}", sql);

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Object value = rs.getObject(1);
            // PostgreSQL inet/macaddr 타입은 PGobject로 들어올 수 있으므로 문자열로 변환
            if (value instanceof PGobject pgObj) {
                return pgObj.getValue();
            }
            return value != null ? value.toString() : null;
        });
    }


    /** ✅ PostgreSQL용 컬럼명 + 타입 조회 (정규화 버전) */
    public List<SearchResponseDTO.ColumnDTO> getColumnsWithType(String layer) {
        String tableName = switch (layer.toLowerCase()) {
            case "http_page" -> "page_sample";
            default -> "ethernet_sample";
        };

        String colSql = """
        SELECT 
            column_name,
            data_type,
            udt_name
        FROM information_schema.columns
        WHERE table_name = ?
        ORDER BY ordinal_position
    """;

        List<Map<String, Object>> result = jdbcTemplate.queryForList(colSql, tableName);
        List<SearchResponseDTO.ColumnDTO> columns = new ArrayList<>();

        for (Map<String, Object> row : result) {
            String name = row.get("column_name").toString();
            String dataType = row.get("data_type").toString().toLowerCase();
            String udtType = row.get("udt_name").toString().toLowerCase();

            // ✅ DB 타입 → 표준화된 타입으로 매핑
            String mappedType = mapToFrontendType(udtType, dataType, name);
            columns.add(new SearchResponseDTO.ColumnDTO(name, mappedType));
        }

        return columns;
    }

    /** ✅ PostgreSQL 타입 → 프론트용 타입 매핑 */
    private String mapToFrontendType(String udtType, String dataType, String colName) {
        String t = udtType != null ? udtType : dataType;
        t = t.toLowerCase();
        colName = colName.toLowerCase();

        // 문자열형
        if (t.contains("char") || t.contains("text") || t.contains("varchar")) return "string";

        // 숫자형
        if (t.contains("int") || t.contains("numeric") || t.contains("decimal")
                || t.contains("float") || t.contains("double") || t.contains("real")) return "number";

        // 날짜/시간형
        if (t.contains("date") || t.contains("time") || t.contains("timestamp")) return "date";

        // 불리언형
        if (t.contains("bool")) return "boolean";

        // 네트워크형
        if (t.contains("inet") || colName.contains("ip")) return "ip";
        if (t.contains("mac") || colName.contains("mac")) return "mac";

        // JSON형
        if (t.contains("json")) return "json";

        // 기본값
        return "string";
    }


    /** ✅ PostgreSQL 타입 → 프론트 타입 매핑 */
    private String mapDbType(String dbType, String colName) {
        dbType = dbType.toLowerCase();
        colName = colName.toLowerCase();

        // 문자열
        if (dbType.equals("varchar") || dbType.equals("text") || dbType.equals("bpchar")) return "string";

        // 숫자
        if (dbType.equals("int4") || dbType.equals("int8") || dbType.equals("float4") ||
                dbType.equals("float8") || dbType.equals("numeric")) return "number";

        // 날짜/시간
        if (dbType.equals("date") || dbType.equals("timestamp") || dbType.equals("timestamptz") || dbType.equals("time")) return "date";

        // boolean
        if (dbType.equals("bool")) return "boolean";

        // IP / MAC (PostgreSQL 네이티브 타입)
        if (dbType.equals("inet") || colName.contains("ip")) return "ip";
        if (dbType.equals("macaddr") || colName.contains("mac")) return "mac";

        // JSON / JSONB
        if (dbType.equals("json") || dbType.equals("jsonb")) return "json";

        // 기본값
        return "string";
    }
}
