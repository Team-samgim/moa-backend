package com.moa.api.export.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.moa.api.export.config.ExportProperties;
import com.moa.api.export.dto.CsvPreviewDTO;
import com.moa.api.export.dto.DownloadUrlDTO;
import com.moa.api.export.dto.ExportFileItemDTO;
import com.moa.api.export.dto.ExportFileListResponseDTO;
import com.moa.api.export.entity.ExportFile;
import com.moa.api.export.exception.ExportException;
import com.moa.api.export.repository.ExportFileRepository;
import com.moa.api.preset.repository.PresetRepository;
import com.moa.global.aws.S3Uploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Export Query Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportQueryService {

    private final ExportFileRepository exportFileRepository;
    private final PresetRepository presetRepository;
    private final S3Uploader s3Uploader;
    private final S3Client s3Client;
    private final ExportProperties exportProperties;

    /**
     * Export 파일 목록 조회
     */
    @Transactional(readOnly = true)
    public ExportFileListResponseDTO list(String type, int page, int size) {
        log.debug("Fetching export file list: type={}, page={}, size={}", type, page, size);

        Long memberId = resolveMemberId();
        String normalizedType = normalizeType(type);

        // 페이지 파라미터 검증
        int validPage = Math.max(page, 0);
        int validSize = Math.max(Math.min(size, 100), 1); // 최대 100개

        Pageable pageable = PageRequest.of(validPage, validSize);

        Page<ExportFile> filePage = exportFileRepository
                .findByMember_IdAndExportTypeOrderByCreatedAtDesc(
                        memberId,
                        normalizedType,
                        pageable
                );

        List<ExportFileItemDTO> items = filePage.getContent().stream()
                .map(this::convertToItemDTO)
                .collect(Collectors.toList());

        log.debug("Found {} export files for member {}", items.size(), memberId);

        return ExportFileListResponseDTO.builder()
                .items(items)
                .page(filePage.getNumber())
                .size(filePage.getSize())
                .totalPages(filePage.getTotalPages())
                .totalItems(filePage.getTotalElements())
                .build();
    }

    /**
     * Export 파일 삭제
     */
    @Transactional
    public void delete(Long exportId) {
        log.info("Deleting export file: exportId={}", exportId);

        Long memberId = resolveMemberId();

        ExportFile exportFile = exportFileRepository
                .findByExportIdAndMember_Id(exportId, memberId)
                .orElseThrow(() -> new ExportException(
                        ExportException.ErrorCode.FILE_NOT_FOUND,
                        "파일을 찾을 수 없거나 접근 권한이 없습니다: exportId=" + exportId
                ));

        // 프리셋 정보 백업
        Integer presetId = extractPresetId(exportFile);

        // S3 객체 삭제 시도 (실패해도 계속 진행)
        deleteS3ObjectSafely(exportFile);

        // DB에서 파일 삭제
        exportFileRepository.delete(exportFile);
        log.info("Deleted export file from DB: exportId={}", exportId);

        // 고아 프리셋 삭제
        deleteOrphanPresetIfNeeded(presetId, memberId);

        log.info("Export file deletion completed: exportId={}", exportId);
    }

    /**
     * 다운로드 URL 생성 (Presigned URL)
     */
    @Transactional(readOnly = true)
    public DownloadUrlDTO presign(Long exportId) {
        log.debug("Generating presigned URL for export file: exportId={}", exportId);

        Long memberId = resolveMemberId();

        ExportFile exportFile = exportFileRepository
                .findByExportIdAndMember_Id(exportId, memberId)
                .orElseThrow(() -> new ExportException(
                        ExportException.ErrorCode.FILE_NOT_FOUND,
                        "파일을 찾을 수 없거나 접근 권한이 없습니다: exportId=" + exportId
                ));

        try {
            String url = s3Uploader.presign(
                    exportFile.getBucket(),
                    exportFile.getObjectKey(),
                    Duration.ofMinutes(exportProperties.getPresign().getDownloadExpirationMinutes())
            );

            log.debug("Generated presigned URL for exportId={}", exportId);

            return DownloadUrlDTO.builder()
                    .httpUrl(url)
                    .build();

        } catch (Exception e) {
            log.error("Failed to generate presigned URL for exportId={}", exportId, e);
            throw new ExportException(
                    ExportException.ErrorCode.S3_DOWNLOAD_FAILED,
                    e
            );
        }
    }

    /**
     * CSV 미리보기
     */
    @Transactional(readOnly = true)
    public CsvPreviewDTO preview(Long exportId, int limit) {
        log.debug("Generating CSV preview: exportId={}, limit={}", exportId, limit);

        Long memberId = resolveMemberId();

        ExportFile exportFile = exportFileRepository
                .findByExportIdAndMember_Id(exportId, memberId)
                .orElseThrow(() -> new ExportException(
                        ExportException.ErrorCode.FILE_NOT_FOUND,
                        "파일을 찾을 수 없거나 접근 권한이 없습니다: exportId=" + exportId
                ));

        // CSV가 아니면 에러
        if (!"CSV".equalsIgnoreCase(exportFile.getFileFormat())) {
            throw new ExportException(
                    ExportException.ErrorCode.INVALID_REQUEST,
                    "CSV 파일만 미리보기가 가능합니다: format=" + exportFile.getFileFormat()
            );
        }

        try {
            return readCsvPreview(exportFile, Math.max(1, Math.min(limit, 100)));

        } catch (ExportException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to read CSV preview for exportId={}", exportId, e);
            throw new ExportException(
                    ExportException.ErrorCode.S3_DOWNLOAD_FAILED,
                    e
            );
        }
    }

    /* ========== Private Helper Methods ========== */

    /**
     * 인증된 사용자 ID 조회
     */
    private Long resolveMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new ExportException(ExportException.ErrorCode.UNAUTHORIZED);
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof Long memberId) {
            return memberId;
        }

        throw new ExportException(
                ExportException.ErrorCode.UNAUTHORIZED,
                "지원하지 않는 principal 타입: " + principal.getClass().getName()
        );
    }

    /**
     * Export Type 정규화
     */
    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return "GRID";
        }

        String normalized = type.trim().toUpperCase();

        // 하위 호환성: SEARCH -> GRID
        if ("SEARCH".equals(normalized)) {
            return "GRID";
        }

        return switch (normalized) {
            case "GRID", "PIVOT", "CHART" -> normalized;
            default -> "GRID";
        };
    }

    /**
     * ExportFile -> ExportFileItemDTO 변환
     */
    private ExportFileItemDTO convertToItemDTO(ExportFile file) {
        JsonNode config = extractPresetConfig(file);
        String layer = extractLayer(config);
        Integer presetId = extractPresetId(file);

        return ExportFileItemDTO.builder()
                .fileId(file.getExportId())
                .fileName(file.getFileName())
                .exportType(file.getExportType())
                .layer(layer)
                .createdAt(file.getCreatedAt())
                .presetId(presetId)
                .config(config)
                .build();
    }

    /**
     * Preset Config 추출
     */
    private JsonNode extractPresetConfig(ExportFile file) {
        if (file.getPreset() == null) {
            return null;
        }
        return file.getPreset().getConfig();
    }

    /**
     * Layer 추출
     */
    private String extractLayer(JsonNode config) {
        if (config == null || !config.hasNonNull("layer")) {
            return null;
        }
        return config.get("layer").asText();
    }

    /**
     * Preset ID 추출
     */
    private Integer extractPresetId(ExportFile file) {
        if (file.getPreset() == null) {
            return null;
        }
        return file.getPreset().getPresetId();
    }

    /**
     * S3 객체 안전하게 삭제 (실패해도 예외 던지지 않음)
     */
    private void deleteS3ObjectSafely(ExportFile file) {
        try {
            s3Uploader.delete(file.getBucket(), file.getObjectKey());
            log.debug("Deleted S3 object: bucket={}, key={}",
                    file.getBucket(), file.getObjectKey());
        } catch (Exception e) {
            log.warn("Failed to delete S3 object (continuing): bucket={}, key={}, error={}",
                    file.getBucket(), file.getObjectKey(), e.getMessage());
        }
    }

    /**
     * 고아 프리셋 삭제
     */
    private void deleteOrphanPresetIfNeeded(Integer presetId, Long memberId) {
        if (presetId == null) {
            return;
        }

        // 해당 프리셋을 참조하는 다른 파일이 있는지 확인
        long remainingReferences = exportFileRepository.countByPreset_PresetId(presetId);

        if (remainingReferences == 0L) {
            // 더 이상 참조하는 파일이 없으면 프리셋 삭제
            try {
                int affected = presetRepository.deleteOne(memberId, presetId);

                if (affected > 0) {
                    log.info("Deleted orphan preset: presetId={}", presetId);
                } else {
                    log.debug("Preset not deleted (already deleted or not owner): presetId={}",
                            presetId);
                }
            } catch (Exception e) {
                log.warn("Failed to delete orphan preset (ignoring): presetId={}, error={}",
                        presetId, e.getMessage());
            }
        } else {
            log.debug("Preset preserved (still referenced): presetId={}, references={}",
                    presetId, remainingReferences);
        }
    }

    /**
     * CSV 미리보기 읽기
     */
    private CsvPreviewDTO readCsvPreview(ExportFile file, int limit) throws Exception {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(file.getBucket())
                .key(file.getObjectKey())
                .build();

        try (var inputStream = s3Client.getObject(getRequest);
             var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {

            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build();

            try (CSVParser parser = new CSVParser(reader, format)) {
                List<String> headers = parser.getHeaderNames();
                List<Map<String, String>> rows = new ArrayList<>();

                int count = 0;
                for (CSVRecord record : parser) {
                    if (count >= limit) {
                        break;
                    }

                    Map<String, String> row = new LinkedHashMap<>();
                    for (String header : headers) {
                        row.put(header, record.get(header));
                    }

                    rows.add(row);
                    count++;
                }

                log.debug("CSV preview generated: exportId={}, headers={}, rows={}",
                        file.getExportId(), headers.size(), rows.size());

                return CsvPreviewDTO.builder()
                        .columns(headers)
                        .rows(rows)
                        .build();
            }
        }
    }
}