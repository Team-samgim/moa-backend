// 작성자: 최이서
package com.moa.api.chart.repository;

import com.moa.api.chart.dto.request.DrilldownTimeSeriesRequestDTO;
import com.moa.api.chart.dto.response.DrilldownTimeSeriesResponseDTO;
import com.moa.api.chart.util.ChartAxisResolver;
import com.moa.api.chart.util.ChartSeriesFactory;
import com.moa.api.chart.util.HeatmapTableLoader;
import com.moa.api.chart.dto.request.PivotChartRequestDTO;
import com.moa.api.chart.dto.request.PivotHeatmapTableRequestDTO;
import com.moa.api.pivot.dto.request.PivotQueryRequestDTO;
import com.moa.api.chart.dto.response.PivotChartResponseDTO;
import com.moa.api.chart.dto.response.PivotHeatmapTableResponseDTO;
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

        // 레이아웃 설정 확인
        Integer chartsPerRow = req.getLayout() != null ? req.getLayout().getChartsPerRow() : null;

        // 다중 차트 모드
        if (chartsPerRow != null && chartsPerRow > 0) {
            return getMultipleCharts(ctx, colDef, rowDef, metricDef, baseFilters, chartsPerRow);
        }

        // ===== 기존 단일 차트 로직 =====
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

    /**
     * 다중 차트 생성 로직
     * Column 축의 각 값에 대해 별도의 차트를 생성
     */
    private PivotChartResponseDTO getMultipleCharts(
            PivotQueryContext ctx,
            PivotChartRequestDTO.AxisDef colDef,
            PivotChartRequestDTO.AxisDef rowDef,
            PivotQueryRequestDTO.ValueDef metricDef,
            List<PivotQueryRequestDTO.FilterDef> baseFilters,
            int chartsPerRow
    ) {
        // Column 축 카테고리 결정 (최대 개수 = chartsPerRow * 2)
        int maxColCount = chartsPerRow * 2;
        int maxRowCount = 5;

        List<String> columnCategories = chartAxisResolver.resolveAxisKeys(
                ctx, colDef, metricDef, baseFilters, maxColCount);

        if (columnCategories.isEmpty()) {
            return PivotChartResponseDTO.builder()
                    .xField(colDef.getField())
                    .yField(rowDef.getField())
                    .charts(Collections.emptyList())
                    .build();
        }

        List<PivotChartResponseDTO.ChartData> chartDataList = new ArrayList<>();

        // 각 Column 값에 대해 별도의 차트 생성
        for (String columnKey : columnCategories) {
            // 이 Column 값에 대한 필터 추가
            List<PivotQueryRequestDTO.FilterDef> columnFilters = new ArrayList<>(baseFilters);
            PivotQueryRequestDTO.FilterDef colFilter = new PivotQueryRequestDTO.FilterDef();
            colFilter.setField(colDef.getField());
            colFilter.setOp("=");
            colFilter.setValue(columnKey);
            columnFilters.add(colFilter);

            // 이 Column에 대한 Row 축 TOP-N 결정
            List<String> yCategories = chartAxisResolver.resolveAxisKeys(
                    ctx, rowDef, metricDef, columnFilters, maxRowCount);

            if (yCategories.isEmpty()) {
                continue;
            }

            // 데이터 조회 (해당 Column + Row 조합)
            var query = chartQueryBuilder.buildSingleColumnChartDataQuery(
                    ctx, colDef.getField(), columnKey, rowDef.getField(),
                    metricDef, yCategories, columnFilters
            );

            Map<String, Double> valueMap = new HashMap<>();
            jdbc.query(query.sql(), query.params(), rs -> {
                String yKey = ValueUtils.normalizeKey(rs.getString("r"));
                double v = rs.getDouble("m");
                if (rs.wasNull()) v = 0.0;
                valueMap.put(yKey, v);
            });

            // ⚠️ 문제 부분: values를 2차원 배열로 만들어야 함!
            // 프론트엔드는 values[yIndex][xIndex] 형태를 기대
            // 다중 차트에서는 xIndex가 항상 0 (Column이 1개이므로)
            List<List<Double>> valuesMatrix = new ArrayList<>();
            for (String yKey : yCategories) {
                List<Double> rowValues = new ArrayList<>();
                // xIndex = 0 위치에 값 넣기
                rowValues.add(valueMap.getOrDefault(yKey, 0.0));
                valuesMatrix.add(rowValues);
            }

            log.info("Column: {}, yCategories: {}, valuesMatrix size: {}",
                    columnKey, yCategories, valuesMatrix.size());
            log.info("valuesMatrix: {}", valuesMatrix);

            PivotChartResponseDTO.SeriesDef seriesDef =
                    ChartSeriesFactory.buildSingleSeries(metricDef, valuesMatrix);

            log.info("SeriesDef created - id: {}, label: {}, values: {}",
                    seriesDef.getId(), seriesDef.getLabel(), seriesDef.getValues());

            PivotChartResponseDTO.ChartData chartData = PivotChartResponseDTO.ChartData.builder()
                    .columnKey(columnKey)
                    .yField(rowDef.getField())
                    .yCategories(yCategories)
                    .series(Collections.singletonList(seriesDef))
                    .build();

            log.info("ChartData built - columnKey: {}, series size: {}, first series: {}",
                    chartData.getColumnKey(),
                    chartData.getSeries() != null ? chartData.getSeries().size() : "null",
                    chartData.getSeries() != null && !chartData.getSeries().isEmpty() ? chartData.getSeries().get(0) : "empty");

            chartDataList.add(chartData);
        }


        return PivotChartResponseDTO.builder()
                .xField(colDef.getField())
                .yField(rowDef.getField())
                .charts(chartDataList)
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

    @Override
    public DrilldownTimeSeriesResponseDTO getDrilldownTimeSeries(
            PivotQueryContext ctx,
            DrilldownTimeSeriesRequestDTO req
    ) {
        validateDrilldownRequest(req);

        PivotQueryRequestDTO.ValueDef metricDef = req.getMetric();

        var query = chartQueryBuilder.buildDrilldownTimeSeriesQuery(ctx, req);

        Map<String, List<DrilldownTimeSeriesResponseDTO.Point>> seriesMap = new LinkedHashMap<>();
        List<Double> allValues = new ArrayList<>();

        Long[] globalMinTsHolder = new Long[1];
        Long[] globalMaxTsHolder = new Long[1];

        jdbc.query(query.sql(), query.params(), rs -> {
            String rowKey = ValueUtils.normalizeKey(rs.getString("row_key"));

            double tsSec = rs.getDouble("ts");
            long tsMs = (long) Math.floor(tsSec * 1000);

            double v = rs.getDouble("m");
            if (rs.wasNull()) v = 0.0;

            allValues.add(v);

            seriesMap
                    .computeIfAbsent(rowKey, k -> new ArrayList<>())
                    .add(DrilldownTimeSeriesResponseDTO.Point.builder()
                            .ts(tsMs)
                            .value(v)
                            .build()
                    );

            if (globalMinTsHolder[0] == null || tsMs < globalMinTsHolder[0]) {
                globalMinTsHolder[0] = tsMs;
            }
            if (globalMaxTsHolder[0] == null || tsMs > globalMaxTsHolder[0]) {
                globalMaxTsHolder[0] = tsMs;
            }
        });

        Long globalMinTs = globalMinTsHolder[0];
        Long globalMaxTs = globalMaxTsHolder[0];

        if (seriesMap.isEmpty()) {
            return DrilldownTimeSeriesResponseDTO.builder()
                    .timeField(req.getTimeField())
                    .metricField(metricDef.getField())
                    .metricAgg(metricDef.getAgg())
                    .globalMinTime(null)
                    .globalMaxTime(null)
                    .globalMedian(null)
                    .series(Collections.emptyList())
                    .seriesMedianMap(Collections.emptyMap())
                    .build();
        }

        Double globalMedian = computeMedian(allValues);

        Map<String, Double> seriesMedianMap = new LinkedHashMap<>();
        List<DrilldownTimeSeriesResponseDTO.SeriesDef> seriesList = new ArrayList<>();

        for (Map.Entry<String, List<DrilldownTimeSeriesResponseDTO.Point>> entry : seriesMap.entrySet()) {
            String rowKey = entry.getKey();
            List<DrilldownTimeSeriesResponseDTO.Point> points = entry.getValue();

            List<Double> values = new ArrayList<>(points.size());
            for (DrilldownTimeSeriesResponseDTO.Point p : points) {
                values.add(p.getValue());
            }

            Double median = computeMedian(values);
            seriesMedianMap.put(rowKey, median);

            seriesList.add(
                    DrilldownTimeSeriesResponseDTO.SeriesDef.builder()
                            .rowKey(rowKey)
                            .points(points)
                            .median(median)
                            .build()
            );
        }

        return DrilldownTimeSeriesResponseDTO.builder()
                .timeField(req.getTimeField())
                .metricField(metricDef.getField())
                .metricAgg(metricDef.getAgg())
                .globalMinTime(globalMinTs)
                .globalMaxTime(globalMaxTs)
                .globalMedian(globalMedian)
                .series(seriesList)
                .seriesMedianMap(seriesMedianMap)
                .build();
    }

    private void validateDrilldownRequest(DrilldownTimeSeriesRequestDTO req) {
        if (req.getLayer() == null || req.getLayer().isBlank()) {
            throw new BadRequestException("layer is required");
        }
        if (req.getColField() == null || req.getColField().isBlank()) {
            throw new BadRequestException("colField is required");
        }
        if (req.getRowField() == null || req.getRowField().isBlank()) {
            throw new BadRequestException("rowField is required");
        }
        if (req.getTimeField() == null || req.getTimeField().isBlank()) {
            throw new BadRequestException("timeField is required");
        }
        if (req.getMetric() == null || req.getMetric().getField() == null ||
                req.getMetric().getField().isBlank()) {
            throw new BadRequestException("metric is required");
        }
    }

    private Double computeMedian(List<Double> values) {
        if (values == null || values.isEmpty()) return null;
        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(Double::compareTo);
        int n = sorted.size();
        if (n % 2 == 1) {
            return sorted.get(n / 2);
        } else {
            return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
        }
    }
}
