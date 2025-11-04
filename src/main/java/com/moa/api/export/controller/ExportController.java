package com.moa.api.export.controller;

import com.moa.api.export.dto.ExportCreateResponse;
import com.moa.api.export.dto.ExportGridRequest;
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

    @PostMapping("/grid")
    public ResponseEntity<ExportCreateResponse> exportGridCsv(@RequestBody ExportGridRequest req) throws Exception {
        // 필수값: memberId, layer, columns
        if (req.getLayer() == null || req.getColumns() == null || req.getColumns().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        ExportCreateResponse res = gridExportService.exportCsv(req);
        return ResponseEntity.ok(res);
    }
}