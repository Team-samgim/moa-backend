package com.moa.api.chart.controller;

import com.moa.api.chart.dto.ChartThumbnailResponseDTO;
import com.moa.api.chart.dto.CreateChartThumbnailRequestDTO;
import com.moa.api.chart.entity.ChartThumbnail;
import com.moa.api.chart.service.ChartService;
import com.moa.api.chart.service.ChartThumbnailService;
import com.moa.api.chart.dto.PivotChartRequestDTO;
import com.moa.api.chart.dto.PivotHeatmapTableRequestDTO;
import com.moa.api.chart.dto.PivotChartResponseDTO;
import com.moa.api.chart.dto.PivotHeatmapTableResponseDTO;
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

    @PostMapping("/heatmap-table")
    public PivotHeatmapTableResponseDTO heatmapTable(@RequestBody PivotHeatmapTableRequestDTO req) {
        return chartService.getHeatmapTable(req);
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
