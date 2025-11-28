// 작성자: 최이서
package com.moa.api.pivot.repository;

import com.moa.api.pivot.dto.request.DistinctValuesRequestDTO;
import com.moa.api.pivot.dto.request.PivotQueryRequestDTO;
import com.moa.api.pivot.dto.response.DistinctValuesResponseDTO;
import com.moa.api.pivot.dto.response.PivotQueryResponseDTO;
import com.moa.api.pivot.exception.BadRequestException;
import com.moa.api.pivot.model.PivotFieldMeta;
import com.moa.api.pivot.model.PivotLayer;
import com.moa.api.pivot.model.PivotQueryContext;
import com.moa.api.pivot.model.TimeWindow;
import com.moa.api.pivot.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PivotRepositoryImpl implements PivotRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final SqlSupport sql;
    private final PivotQueryBuilder pivotQueryBuilder;
    private final PivotChartQueryBuilder chartQueryBuilder;
    private final PivotRowQueryBuilder rowQueryBuilder;

    // ===========================
    // 1) 필드 메타 조회
    // ===========================

    @Override
    public List<PivotFieldMeta> findFieldMetaForLayer(PivotLayer layer) {
        String tableName = layer.getFieldsTable();

        String text = """
            SELECT field_key, data_type, label_ko
            FROM %s
            ORDER BY field_key
            """.formatted(tableName);

        return jdbc.query(
                text,
                (rs, i) -> new PivotFieldMeta(
                        rs.getString("field_key"),
                        rs.getString("data_type"),
                        rs.getString("label_ko")
                )
        );
    }

    // ===========================
    // 2) Distinct Values 페이징
    // ===========================

    @Override
    public DistinctValuesResponseDTO pageDistinctValues(DistinctValuesRequestDTO req, TimeWindow tw) {
        int limit = Math.min(req.getLimit() != null ? req.getLimit() : 50, 200);

        // COUNT 쿼리
        var countQuery = pivotQueryBuilder.buildDistinctValuesCountSql(
                createContext(req.getLayer(), tw, req.getFilters(), req.getTime()),
                req
        );
        Integer totalCount = jdbc.queryForObject(countQuery.getSql(), countQuery.getParams(), Integer.class);

        // 페이지 쿼리
        var pageQuery = pivotQueryBuilder.buildDistinctValuesPageSql(
                createContext(req.getLayer(), tw, req.getFilters(), req.getTime()),
                req,
                limit
        );

        List<String> items = jdbc.query(pageQuery.getSql(), pageQuery.getParams(), (rs, i) -> {
            String v = rs.getString("val");
            return (v == null || v.isBlank()) ? "(empty)" : v;
        });

        boolean hasMore = items.size() == limit;
        String nextCursor = hasMore ? CursorCodec.encode(items.get(items.size() - 1)) : null;

        return new DistinctValuesResponseDTO(items, nextCursor, hasMore, totalCount);
    }

    // ===========================
    // 3) Column 축 상위 값
    // ===========================

    @Override
    public List<String> findTopColumnValues(PivotQueryContext ctx, String columnField) {
        var query = pivotQueryBuilder.buildTopColumnValuesSql(ctx, columnField);
        return jdbc.query(query.getSql(), query.getParams(), (rs, i) -> rs.getString("col_val"));
    }

    // ===========================
    // 4) RowGroup 빌드
    // ===========================

    @Override
    public List<PivotQueryResponseDTO.RowGroup> buildRowGroups(
            PivotQueryContext ctx,
            List<PivotQueryRequestDTO.RowDef> rows,
            List<PivotQueryRequestDTO.ValueDef> values,
            String columnField,
            List<String> columnValues
    ) {
        List<PivotQueryResponseDTO.RowGroup> groups = new ArrayList<>();

        boolean hasColumn = columnField != null && !columnField.isBlank()
                && columnValues != null && !columnValues.isEmpty();
        boolean hasMetrics = values != null && !values.isEmpty();

        Map<String, Map<String, Object>> summaryCells = new LinkedHashMap<>();
        if (hasColumn && hasMetrics) {
            summaryCells = fetchSummaryByColumn(ctx, columnField, columnValues, values);
        }

        for (PivotQueryRequestDTO.RowDef rowDef : rows) {
            String rowField = rowDef.getField();
            List<String> rowVals = fetchDistinctRowValues(ctx, rowField);
            int distinctCount = rowVals.size();

            groups.add(
                    PivotQueryResponseDTO.RowGroup.builder()
                            .rowLabel(rowField)
                            .displayLabel(rowField + " (" + distinctCount + ")")
                            .rowInfo(PivotQueryResponseDTO.RowInfo.builder()
                                    .count(distinctCount)
                                    .build())
                            .cells(summaryCells)
                            .items(List.of())
                            .build()
            );
        }

        return groups;
    }

    // ===========================
    // 5) RowGroupItem 빌드
    // ===========================

    @Override
    public List<PivotQueryResponseDTO.RowGroupItem> buildRowGroupItems(
            PivotQueryContext ctx,
            String rowField,
            List<PivotQueryRequestDTO.ValueDef> values,
            String columnField,
            List<String> columnValues,
            int offset,
            int limit,
            PivotQueryRequestDTO.SortDef sort
    ) {
        boolean hasColumn = columnField != null && !columnField.isBlank()
                && columnValues != null && !columnValues.isEmpty();
        boolean hasMetrics = values != null && !values.isEmpty();

        Map<String, Map<String, Map<String, Object>>> breakdown = new LinkedHashMap<>();
        if (hasColumn && hasMetrics) {
            breakdown = fetchBreakdownByRowAndColumn(ctx, rowField, columnField, values);
        }

        boolean hasSort = sort != null && sort.getDirection() != null
                && sort.getValueField() != null && sort.getAgg() != null
                && sort.getColumnValue() != null && hasColumn && hasMetrics;

        // 정렬 없음 → 단순 페이징
        if (!hasSort) {
            return buildSimplePagedItems(ctx, rowField, columnValues, offset, limit, breakdown, hasColumn, hasMetrics);
        }

        // 정렬 있음 → 전체 로드 후 정렬
        return buildSortedPagedItems(ctx, rowField, values, columnValues, offset, limit, sort, breakdown, hasColumn, hasMetrics);
    }

    // ===========================
    // 6) TopN dimension 값 조회
    // ===========================

    @Override
    public List<String> findTopNDimensionValues(
            PivotLayer layer,
            String field,
            PivotQueryRequestDTO.TopNDef topN,
            PivotQueryRequestDTO.ValueDef metric,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw
    ) {
        if (topN == null || metric == null) {
            return List.of();
        }

        String layerCode = layer.getCode();
        String table = sql.table(layerCode);
        String dimCol = sql.col(layerCode, field);
        String metricCol = sql.col(layerCode, metric.getField());

        String aggFunc = metric.getAgg() != null ? metric.getAgg().toUpperCase() : "SUM";
        String orderDir = "bottom".equalsIgnoreCase(topN.getMode()) ? "ASC" : "DESC";
        int limit = (topN.getN() != null && topN.getN() > 0) ? topN.getN() : 5;

        var ps = new org.springframework.jdbc.core.namedparam.MapSqlParameterSource();
        String where = sql.where(layerCode, layer.getDefaultTimeField(), tw, filters, ps);

        String text = """
            SELECT %s AS dim_val,
                   %s(%s) AS metric_val
            FROM %s
            %s
            GROUP BY %s
            ORDER BY metric_val %s
            LIMIT :lim
            """.formatted(dimCol, aggFunc, metricCol, table, where, dimCol, orderDir);

        ps.addValue("lim", limit);

        return jdbc.query(text, ps, (rs, i) -> rs.getString("dim_val"));
    }

    // ===========================
    // Private Helper Methods
    // ===========================

    private PivotQueryContext createContext(
            String layerCode,
            TimeWindow tw,
            List<PivotQueryRequestDTO.FilterDef> filters,
            PivotQueryRequestDTO.TimeDef timeDef
    ) {
        // 1) 레이어 매핑
        PivotLayer layer = PivotLayer.from(layerCode);

        // 2) 시간 컬럼 결정 (요청에 field 있으면 그거, 없으면 레이어 기본값)
        String timeField = null;
        if (timeDef != null && timeDef.getField() != null && !timeDef.getField().isBlank()) {
            timeField = timeDef.getField();
        } else {
            timeField = layer.getDefaultTimeField(); // 예: ts_server_nsec 같은 기본값
        }

        // 3) PivotQueryContext 생성
        return new PivotQueryContext(
                layer,
                timeField,
                tw,
                filters,
                sql       // 기존 SqlSupport
        );
    }

    private List<String> fetchDistinctRowValues(PivotQueryContext ctx, String rowField) {
        var query = rowQueryBuilder.buildDistinctRowValuesQuery(ctx, rowField);
        return jdbc.query(query.sql(), query.params(), (rs, i) -> rs.getString("val"));
    }

    private List<String> fetchDistinctRowValues(PivotQueryContext ctx, String rowField, int offset, int limit) {
        var query = rowQueryBuilder.buildDistinctRowValuesPageQuery(ctx, rowField, offset, limit);
        return jdbc.query(query.sql(), query.params(), (rs, i) -> rs.getString("val"));
    }

    private Map<String, Map<String, Object>> fetchSummaryByColumn(
            PivotQueryContext ctx,
            String columnField,
            List<String> columnValues,
            List<PivotQueryRequestDTO.ValueDef> metrics
    ) {
        var query = rowQueryBuilder.buildSummaryByColumnQuery(ctx, columnField, metrics);
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();

        jdbc.query(query.sql(), query.params(), rs -> {
            String colVal = rs.getString("col_val");
            if (colVal == null || !columnValues.contains(colVal)) return;

            Map<String, Object> metricMap = new LinkedHashMap<>();
            for (PivotQueryRequestDTO.ValueDef m : metrics) {
                metricMap.put(m.getAlias(), rs.getObject(m.getAlias()));
            }
            result.put(colVal, metricMap);
        });

        for (String v : columnValues) {
            result.putIfAbsent(v, new LinkedHashMap<>());
        }

        return result;
    }

    private Map<String, Map<String, Map<String, Object>>> fetchBreakdownByRowAndColumn(
            PivotQueryContext ctx,
            String rowField,
            String columnField,
            List<PivotQueryRequestDTO.ValueDef> metrics
    ) {
        var query = rowQueryBuilder.buildBreakdownQuery(ctx, rowField, columnField, metrics);
        Map<String, Map<String, Map<String, Object>>> tmp = new LinkedHashMap<>();

        jdbc.query(query.sql(), query.params(), rs -> {
            String rv = rs.getString("row_val");
            String cv = rs.getString("col_val");

            Map<String, Object> metricMap = new LinkedHashMap<>();
            for (PivotQueryRequestDTO.ValueDef m : metrics) {
                metricMap.put(m.getAlias(), rs.getObject(m.getAlias()));
            }

            tmp.computeIfAbsent(rv, k -> new LinkedHashMap<>()).put(cv, metricMap);
        });

        return tmp;
    }

    private List<PivotQueryResponseDTO.RowGroupItem> buildSimplePagedItems(
            PivotQueryContext ctx,
            String rowField,
            List<String> columnValues,
            int offset,
            int limit,
            Map<String, Map<String, Map<String, Object>>> breakdown,
            boolean hasColumn,
            boolean hasMetrics
    ) {
        List<String> rowVals = fetchDistinctRowValues(ctx, rowField, offset, limit);
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

    private List<PivotQueryResponseDTO.RowGroupItem> buildSortedPagedItems(
            PivotQueryContext ctx,
            String rowField,
            List<PivotQueryRequestDTO.ValueDef> values,
            List<String> columnValues,
            int offset,
            int limit,
            PivotQueryRequestDTO.SortDef sort,
            Map<String, Map<String, Map<String, Object>>> breakdown,
            boolean hasColumn,
            boolean hasMetrics
    ) {
        List<String> allRowVals = fetchDistinctRowValues(ctx, rowField);

        String sortAlias = findSortAlias(values, sort);
        if (sortAlias == null) {
            return buildSimplePagedItems(ctx, rowField, columnValues, offset, limit, breakdown, hasColumn, hasMetrics);
        }

        allRowVals.sort(new RowValueComparator(breakdown, sort.getColumnValue(), sortAlias, sort.getDirection()));

        int fromIdx = Math.max(0, offset);
        int toIdx = Math.min(allRowVals.size(), offset + limit);
        if (fromIdx >= toIdx) {
            return List.of();
        }

        List<String> pageRowVals = allRowVals.subList(fromIdx, toIdx);
        List<PivotQueryResponseDTO.RowGroupItem> items = new ArrayList<>();

        for (String rv : pageRowVals) {
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

    private String findSortAlias(List<PivotQueryRequestDTO.ValueDef> values, PivotQueryRequestDTO.SortDef sort) {
        if (values == null) return null;
        for (PivotQueryRequestDTO.ValueDef v : values) {
            if (sort.getValueField().equals(v.getField())
                    && sort.getAgg().equalsIgnoreCase(v.getAgg())) {
                return v.getAlias();
            }
        }
        return null;
    }

    // Inner classes
    private static class RowValueComparator implements Comparator<String> {
        private final Map<String, Map<String, Map<String, Object>>> breakdown;
        private final String sortColumnValue;
        private final String sortAlias;
        private final String direction;

        public RowValueComparator(
                Map<String, Map<String, Map<String, Object>>> breakdown,
                String sortColumnValue,
                String sortAlias,
                String direction
        ) {
            this.breakdown = breakdown;
            this.sortColumnValue = sortColumnValue;
            this.sortAlias = sortAlias;
            this.direction = direction;
        }

        @Override
        public int compare(String rv1, String rv2) {
            Map<String, Map<String, Object>> byCol1 =
                    breakdown.getOrDefault(rv1, Collections.emptyMap());
            Map<String, Map<String, Object>> byCol2 =
                    breakdown.getOrDefault(rv2, Collections.emptyMap());

            Map<String, Object> metrics1 =
                    byCol1.getOrDefault(sortColumnValue, Collections.emptyMap());
            Map<String, Object> metrics2 =
                    byCol2.getOrDefault(sortColumnValue, Collections.emptyMap());

            Object o1 = metrics1.get(sortAlias);
            Object o2 = metrics2.get(sortAlias);

            double d1 = (o1 instanceof Number) ? ((Number) o1).doubleValue() : Double.NaN;
            double d2 = (o2 instanceof Number) ? ((Number) o2).doubleValue() : Double.NaN;

            if (Double.isNaN(d1) && Double.isNaN(d2)) return 0;
            if (Double.isNaN(d1)) return 1;
            if (Double.isNaN(d2)) return -1;

            int cmp = Double.compare(d1, d2);
            if ("desc".equalsIgnoreCase(direction)) {
                cmp = -cmp;
            }

            if (cmp != 0) return cmp;

            if (rv1 == null && rv2 == null) return 0;
            if (rv1 == null) return 1;
            if (rv2 == null) return -1;
            return rv1.compareTo(rv2);
        }
    }
}