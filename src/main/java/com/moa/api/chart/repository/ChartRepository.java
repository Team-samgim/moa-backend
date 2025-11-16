package com.moa.api.chart.repository;

import com.moa.api.chart.dto.PivotChartRequestDTO;
import com.moa.api.chart.dto.PivotHeatmapTableRequestDTO;
import com.moa.api.chart.dto.PivotChartResponseDTO;
import com.moa.api.chart.dto.PivotHeatmapTableResponseDTO;
import com.moa.api.pivot.model.PivotQueryContext;

public interface ChartRepository {

    PivotChartResponseDTO getChart(PivotQueryContext ctx, PivotChartRequestDTO req);

    PivotHeatmapTableResponseDTO getHeatmapTable(PivotQueryContext ctx, PivotHeatmapTableRequestDTO req);
}
