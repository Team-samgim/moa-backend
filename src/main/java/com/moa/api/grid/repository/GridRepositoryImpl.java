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
    public List<Map<String, Object>> getGridData(String layer, String sortField, String sortDirection,
                                                 String filterModel, int offset, int limit) {

        Map<String,String> typeMap = buildFrontendTypeMap(layer);
        Map<String,String> temporalMap = buildTemporalKindMap(layer);

        String sql = queryBuilder.buildSelectSQL(
                layer, sortField, sortDirection, filterModel, offset, limit,
                typeMap, temporalMap
        );
        log.info("[GridRepositoryImpl] Executing SQL: {}", sql);

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> row = new HashMap<>();
            var meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                String columnName = meta.getColumnLabel(i);
                Object value = rs.getObject(i);
                if (value instanceof PGobject pgObj) row.put(columnName, pgObj.getValue());
                else row.put(columnName, value);
            }
            return row;
        });
    }

    @Override
    public List<String> getDistinctValues(String layer, String column, String filterModel) {
        Map<String,String> typeMap = buildFrontendTypeMap(layer);
        Map<String,String> temporalMap = buildTemporalKindMap(layer);

        String sql = queryBuilder.buildDistinctSQL(
                layer, column, filterModel, true, typeMap, temporalMap
        );
        log.info("[GridRepositoryImpl] DISTINCT SQL: {}", sql);

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Object value = rs.getObject(1);
            if (value instanceof PGobject pgObj) return pgObj.getValue();
            return value != null ? value.toString() : null;
        });
    }

    /** ✅ PostgreSQL용 컬럼명 + 타입 조회 (정규화 버전) */
    public List<SearchResponseDTO.ColumnDTO> getColumnsWithType(String layer) {
        String tableName = switch (layer.toLowerCase()) {
            case "http_page" -> "http_page_sample";
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

    // 컬럼 -> 프론트타입(string/number/date/ip/mac...)
    private Map<String,String> buildFrontendTypeMap(String layer) {
        List<SearchResponseDTO.ColumnDTO> cols = getColumnsWithType(layer);
        Map<String,String> m = new HashMap<>();
        for (var c : cols) m.put(c.getName(), c.getType());
        return m;
    }

    // 컬럼 -> 원시 시간 UDT(kind): timestamptz / timestamp / date
    private Map<String,String> buildTemporalKindMap(String layer) {
        String tableName = switch (layer.toLowerCase()) {
            case "http_page" -> "http_page_sample";
            case "http_uri"  -> "uri_sample";
            case "l4_tcp"    -> "tcp_sample";
            default -> "ethernet_sample";
        };
        String sql = """
        SELECT column_name, udt_name
        FROM information_schema.columns
        WHERE table_name = ?
    """;
        Map<String,String> m = new HashMap<>();
        for (var row : jdbcTemplate.queryForList(sql, tableName)) {
            String col  = row.get("column_name").toString();
            String udt  = row.get("udt_name").toString().toLowerCase(); // e.g. timestamptz, timestamp, date ...
            // 우리가 쓰는 키만 표준화
            if (udt.contains("timestamptz")) m.put(col, "timestamptz");
            else if (udt.equals("timestamp")) m.put(col, "timestamp");
            else if (udt.contains("date"))   m.put(col, "date");
            else if (udt.contains("time"))   m.put(col, "timestamp"); // time만 있는 경우 비교시 ::date 변환용으로 timestamp 취급
        }
        return m;
    }
}
