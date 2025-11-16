package com.moa.api.chart.util;

import com.moa.api.pivot.dto.request.PivotChartRequestDTO;
import com.moa.api.pivot.dto.request.PivotQueryRequestDTO;
import com.moa.api.pivot.model.PivotQueryContext;
import com.moa.api.pivot.util.PivotChartQueryBuilder;
import com.moa.api.pivot.util.ValueUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ChartAxisResolver {

    private final NamedParameterJdbcTemplate jdbc;
    private final PivotChartQueryBuilder chartQueryBuilder;

    public List<String> resolveAxisKeys(
            PivotQueryContext ctx,
            PivotChartRequestDTO.AxisDef axisDef,
            PivotQueryRequestDTO.ValueDef metricDef,
            List<PivotQueryRequestDTO.FilterDef> baseFilters,
            int maxCount
    ) {
        if (axisDef == null || axisDef.getField() == null || axisDef.getField().isBlank()) {
            return Collections.emptyList();
        }

        String mode = axisDef.getMode() != null ? axisDef.getMode().toLowerCase() : "topn";

        if ("manual".equals(mode)) {
            return resolveManualAxis(axisDef, maxCount);
        }

        return resolveTopNAxis(ctx, axisDef, metricDef, baseFilters, maxCount);
    }

    private List<String> resolveManualAxis(PivotChartRequestDTO.AxisDef axisDef, int maxCount) {
        List<String> selected = axisDef.getSelectedItems();
        if (selected == null || selected.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        for (String v : selected) {
            if (v == null || v.isBlank()) {
                result.add("(empty)");
            } else {
                result.add(v);
            }
            if (result.size() >= maxCount) break;
        }
        return result;
    }

    private List<String> resolveTopNAxis(
            PivotQueryContext ctx,
            PivotChartRequestDTO.AxisDef axisDef,
            PivotQueryRequestDTO.ValueDef metricDef,
            List<PivotQueryRequestDTO.FilterDef> baseFilters,
            int maxCount
    ) {
        int n = 5;
        if (axisDef.getTopN() != null && axisDef.getTopN().getN() != null && axisDef.getTopN().getN() > 0) {
            n = axisDef.getTopN().getN();
        }
        if (n > maxCount) {
            n = maxCount;
        }

        var query = chartQueryBuilder.buildTopNAxisQuery(ctx, axisDef, metricDef, baseFilters, n);

        List<String> result = new ArrayList<>();
        jdbc.query(query.sql(), query.params(), rs -> {
            String key = ValueUtils.normalizeKey(rs.getString("k"));
            result.add(key);
        });

        return result;
    }
}
