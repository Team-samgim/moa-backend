package com.moa.api.export.controller;

import com.moa.api.export.dto.CsvPreviewDTO;
import com.moa.api.export.dto.DownloadUrlDTO;
import com.moa.api.export.dto.ExportFileListResponseDTO;
import com.moa.api.export.service.ExportQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/files/exports") // ← /api 접두사 포함
@RequiredArgsConstructor
public class ExportQueryController {

    private final ExportQueryService svc;

    @GetMapping
    public ResponseEntity<ExportFileListResponseDTO> list(
            @RequestParam(defaultValue = "GRID") String type, // GRID | PIVOT | CHART
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(svc.list(type, page, size));
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> delete(@PathVariable Long fileId) {
        svc.delete(fileId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<DownloadUrlDTO> download(@PathVariable Long fileId) {
        return ResponseEntity.ok(svc.presign(fileId));
    }

    @GetMapping("/{fileId}/preview")
    public ResponseEntity<CsvPreviewDTO> preview(
            @PathVariable Long fileId,
            @RequestParam(defaultValue = "20") int limit
    ) throws Exception {
        return ResponseEntity.ok(svc.preview(fileId, limit));
    }
}