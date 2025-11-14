package com.moa.api.export.controller;

import com.moa.api.export.dto.ExportChartRequestDTO;
import com.moa.api.export.dto.ExportCreateResponseDTO;
import com.moa.api.export.dto.ExportGridRequestDTO;
import com.moa.api.export.service.ChartExportService;
import com.moa.api.export.service.GridExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exports")
@RequiredArgsConstructor
public class ExportController {

    private final GridExportService gridExportService;
    private final ChartExportService chartExportService;

    @PostMapping("/grid")
    public ResponseEntity<ExportCreateResponseDTO> exportGridCsv(@RequestBody ExportGridRequestDTO req) throws Exception {
        // 필수값: memberId, layer, columns
        if (req.getLayer() == null || req.getColumns() == null || req.getColumns().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        ExportCreateResponseDTO res = gridExportService.exportCsv(req);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/chart")
    public ExportCreateResponseDTO exportChart(@RequestBody ExportChartRequestDTO req) throws Exception {
        return chartExportService.exportChart(req);
    }
}