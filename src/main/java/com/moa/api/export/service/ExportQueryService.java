package com.moa.api.export.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.moa.api.export.dto.CsvPreviewDTO;
import com.moa.api.export.dto.DownloadUrlDTO;
import com.moa.api.export.dto.ExportFileItemDTO;
import com.moa.api.export.dto.ExportFileListResponseDTO;
import com.moa.api.export.entity.ExportFile;
import com.moa.api.export.repository.ExportFileRepository;
import com.moa.api.preset.repository.PresetRepository;
import com.moa.global.aws.S3Props;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportQueryService {

    private final ExportFileRepository repo;
    private final PresetRepository presetRepo;
    private final S3Uploader s3Uploader;
    private final S3Client s3;
    private final S3Props s3Props;

    // type: SEARCH|PIVOT|CHART -> GRID|PIVOT|CHART
    private String mapType(String t) {
        if (t == null) return "GRID";
        String k = t.trim().toUpperCase();
        return switch (k) {
            case "SEARCH", "GRID" -> "GRID";
            case "PIVOT" -> "PIVOT";
            case "CHART" -> "CHART";
            default -> "GRID";
        };
    }

    private Long me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new IllegalStateException("인증 정보 없음");
        Object p = auth.getPrincipal();
        if (p instanceof Long id) return id;
        throw new IllegalStateException("지원하지 않는 principal: " + p);
    }

    @Transactional(readOnly = true)
    public ExportFileListResponseDTO list(String type, int page, int size) {
        Long memberId = me();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        String t = normalizeType(type);
        Page<ExportFile> slice =
                repo.findByMember_IdAndExportTypeOrderByCreatedAtDesc(memberId, t, pageable);

        var items = slice.getContent().stream().map(f -> {
            JsonNode cfg = (f.getPreset() != null) ? f.getPreset().getConfig() : null;
            String layer = (cfg != null && cfg.hasNonNull("layer")) ? cfg.get("layer").asText() : null;
            Integer presetId = (f.getPreset() != null) ? f.getPreset().getPresetId() : null;
            return ExportFileItemDTO.builder()
                    .fileId(f.getExportId())
                    .fileName(f.getFileName())
                    .exportType(f.getExportType())
                    .layer(layer)
                    .createdAt(f.getCreatedAt())
                    .presetId(presetId)
                    .config(cfg) // 필요 없으면 null로 바꿔도 됨
                    .build();
        }).collect(Collectors.toList());

        return ExportFileListResponseDTO.builder()
                .items(items)
                .page(slice.getNumber())
                .size(slice.getSize())
                .totalPages(slice.getTotalPages())
                .totalItems(slice.getTotalElements())
                .build();
    }

    @Transactional
    public void delete(Long exportId) {
        Long memberId = me();
        ExportFile f = repo.findByExportIdAndMember_Id(exportId, memberId)
                .orElseThrow(() -> new NoSuchElementException("파일이 없거나 권한이 없습니다."));

        // S3 객체 먼저 시도 삭제(실패해도 파일/프리셋 삭제는 진행)
        try {
            s3Uploader.delete(f.getBucket(), f.getObjectKey());
        } catch (Exception e) {
            log.warn("S3 삭제 실패(무시): {}", e.toString());
        }

        // 현재 파일이 참조하던 preset 백업
        var preset = f.getPreset();
        Integer presetId = (preset != null) ? preset.getPresetId() : null;

        // 1) 파일 삭제
        repo.delete(f);

        // 2) 프리셋 고아 여부 확인 후 삭제
        if (presetId != null) {
            long remain = repo.countByPreset_PresetId(presetId);
            if (remain == 0L) {
                // 다른 파일이 더 이상 참조하지 않으면 프리셋 삭제
                try {
                    int affected = presetRepo.deleteOne(memberId, presetId);
                    if (affected == 0) {
                        log.debug("프리셋 삭제 없음(이미 삭제되었거나 소유자 불일치): presetId={}", presetId);
                    }
                } catch (Exception e) {
                    log.warn("프리셋 삭제 실패(무시): presetId={}, err={}", presetId, e.toString());
                }
            } else {
                log.debug("프리셋 보존: presetId={}, 남은 참조 파일 수={}", presetId, remain);
            }
        }
    }

    @Transactional(readOnly = true)
    public DownloadUrlDTO presign(Long exportId) {
        Long memberId = me();
        ExportFile f = repo.findByExportIdAndMember_Id(exportId, memberId)
                .orElseThrow(() -> new NoSuchElementException("파일이 없거나 권한이 없습니다."));
        String url = s3Uploader.presign(f.getBucket(), f.getObjectKey(), Duration.ofMinutes(5));
        return DownloadUrlDTO.builder().httpUrl(url).build();
    }

    @Transactional(readOnly = true)
    public CsvPreviewDTO preview(Long exportId, int limit) throws Exception {
        Long memberId = me();
        ExportFile f = repo.findByExportIdAndMember_Id(exportId, memberId)
                .orElseThrow(() -> new NoSuchElementException("파일이 없거나 권한이 없습니다."));

        // S3에서 바로 스트리밍 파싱 (헤더 + 상위 N행)
        var get = GetObjectRequest.builder()
                .bucket(f.getBucket())
                .key(f.getObjectKey())
                .build();

        try (var in = s3.getObject(get);
             var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {

            CSVFormat fmt = CSVFormat.DEFAULT.builder()
                    .setHeader()                   // 첫 줄을 헤더로
                    .setSkipHeaderRecord(true)
                    .build();

            try (CSVParser parser = new CSVParser(reader, fmt)) {
                List<String> headers = parser.getHeaderNames();
                List<Map<String, String>> rows = new ArrayList<>();

                int i = 0;
                for (CSVRecord r : parser) {
                    Map<String, String> row = new LinkedHashMap<>();
                    for (String h : headers) row.put(h, r.get(h));
                    rows.add(row);
                    if (++i >= Math.max(1, limit)) break;
                }

                return CsvPreviewDTO.builder()
                        .columns(headers)
                        .rows(rows)
                        .build();
            }
        }
    }

    private String normalizeType(String t) {
        if (t == null) return "GRID";
        String k = t.trim().toUpperCase();
        if (k.equals("SEARCH")) return "GRID"; // 임시 하위호환
        return switch (k) {
            case "GRID", "PIVOT", "CHART" -> k;
            default -> "GRID";
        };
    }

}