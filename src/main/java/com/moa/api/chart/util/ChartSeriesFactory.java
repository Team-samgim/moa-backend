// 작성자: 최이서
package com.moa.api.chart.util;

import com.moa.api.pivot.dto.request.PivotQueryRequestDTO;
import com.moa.api.chart.dto.response.PivotChartResponseDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ChartSeriesFactory {

    private ChartSeriesFactory() {
    }

    public static List<List<Double>> buildValuesMatrix(
            List<String> xCategories,
            List<String> yCategories,
            Map<String, Map<String, Double>> valueMap
    ) {
        List<List<Double>> valuesMatrix = new ArrayList<>();

        for (String yKey : yCategories) {
            List<Double> rowValues = new ArrayList<>();
            Map<String, Double> rowMap = valueMap.get(yKey);

            for (String xKey : xCategories) {
                double v = 0.0;
                if (rowMap != null) {
                    Double vv = rowMap.get(xKey);
                    if (vv != null) v = vv;
                }
                rowValues.add(v);
            }
            valuesMatrix.add(rowValues);
        }

        return valuesMatrix;
    }

    public static PivotChartResponseDTO.SeriesDef buildSingleSeries(
            PivotQueryRequestDTO.ValueDef metricDef,
            List<List<Double>> valuesMatrix
    ) {
        String metricField = metricDef.getField();
        String metricAgg = metricDef.getAgg();
        String metricId = metricField + "::" + (metricAgg != null ? metricAgg : "agg");

        String metricLabel;
        if (metricDef.getAlias() != null && !metricDef.getAlias().isBlank()) {
            metricLabel = metricDef.getAlias();
        } else {
            String upperAgg = metricAgg != null ? metricAgg.toUpperCase() : "VAL";
            metricLabel = upperAgg + ": " + metricField;
        }

        return PivotChartResponseDTO.SeriesDef.builder()
                .id(metricId)
                .label(metricLabel)
                .field(metricField)
                .agg(metricAgg)
                .values(valuesMatrix)
                .build();
    }
}
