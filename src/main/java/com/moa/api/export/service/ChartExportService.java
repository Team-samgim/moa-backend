package com.moa.api.export.service;

import com.moa.api.export.dto.ExportChartRequestDTO;
import com.moa.api.export.dto.ExportCreateResponseDTO;
import com.moa.api.export.entity.ExportFile;
import com.moa.api.export.repository.ExportFileRepository;
import com.moa.api.member.entity.Member;
import com.moa.api.preset.entity.Preset;
import com.moa.api.preset.entity.PresetOrigin;
import com.moa.api.preset.entity.PresetType;
import com.moa.api.preset.repository.PresetRepository;
import com.moa.global.aws.S3Props;
import com.moa.global.aws.S3Uploader;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChartExportService {

    private final ExportFileRepository exportFileRepository;
    private final PresetRepository presetRepository;
    private final EntityManager em;
    private final S3Uploader s3Uploader;
    private final S3Props s3Props;

    private static final DateTimeFormatter NAME_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Transactional
    public ExportCreateResponseDTO exportChart(ExportChartRequestDTO req) throws Exception {
        if (req.getConfig() == null || req.getConfig().isEmpty()) {
            throw new IllegalArgumentException("config is required");
        }
        if (req.getImageBase64() == null || req.getImageBase64().isBlank()) {
            throw new IllegalArgumentException("imageBase64 is required");
        }

        Long memberId = resolveMemberId();
        Member memberRef = em.getReference(Member.class, memberId);

        // 1) CHART 프리셋 생성 (preset_type=CHART, origin=EXPORT)
        String presetName = Optional.ofNullable(req.getFileName())
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .orElseGet(() -> "차트 내보내기 " + LocalDateTime.now().format(NAME_FMT));

        Map<String, Object> cfg = req.getConfig();

        Integer presetId = presetRepository.insert(
                memberId,
                presetName,
                PresetType.CHART.name(),   // CHART
                cfg,
                false,
                PresetOrigin.EXPORT        // EXPORT
        );
        Preset presetRef = em.getReference(Preset.class, presetId);

        // 2) PNG 파일 생성 후 S3 업로드
        final String bucket = s3Props.getS3().getBucket();
        final String rootPrefix = normalizePrefix(s3Props.getS3().getPrefix());

        String safeBase = Optional.ofNullable(req.getFileName())
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .orElseGet(() -> "chart_" + LocalDateTime.now().format(NAME_FMT));

        String objectKey = buildObjectKey(rootPrefix, "chart", memberId, safeBase + ".png");

        byte[] pngBytes = Base64.getDecoder().decode(req.getImageBase64());

        Path tmp = Files.createTempFile("chart-export-", ".png");
        try {
            Files.write(tmp, pngBytes);
            s3Uploader.upload(bucket, objectKey, tmp, "image/png");
        } finally {
            try {
                Files.deleteIfExists(tmp);
            } catch (Exception ignore) {}
        }

        // 3) 프리사인드 URL
        String httpUrl = s3Uploader.presign(bucket, objectKey, Duration.ofMinutes(10));

        // 4) export_files INSERT
        ExportFile row = ExportFile.builder()
                .member(memberRef)
                .preset(presetRef)
                .exportType("CHART")
                .fileFormat("PNG")
                .fileName(safeBase + ".png")
                .bucket(bucket)
                .objectKey(objectKey)
                .exportStatus("SUCCESS")
                .createdAt(LocalDateTime.now())
                .build();

        exportFileRepository.save(row);

        return ExportCreateResponseDTO.builder()
                .exportId(row.getExportId())
                .bucket(bucket)
                .objectKey(objectKey)
                .s3Url("s3://" + bucket + "/" + objectKey)
                .httpUrl(httpUrl)
                .status(row.getExportStatus())
                .build();
    }

    private String normalizePrefix(String p) {
        if (p == null || p.isBlank()) return "";
        String x = p.trim();
        return x.endsWith("/") ? x : x + "/";
    }

    private String buildObjectKey(String rootPrefix, String typePrefix, Long memberId, String fileName) {
        LocalDateTime now = LocalDateTime.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = java.util.UUID.randomUUID().toString();

        String key = String.format(
                "%s%s/%s/%d/%s/%s",
                rootPrefix,   // app/exports/dev/
                typePrefix,   // chart
                datePath,     // 2025/11/14
                memberId,     // 1, 2, ...
                uuid,
                fileName
        );
        return key.replace("//", "/");
    }

    private Long resolveMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            throw new IllegalStateException("인증 정보가 없습니다.");
        Object principal = auth.getPrincipal();
        if (principal instanceof Long l) return l;
        throw new IllegalStateException("지원하지 않는 principal 타입: " + principal);
    }
}
