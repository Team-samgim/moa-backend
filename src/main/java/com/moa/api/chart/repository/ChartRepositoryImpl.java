package com.moa.api.chart.repository;

import com.moa.api.chart.util.ChartAxisResolver;
import com.moa.api.chart.util.ChartSeriesFactory;
import com.moa.api.chart.util.HeatmapTableLoader;
import com.moa.api.pivot.dto.request.PivotChartRequestDTO;
import com.moa.api.pivot.dto.request.PivotHeatmapTableRequestDTO;
import com.moa.api.pivot.dto.request.PivotQueryRequestDTO;
import com.moa.api.pivot.dto.response.PivotChartResponseDTO;
import com.moa.api.pivot.dto.response.PivotHeatmapTableResponseDTO;
import com.moa.api.pivot.exception.BadRequestException;
import com.moa.api.pivot.model.PivotLayer;
import com.moa.api.pivot.model.PivotQueryContext;
import com.moa.api.pivot.util.PivotChartQueryBuilder;
import com.moa.api.pivot.util.PivotHeatmapQueryBuilder;
import com.moa.api.pivot.util.ValueUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ChartRepositoryImpl implements ChartRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final PivotChartQueryBuilder chartQueryBuilder;
    private final PivotHeatmapQueryBuilder heatmapQueryBuilder;

    private final ChartAxisResolver chartAxisResolver;
    private final HeatmapTableLoader heatmapTableLoader;

    @Override
    public PivotChartResponseDTO getChart(PivotQueryContext ctx, PivotChartRequestDTO req) {
        PivotLayer layer = ctx.getLayer();
        if (layer == null) {
            throw new BadRequestException("layer is required");
        }

        PivotChartRequestDTO.AxisDef colDef = req.getCol();
        PivotChartRequestDTO.AxisDef rowDef = req.getRow();
        PivotQueryRequestDTO.ValueDef metricDef = req.getMetric();

        validateChartRequest(colDef, rowDef, metricDef);

        List<PivotQueryRequestDTO.FilterDef> baseFilters =
                req.getFilters() != null ? new ArrayList<>(req.getFilters()) : new ArrayList<>();

        boolean isMultiplePie = "multiplePie".equalsIgnoreCase(req.getChartType());
        int maxColCount = isMultiplePie ? 6 : 5;
        int maxRowCount = 5;

        // X, Y 축 카테고리 결정
        List<String> xCategories = chartAxisResolver.resolveAxisKeys(ctx, colDef, metricDef, baseFilters, maxColCount);
        List<String> yCategories = chartAxisResolver.resolveAxisKeys(ctx, rowDef, metricDef, baseFilters, maxRowCount);

        if (xCategories.isEmpty() || yCategories.isEmpty()) {
            return PivotChartResponseDTO.empty(colDef.getField(), rowDef.getField());
        }

        // 데이터 조회
        var query = chartQueryBuilder.buildChartDataQuery(
                ctx, colDef.getField(), rowDef.getField(), metricDef,
                xCategories, yCategories, baseFilters
        );

        Map<String, Map<String, Double>> valueMap = new HashMap<>();
        jdbc.query(query.sql(), query.params(), rs -> {
            String xKey = ValueUtils.normalizeKey(rs.getString("c"));
            String yKey = ValueUtils.normalizeKey(rs.getString("r"));
            double v = rs.getDouble("m");
            if (rs.wasNull()) v = 0.0;

            valueMap.computeIfAbsent(yKey, k -> new HashMap<>()).put(xKey, v);
        });

        // 매트릭스 & SeriesDef 생성
        List<List<Double>> valuesMatrix = ChartSeriesFactory.buildValuesMatrix(xCategories, yCategories, valueMap);
        PivotChartResponseDTO.SeriesDef seriesDef =
                ChartSeriesFactory.buildSingleSeries(metricDef, valuesMatrix);

        return PivotChartResponseDTO.builder()
                .xField(colDef.getField())
                .yField(rowDef.getField())
                .xCategories(xCategories)
                .yCategories(yCategories)
                .series(Collections.singletonList(seriesDef))
                .build();
    }

    @Override
    public PivotHeatmapTableResponseDTO getHeatmapTable(PivotQueryContext ctx, PivotHeatmapTableRequestDTO req) {
        PivotLayer layer = ctx.getLayer();
        String colField = req.getColField();
        String rowField = req.getRowField();
        PivotQueryRequestDTO.ValueDef metric = req.getMetric();

        validateHeatmapRequest(colField, rowField, metric);

        int offset = heatmapTableLoader.resolveOffset(req);
        int limit = heatmapTableLoader.resolveLimit(req);

        // 1) X축 카테고리
        List<String> xCategories = heatmapTableLoader.fetchXCategories(ctx, colField, metric);
        if (xCategories.isEmpty()) {
            return PivotHeatmapTableResponseDTO.empty(colField, rowField);
        }

        // 2) Y축 전체 수
        long totalRowCount = heatmapTableLoader.fetchYTotalCount(ctx, rowField);
        if (totalRowCount == 0L) {
            return PivotHeatmapTableResponseDTO.empty(colField, rowField);
        }

        // 3) Y축 페이지
        List<String> yCategories = heatmapTableLoader.fetchYCategoriesPage(ctx, rowField, metric, offset, limit);
        if (yCategories.isEmpty()) {
            return PivotHeatmapTableResponseDTO.empty(colField, rowField);
        }

        // 4) 셀 값 조회
        HeatmapTableLoader.HeatmapCellResult cellResult =
                heatmapTableLoader.fetchCellValues(ctx, colField, rowField, metric, xCategories, yCategories);

        List<PivotHeatmapTableResponseDTO.RowDef> rows = new ArrayList<>();
        for (int yi = 0; yi < yCategories.size(); yi++) {
            rows.add(
                    PivotHeatmapTableResponseDTO.RowDef.builder()
                            .yCategory(yCategories.get(yi))
                            .cells(cellResult.values().get(yi))
                            .rowTotal(cellResult.rowTotals().get(yi))
                            .build()
            );
        }

        return PivotHeatmapTableResponseDTO.builder()
                .xField(colField)
                .yField(rowField)
                .xCategories(xCategories)
                .rows(rows)
                .totalRowCount(totalRowCount)
                .offset(offset)
                .limit(limit)
                .pageMin(cellResult.pageMin())
                .pageMax(cellResult.pageMax())
                .build();
    }

    private void validateChartRequest(
            PivotChartRequestDTO.AxisDef colDef,
            PivotChartRequestDTO.AxisDef rowDef,
            PivotQueryRequestDTO.ValueDef metricDef
    ) {
        if (colDef == null || colDef.getField() == null || colDef.getField().isBlank()) {
            throw new BadRequestException("col.field is required");
        }
        if (rowDef == null || rowDef.getField() == null || rowDef.getField().isBlank()) {
            throw new BadRequestException("row.field is required");
        }
        if (metricDef == null || metricDef.getField() == null || metricDef.getField().isBlank()) {
            throw new BadRequestException("metric.field is required");
        }
    }

    private void validateHeatmapRequest(String colField, String rowField, PivotQueryRequestDTO.ValueDef metric) {
        if (colField == null || colField.isBlank()) {
            throw new BadRequestException("colField is required");
        }
        if (rowField == null || rowField.isBlank()) {
            throw new BadRequestException("rowField is required");
        }
        if (metric == null || metric.getField() == null || metric.getField().isBlank()) {
            throw new BadRequestException("metric is required");
        }
    }
}
