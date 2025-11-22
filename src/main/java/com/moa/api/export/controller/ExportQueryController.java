package com.moa.api.export.controller;

import com.moa.api.export.dto.CsvPreviewDTO;
import com.moa.api.export.dto.DownloadUrlDTO;
import com.moa.api.export.dto.ExportFileListResponseDTO;
import com.moa.api.export.service.ExportQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Export Query Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/files/exports")
@RequiredArgsConstructor
@Validated
public class ExportQueryController {

    private final ExportQueryService exportQueryService;

    /**
     * Export 파일 목록 조회
     *
     * @param type Export 타입 (GRID, PIVOT, CHART)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return Export 파일 목록
     */
    @GetMapping
    public ResponseEntity<ExportFileListResponseDTO> list(
            @RequestParam(defaultValue = "GRID") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("GET /api/files/exports - type={}, page={}, size={}", type, page, size);

        ExportFileListResponseDTO response = exportQueryService.list(type, page, size);

        log.debug("Found {} export files (page {}/{})",
                response.getItems().size(), response.getPage(), response.getTotalPages());

        return ResponseEntity.ok(response);
    }

    /**
     * Export 파일 삭제
     *
     * @param fileId Export 파일 ID
     * @return 204 No Content
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> delete(@PathVariable Long fileId) {
        log.info("DELETE /api/files/exports/{}", fileId);

        exportQueryService.delete(fileId);

        log.info("Export file deleted successfully: fileId={}", fileId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Export 파일 다운로드 URL 생성
     *
     * @param fileId Export 파일 ID
     * @return Presigned URL
     */
    @GetMapping("/{fileId}/download")
    public ResponseEntity<DownloadUrlDTO> download(@PathVariable Long fileId) {
        log.info("GET /api/files/exports/{}/download", fileId);

        DownloadUrlDTO response = exportQueryService.presign(fileId);

        log.debug("Generated download URL for fileId={}", fileId);

        return ResponseEntity.ok(response);
    }

    /**
     * CSV 미리보기
     *
     * @param fileId Export 파일 ID
     * @param limit 미리보기 행 수 (최대 100)
     * @return CSV 미리보기 데이터
     */
    @GetMapping("/{fileId}/preview")
    public ResponseEntity<CsvPreviewDTO> preview(
            @PathVariable Long fileId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        log.info("GET /api/files/exports/{}/preview - limit={}", fileId, limit);

        CsvPreviewDTO response = exportQueryService.preview(fileId, limit);

        log.debug("Generated CSV preview: fileId={}, columns={}, rows={}",
                fileId, response.getColumns().size(), response.getRows().size());

        return ResponseEntity.ok(response);
    }
}