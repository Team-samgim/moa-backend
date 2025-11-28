// 작성자: 최이서
package com.moa.api.chart.repository;

import com.moa.api.chart.dto.request.DrilldownTimeSeriesRequestDTO;
import com.moa.api.chart.dto.request.PivotChartRequestDTO;
import com.moa.api.chart.dto.request.PivotHeatmapTableRequestDTO;
import com.moa.api.chart.dto.response.DrilldownTimeSeriesResponseDTO;
import com.moa.api.chart.dto.response.PivotChartByColumnResponseDTO;
import com.moa.api.chart.dto.response.PivotChartResponseDTO;
import com.moa.api.chart.dto.response.PivotHeatmapTableResponseDTO;
import com.moa.api.pivot.model.PivotQueryContext;

public interface ChartRepository {

    PivotChartResponseDTO getChart(PivotQueryContext ctx, PivotChartRequestDTO req);

//    PivotChartByColumnResponseDTO getChartByColumn(PivotQueryContext ctx, PivotChartRequestDTO req);

    PivotHeatmapTableResponseDTO getHeatmapTable(PivotQueryContext ctx, PivotHeatmapTableRequestDTO req);

    DrilldownTimeSeriesResponseDTO getDrilldownTimeSeries(
            PivotQueryContext ctx,
            DrilldownTimeSeriesRequestDTO req
    );
}
