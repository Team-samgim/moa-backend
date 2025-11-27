package com.moa.api.export.controller;

/*****************************************************************************
 CLASS NAME    : ExportController
 DESCRIPTION   : Grid / Chart / Pivot Export REST API 컨트롤러
 AUTHOR        : 방대혁, 최이서
 ******************************************************************************/

import com.moa.api.export.dto.ExportChartRequestDTO;
import com.moa.api.export.dto.ExportCreateResponseDTO;
import com.moa.api.export.dto.ExportGridRequestDTO;
import com.moa.api.export.dto.ExportPivotRequestDTO;
import com.moa.api.export.service.ChartExportService;
import com.moa.api.export.service.GridExportService;
import com.moa.api.export.service.PivotExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/exports")
@RequiredArgsConstructor
@Validated
public class ExportController {

    private final GridExportService gridExportService;
    private final ChartExportService chartExportService;
    private final PivotExportService pivotExportService;

    /**
     * Grid CSV Export
     *
     * @param req Export 요청 (layer, columns 필수)
     * @return Export 결과 (S3 URL 포함)
     */
    @PostMapping("/grid")
    public ResponseEntity<ExportCreateResponseDTO> exportGridCsv(
            @RequestBody ExportGridRequestDTO req
    ) {
        log.info("POST /api/exports/grid - layer={}, columns={}",
                req.getLayer(), req.getColumns());

        ExportCreateResponseDTO response = gridExportService.exportCsv(req);

        log.info("Grid CSV export completed: exportId={}", response.getExportId());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/chart")
    public ExportCreateResponseDTO exportChart(@RequestBody ExportChartRequestDTO req) throws Exception {
        return chartExportService.exportChart(req);
    }

    @PostMapping("/pivot")
    public ResponseEntity<ExportCreateResponseDTO> exportPivotCsv(@RequestBody ExportPivotRequestDTO req) throws Exception {
        if (req.getPresetId() == null || req.getPivot() == null) {
            return ResponseEntity.badRequest().build();
        }
        ExportCreateResponseDTO res = pivotExportService.exportCsv(req);
        return ResponseEntity.ok(res);
    }
}
