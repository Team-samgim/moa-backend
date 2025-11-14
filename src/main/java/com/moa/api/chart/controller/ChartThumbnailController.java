package com.moa.api.chart.controller;

import com.moa.api.chart.dto.ChartThumbnailResponseDTO;
import com.moa.api.chart.dto.CreateChartThumbnailRequestDTO;
import com.moa.api.chart.entity.ChartThumbnail;
import com.moa.api.chart.service.ChartThumbnailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chart-thumbnails")
@RequiredArgsConstructor
public class ChartThumbnailController {

    private final ChartThumbnailService chartThumbnailService;

    @PostMapping
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
