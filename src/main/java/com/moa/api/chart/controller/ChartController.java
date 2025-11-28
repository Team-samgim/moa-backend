// 작성자: 최이서
package com.moa.api.chart.controller;

import com.moa.api.chart.dto.request.DrilldownTimeSeriesRequestDTO;
import com.moa.api.chart.dto.response.*;
import com.moa.api.chart.dto.request.CreateChartThumbnailRequestDTO;
import com.moa.api.chart.entity.ChartThumbnail;
import com.moa.api.chart.service.ChartService;
import com.moa.api.chart.service.ChartThumbnailService;
import com.moa.api.chart.dto.request.PivotChartRequestDTO;
import com.moa.api.chart.dto.request.PivotHeatmapTableRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chart")
@RequiredArgsConstructor
public class ChartController {

    private final ChartService chartService;
    private final ChartThumbnailService chartThumbnailService;

    @PostMapping
    public PivotChartResponseDTO chart(@RequestBody PivotChartRequestDTO req) {
        return chartService.getChart(req);
    }

//    @PostMapping
//    public PivotChartByColumnResponseDTO chart(@RequestBody PivotChartRequestDTO req) {
//        return chartService.getChartByColumn(req);
//    }

    @PostMapping("/heatmap-table")
    public PivotHeatmapTableResponseDTO heatmapTable(@RequestBody PivotHeatmapTableRequestDTO req) {
        return chartService.getHeatmapTable(req);
    }

    @PostMapping("/drilldown-time-series")
    public DrilldownTimeSeriesResponseDTO drilldownTimeSeries(
            @RequestBody DrilldownTimeSeriesRequestDTO req
    ) {
        return chartService.getDrilldownTimeSeries(req);
    }

    @PostMapping("/thumbnails")
    public ChartThumbnailResponseDTO create(@RequestBody CreateChartThumbnailRequestDTO req) {
        ChartThumbnail t = chartThumbnailService.create(req);
        return ChartThumbnailResponseDTO.builder()
                .thumbnailId(t.getThumbnailId())
                .exportId(t.getExportFile().getExportId())
                .bucket(t.getBucket())
                .objectKey(t.getObjectKey())
                .build();
    }
}
