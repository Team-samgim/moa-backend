package com.moa.api.chart.repository;

import com.moa.api.pivot.dto.request.PivotChartRequestDTO;
import com.moa.api.pivot.dto.request.PivotHeatmapTableRequestDTO;
import com.moa.api.pivot.dto.response.PivotChartResponseDTO;
import com.moa.api.pivot.dto.response.PivotHeatmapTableResponseDTO;
import com.moa.api.pivot.model.PivotQueryContext;

public interface ChartRepository {

    PivotChartResponseDTO getChart(PivotQueryContext ctx, PivotChartRequestDTO req);

    PivotHeatmapTableResponseDTO getHeatmapTable(PivotQueryContext ctx, PivotHeatmapTableRequestDTO req);
}
