package com.moa.api.pivot.repository;

import com.moa.api.pivot.dto.*;
import com.moa.api.pivot.model.TimeWindow;
import com.moa.api.pivot.util.CursorCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PivotRepositoryImpl implements PivotRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final SqlSupport sql;

    private String resolveTable(String layer) {
        return switch (layer) {
            case "HTTP_PAGE" -> "http_page_sample";
            case "Ethernet"  -> "ethernet_sample";
            case "TCP"       -> "tcp_sample";
            case "HTTP_URI"  -> "http_uri_sample";
            default -> throw new IllegalArgumentException("Unsupported layer: " + layer);
        };
    }

    @Override
    public List<String> findColumnNamesForLayer(String layer) {
        String tableName = resolveTable(layer);

        String sql = """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = :tableName
            ORDER BY ordinal_position
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("tableName", tableName);

        return jdbc.query(
                sql,
                params,
                (rs, i) -> rs.getString("column_name")
        );
    }

    /* ========= 1) 필드 값 페이지네이션 ========= */

    @Override
    public DistinctValuesPageDTO pageDistinctValues(DistinctValuesRequestDTO req, TimeWindow tw) {
        String table = sql.table(req.getLayer());
        String col   = sql.col(req.getLayer(), req.getField());

        MapSqlParameterSource ps = new MapSqlParameterSource();

        // 기본 시간 컬럼: ts_server_nsec
        String timeField = "ts_server_nsec";
        if (req.getTime() != null && req.getTime().getField() != null &&
                !req.getTime().getField().isBlank()) {
            timeField = req.getTime().getField();
        }

        String where = sql.where(req.getLayer(), timeField, tw, req.getFilters(), ps);

        // 검색어
        if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
            where += " AND " + col + " ILIKE :kw ";
            ps.addValue("kw", "%" + req.getKeyword() + "%");
        }

        // 커서 조건
        if (req.getCursor() != null && !req.getCursor().isBlank()) {
            where = sql.appendCursorCondition(
                    req.getLayer(),
                    req.getField(),
                    req.getOrder(),
                    where,
                    req.getCursor(),
                    ps
            );
        }

        String ord = "DESC".equalsIgnoreCase(req.getOrder()) ? "DESC" : "ASC";
        int limit  = Math.min(req.getLimit() != null ? req.getLimit() : 50, 200);

        String text = """
            SELECT DISTINCT %s AS val
            FROM %s
            %s
            ORDER BY %s %s NULLS LAST
            LIMIT :lim
        """.formatted(col, table, where, col, ord);
        ps.addValue("lim", limit);

        List<String> items = jdbc.query(text, ps, (rs, i) -> {
            String v = rs.getString("val");
            return (v == null || v.isBlank()) ? "(empty)" : v;
        });

        boolean hasMore   = items.size() == limit;
        String nextCursor = hasMore ? CursorCodec.encode(items.get(items.size() - 1)) : null;

        return new DistinctValuesPageDTO(items, nextCursor, hasMore);
    }

    /* ========= 2) Column 축 상위 값 ========= */

    @Override
    public List<String> findTopColumnValues(
            String layer,
            String columnField,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw
    ) {
        String table = sql.table(layer);
        String col   = sql.col(layer, columnField);

        MapSqlParameterSource ps = new MapSqlParameterSource();
        String where = sql.where(layer, "ts_server_nsec", tw, filters, ps);

        String text = """
            SELECT %s AS col_val, COUNT(*) AS cnt
            FROM %s
            %s
            GROUP BY %s
            ORDER BY cnt DESC, %s ASC
            LIMIT 10
        """.formatted(col, table, where, col, col);

        return jdbc.query(text, ps, (rs, i) -> rs.getString("col_val"));
    }

    /* ========= 3) Row distinct 값 ========= */

    public List<String> fetchDistinctRowValues(
            String layer,
            String rowField,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw
    ) {
        String table = sql.table(layer);
        String col = sql.col(layer, rowField);

        MapSqlParameterSource ps = new MapSqlParameterSource();
        String where = sql.where(layer, "ts_server_nsec", tw, filters, ps);

        String text = """
        SELECT DISTINCT %s AS val
        FROM %s
        %s
        ORDER BY %s ASC
    """.formatted(col, table, where, col);

        return jdbc.query(text, ps, (rs, i) -> rs.getString("val"));
    }

    public List<String> fetchDistinctRowValues(
            String layer,
            String rowField,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw,
            int offset,
            int limit
    ) {
        String table = sql.table(layer);
        String col = sql.col(layer, rowField);

        MapSqlParameterSource ps = new MapSqlParameterSource();
        String where = sql.where(layer, "ts_server_nsec", tw, filters, ps);

        String text = """
        SELECT DISTINCT %s AS val
        FROM %s
        %s
        ORDER BY %s ASC
        LIMIT :limit OFFSET :offset
    """.formatted(col, table, where, col);

        ps.addValue("limit", limit);
        ps.addValue("offset", offset);

//        return jdbc.query(text, ps, (rs, i) -> rs.getString("val"));
        log.info("=== fetchDistinctRowValues ===");
        log.info("SQL: {}", text);
        log.info("offset: {}, limit: {}", offset, limit);

        List<String> result = jdbc.query(text, ps, (rs, i) -> rs.getString("val"));

        log.info("Returned {} rows", result.size());

        return result;
    }

    /* ========= 4) Column별 요약 ========= */

    private Map<String, Map<String, Object>> fetchSummaryByColumn(
            String layer,
            String columnField,
            List<String> columnValues,
            List<PivotQueryRequestDTO.ValueDef> metrics,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw
    ) {
        String table = sql.table(layer);
        String col   = sql.col(layer, columnField);

        String aggSelect = metrics.stream()
                .map(m -> m.getAgg().toUpperCase() + "(" + sql.col(layer, m.getField()) + ") AS \"" + m.getAlias() + "\"")
                .collect(Collectors.joining(", "));

        MapSqlParameterSource ps = new MapSqlParameterSource();
        String where = sql.where(layer, "ts_server_nsec", tw, filters, ps);

        String text = """
            SELECT %s AS col_val,
                   %s
            FROM %s
            %s
            GROUP BY %s
        """.formatted(col, aggSelect, table, where, col);

        Map<String, Map<String, Object>> result = new LinkedHashMap<>();

        jdbc.query(text, ps, rs -> {
            String colVal = rs.getString("col_val");
            if (colVal == null || !columnValues.contains(colVal)) return;

            Map<String, Object> metricMap = new LinkedHashMap<>();
            for (PivotQueryRequestDTO.ValueDef m : metrics) {
                metricMap.put(m.getAlias(), rs.getObject(m.getAlias()));
            }
            result.put(colVal, metricMap);
        });

        // 빠진 컬럼 보강
        for (String v : columnValues) {
            result.putIfAbsent(v, new LinkedHashMap<>());
        }

        return result;
    }

    /* ========= 5) Row x Column breakdown ========= */

    private Map<String, Map<String, Map<String, Object>>> fetchBreakdownByRowAndColumn(
            String layer,
            String rowField,
            String columnField,
            List<PivotQueryRequestDTO.ValueDef> metrics,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw
    ) {
        String table = sql.table(layer);
        String row   = sql.col(layer, rowField);
        String col   = sql.col(layer, columnField);

        String aggSelect = metrics.stream()
                .map(m -> m.getAgg().toUpperCase() + "(" + sql.col(layer, m.getField()) + ") AS \"" + m.getAlias() + "\"")
                .collect(Collectors.joining(", "));

        MapSqlParameterSource ps = new MapSqlParameterSource();
        String where = sql.where(layer, "ts_server_nsec", tw, filters, ps);

        String text = """
            SELECT %s AS row_val,
                   %s AS col_val,
                   %s
            FROM %s
            %s
            GROUP BY %s, %s
        """.formatted(row, col, aggSelect, table, where, row, col);

        Map<String, Map<String, Map<String, Object>>> tmp = new LinkedHashMap<>();

        jdbc.query(text, ps, rs -> {
            String rv = rs.getString("row_val");
            String cv = rs.getString("col_val");

            Map<String, Object> metricMap = new LinkedHashMap<>();
            for (PivotQueryRequestDTO.ValueDef m : metrics) {
                metricMap.put(m.getAlias(), rs.getObject(m.getAlias()));
            }

            tmp.computeIfAbsent(rv, k -> new LinkedHashMap<>())
                    .put(cv, metricMap);
        });

        return tmp;
    }

    /* ========= 6) RowGroup 빌드 ========= */

    @Override
    public List<PivotQueryResponseDTO.RowGroup> buildRowGroups(
            String layer,
            List<PivotQueryRequestDTO.RowDef> rows,
            List<PivotQueryRequestDTO.ValueDef> values,
            String columnField,
            List<String> columnValues,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw
    ) {
        List<PivotQueryResponseDTO.RowGroup> groups = new ArrayList<>();

        boolean hasColumn = columnField != null && !columnField.isBlank()
                && columnValues != null && !columnValues.isEmpty();
        boolean hasMetrics = values != null && !values.isEmpty();

        // 1) 컬럼 요약 == 공통: 1번 계산
        Map<String, Map<String, Object>> summaryCells = new LinkedHashMap<>();
        if (hasColumn && hasMetrics) {
            summaryCells =
                    fetchSummaryByColumn(layer, columnField, columnValues, values, filters, tw);
        }

        // 2) 각 row group: distinct 값 + 개수 계산
        for (PivotQueryRequestDTO.RowDef rowDef : rows) {
            String rowField = rowDef.getField();

            // 해당 필드의 distinct 값들
            List<String> rowVals = fetchDistinctRowValues(layer, rowField, filters, tw);
            int distinctCount = rowVals.size();

            groups.add(
                    PivotQueryResponseDTO.RowGroup.builder()
                            .rowLabel(rowField)
                            .displayLabel(rowField + " (" + distinctCount + ")")
                            .rowInfo(PivotQueryResponseDTO.RowInfo.builder()
                                    .count(distinctCount)
                                    .build())
                            .cells(summaryCells)    // 모든 row group이 공유
                            .items(List.of())       // subRows 계산 X
                            .build()
            );
        }

        return groups;
    }

    @Override
    public List<PivotQueryResponseDTO.RowGroupItem> buildRowGroupItems(
            String layer,
            String rowField,
            List<PivotQueryRequestDTO.ValueDef> values,
            String columnField,
            List<String> columnValues,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw,
            int offset,
            int limit
    ) {
        boolean hasColumn = columnField != null && !columnField.isBlank()
                && columnValues != null && !columnValues.isEmpty();
        boolean hasMetrics = values != null && !values.isEmpty();

        List<String> rowVals = fetchDistinctRowValues(layer, rowField, filters, tw, offset, limit);

        Map<String, Map<String, Map<String, Object>>> breakdown = new LinkedHashMap<>();

        if (hasColumn && hasMetrics) {
            breakdown =
                    fetchBreakdownByRowAndColumn(layer, rowField, columnField, values, filters, tw);
        }

        List<PivotQueryResponseDTO.RowGroupItem> items = new ArrayList<>();
        for (String rv : rowVals) {
            Map<String, Map<String, Object>> childCells = new LinkedHashMap<>();

            if (hasColumn && hasMetrics) {
                Map<String, Map<String, Object>> byCol = breakdown.getOrDefault(rv, Map.of());
                for (String cv : columnValues) {
                    childCells.put(cv, byCol.getOrDefault(cv, Map.of()));
                }
            }

            items.add(
                    PivotQueryResponseDTO.RowGroupItem.builder()
                            .valueLabel(rv)
                            .displayLabel(rv)
                            .cells(childCells)
                            .build()
            );
        }

        return items;
    }
}
