package com.moa.api.chart.util;

import com.moa.api.chart.dto.request.PivotHeatmapTableRequestDTO;
import com.moa.api.pivot.dto.request.PivotQueryRequestDTO;
import com.moa.api.pivot.model.PivotQueryContext;
import com.moa.api.pivot.util.PivotHeatmapQueryBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class HeatmapTableLoader {

    private final NamedParameterJdbcTemplate jdbc;
    private final PivotHeatmapQueryBuilder heatmapQueryBuilder;

    private static final int HEATMAP_TABLE_MAX_X = 50;
    private static final int DEFAULT_PAGE_LIMIT = 100;

    public int resolveOffset(PivotHeatmapTableRequestDTO req) {
        if (req.getPage() != null && req.getPage().getOffset() != null && req.getPage().getOffset() >= 0) {
            return req.getPage().getOffset();
        }
        return 0;
    }

    public int resolveLimit(PivotHeatmapTableRequestDTO req) {
        if (req.getPage() != null && req.getPage().getLimit() != null && req.getPage().getLimit() > 0) {
            return req.getPage().getLimit();
        }
        return DEFAULT_PAGE_LIMIT;
    }

    public List<String> fetchXCategories(
            PivotQueryContext ctx,
            String colField,
            PivotQueryRequestDTO.ValueDef metric
    ) {
        var query = heatmapQueryBuilder.buildXCategoriesQuery(ctx, colField, metric, HEATMAP_TABLE_MAX_X);
        List<String> xCategories = new ArrayList<>();

        jdbc.query(query.sql(), query.params(), rs -> {
            String cx = rs.getString("cx");
            if (cx == null || cx.isBlank()) {
                cx = "(empty)";
            }
            xCategories.add(cx);
        });

        return xCategories;
    }

    public long fetchYTotalCount(PivotQueryContext ctx, String rowField) {
        var query = heatmapQueryBuilder.buildYCountQuery(ctx, rowField);
        Long count = jdbc.queryForObject(query.sql(), query.params(), Long.class);
        return count != null ? count : 0L;
    }

    public List<String> fetchYCategoriesPage(
            PivotQueryContext ctx,
            String rowField,
            PivotQueryRequestDTO.ValueDef metric,
            int offset,
            int limit
    ) {
        var query = heatmapQueryBuilder.buildYCategoriesPageQuery(ctx, rowField, metric, offset, limit);
        List<String> yCategories = new ArrayList<>();

        jdbc.query(query.sql(), query.params(), rs -> {
            String ry = rs.getString("ry");
            if (ry == null || ry.isBlank()) {
                ry = "(empty)";
            }
            yCategories.add(ry);
        });

        return yCategories;
    }

    public HeatmapCellResult fetchCellValues(
            PivotQueryContext ctx,
            String colField,
            String rowField,
            PivotQueryRequestDTO.ValueDef metric,
            List<String> xCategories,
            List<String> yCategories
    ) {
        Map<String, Integer> xPos = new HashMap<>();
        for (int i = 0; i < xCategories.size(); i++) {
            xPos.put(xCategories.get(i), i);
        }
        Map<String, Integer> yPos = new HashMap<>();
        for (int i = 0; i < yCategories.size(); i++) {
            yPos.put(yCategories.get(i), i);
        }

        List<List<Double>> values = new ArrayList<>();
        List<Double> rowTotals = new ArrayList<>();
        for (int yi = 0; yi < yCategories.size(); yi++) {
            List<Double> row = new ArrayList<>();
            for (int xi = 0; xi < xCategories.size(); xi++) {
                row.add(0.0);
            }
            values.add(row);
            rowTotals.add(0.0);
        }

        final Double[] pageMinHolder = new Double[1];
        final Double[] pageMaxHolder = new Double[1];

        var query = heatmapQueryBuilder.buildCellValuesQuery(ctx, colField, rowField, metric, xCategories, yCategories);

        jdbc.query(query.sql(), query.params(), rs -> {
            String cx = rs.getString("cx");
            if (cx == null || cx.isBlank()) {
                cx = "(empty)";
            }
            String ry = rs.getString("ry");
            if (ry == null || ry.isBlank()) {
                ry = "(empty)";
            }

            Integer xi = xPos.get(cx);
            Integer yi = yPos.get(ry);
            if (xi == null || yi == null) return;

            double v = rs.getDouble("m0");
            if (rs.wasNull()) v = 0.0;

            double old = values.get(yi).get(xi);
            double newVal = old + v;
            values.get(yi).set(xi, newVal);

            double oldRowTotal = rowTotals.get(yi);
            rowTotals.set(yi, oldRowTotal + v);

            Double curMin = pageMinHolder[0];
            Double curMax = pageMaxHolder[0];

            if (curMin == null || newVal < curMin) {
                pageMinHolder[0] = newVal;
            }
            if (curMax == null || newVal > curMax) {
                pageMaxHolder[0] = newVal;
            }
        });

        return new HeatmapCellResult(
                values,
                rowTotals,
                pageMinHolder[0],
                pageMaxHolder[0]
        );
    }

    public record HeatmapCellResult(
            List<List<Double>> values,
            List<Double> rowTotals,
            Double pageMin,
            Double pageMax
    ) {}
}
