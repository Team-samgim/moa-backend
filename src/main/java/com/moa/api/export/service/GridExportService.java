package com.moa.api.export.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moa.api.export.config.ExportProperties;
import com.moa.api.export.dto.ExportCreateResponseDTO;
import com.moa.api.export.dto.ExportGridRequestDTO;
import com.moa.api.export.entity.ExportFile;
import com.moa.api.export.exception.ExportException;
import com.moa.api.export.repository.ExportFileRepository;
import com.moa.api.export.validation.ExportValidator;
import com.moa.api.grid.dto.SqlDTO;
import com.moa.api.grid.repository.GridRepositoryImpl;
import com.moa.api.grid.util.QueryBuilder;
import com.moa.api.member.entity.Member;
import com.moa.api.preset.entity.Preset;
import com.moa.api.preset.entity.PresetOrigin;
import com.moa.api.preset.entity.PresetType;
import com.moa.global.aws.S3Props;
import com.moa.global.aws.S3Uploader;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Grid Export Service
 *
 * 개선사항:
 * - ExportValidator 추가
 * - ExportException 사용
 * - ExportProperties 설정 사용
 * - 로깅 강화
 * - 트랜잭션 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GridExportService {

    private final JdbcTemplate jdbcTemplate;
    private final QueryBuilder queryBuilder;
    private final GridRepositoryImpl gridRepository;
    private final ExportFileRepository exportFileRepository;
    private final S3Uploader s3Uploader;
    private final S3Props s3Props;
    private final EntityManager em;
    private final ExportValidator validator;
    private final ExportProperties exportProperties;
    private final ObjectMapper objectMapper;

    /**
     * CSV Export
     */
    @Transactional
    public ExportCreateResponseDTO exportCsv(ExportGridRequestDTO req) {
        log.info("Starting CSV export: layer={}, columns={}", req.getLayer(), req.getColumns());

        try {
            // 1) 검증
            validator.validateGridExportRequest(req);

            // 2) 인증 정보 확인
            Long memberId = resolveMemberId();
            Member memberRef = em.getReference(Member.class, memberId);

            // 3) 프리셋 확보
            Preset presetRef = resolvePreset(req, memberRef);

            // 4) 파일명 & S3 경로 생성
            String safeFileName = generateSafeFileName(req.getFileName());
            String objectKey = buildObjectKey(memberId, safeFileName);

            // 5) CSV 생성 & S3 업로드
            uploadCsvToS3(req, objectKey);

            // 6) 프리사인드 URL 생성
            String bucket = s3Props.getS3().getBucket();
            String httpUrl = s3Uploader.presign(
                    bucket,
                    objectKey,
                    Duration.ofMinutes(exportProperties.getPresign().getExpirationMinutes())
            );

            // 7) DB 저장
            ExportFile exportFile = saveExportFile(memberRef, presetRef, bucket, objectKey, safeFileName);

            log.info("CSV export completed successfully: exportId={}, objectKey={}",
                    exportFile.getExportId(), objectKey);

            return ExportCreateResponseDTO.builder()
                    .exportId(exportFile.getExportId())
                    .bucket(bucket)
                    .objectKey(objectKey)
                    .s3Url("s3://" + bucket + "/" + objectKey)
                    .httpUrl(httpUrl)
                    .status(exportFile.getExportStatus())
                    .build();

        } catch (ExportException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during CSV export", e);
            throw new ExportException(
                    ExportException.ErrorCode.FILE_CREATION_FAILED,
                    e
            );
        }
    }

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
     * 프리셋 확보 (기존 프리셋 사용 또는 신규 생성)
     */
    private Preset resolvePreset(ExportGridRequestDTO req, Member member) {
        if (req.getPresetId() != null) {
            // 기존 프리셋 사용
            return em.getReference(Preset.class, req.getPresetId());
        }

        // 신규 프리셋 생성
        return createPresetFromRequest(req, member);
    }

    /**
     * 요청으로부터 프리셋 생성
     */
    private Preset createPresetFromRequest(ExportGridRequestDTO req, Member member) {
        try {
            String presetName = generatePresetName(req.getFileName());

            Map<String, Object> config = buildPresetConfig(req);

            Preset preset = Preset.builder()
                    .member(member)
                    .presetName(presetName)
                    .presetType(PresetType.SEARCH)
                    .config(objectMapper.valueToTree(config))
                    .favorite(false)
                    .origin(PresetOrigin.EXPORT)
                    .createdAt(LocalDateTime.now())
                    .build();

            em.persist(preset);
            em.flush(); // ID 즉시 채움

            log.debug("Created new preset: presetId={}, name={}", preset.getPresetId(), presetName);

            return preset;

        } catch (Exception e) {
            log.error("Failed to create preset", e);
            throw new ExportException(
                    ExportException.ErrorCode.PRESET_CREATION_FAILED,
                    e
            );
        }
    }

    /**
     * 프리셋 설정 빌드
     */
    private Map<String, Object> buildPresetConfig(ExportGridRequestDTO req) {
        Map<String, Object> sort = new LinkedHashMap<>();
        if (req.getSortField() != null && !req.getSortField().isBlank()) {
            sort.put("field", req.getSortField());
        }
        if (req.getSortDirection() != null && !req.getSortDirection().isBlank()) {
            sort.put("direction", req.getSortDirection());
        }

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("layer", req.getLayer());
        config.put("columns", req.getColumns());
        config.put("filters", parseJsonOrEmpty(req.getFilterModelJson()));
        config.put("baseSpec", parseJsonOrEmpty(req.getBaseSpecJson()));
        config.put("sort", sort);
        config.put("version", 1);

        return config;
    }

    /**
     * JSON 문자열 파싱 (실패 시 빈 Map)
     */
    private Object parseJsonOrEmpty(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }

        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.warn("Failed to parse JSON, using empty map: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 프리셋 이름 생성
     */
    private String generatePresetName(String fileName) {
        if (fileName != null && !fileName.isBlank()) {
            return fileName.replace(".csv", "").trim();
        }

        return "Grid Export " + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 안전한 파일명 생성
     */
    private String generateSafeFileName(String requestedFileName) {
        if (requestedFileName != null && !requestedFileName.isBlank()) {
            return sanitizeFileName(requestedFileName);
        }

        return "grid_" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
    }

    /**
     * 파일명 정제
     */
    private String sanitizeFileName(String fileName) {
        String cleaned = fileName;

        // 위험한 문자 제거
        cleaned = cleaned.replace('\\', ' ');
        cleaned = cleaned.replace('/', ' ');
        cleaned = cleaned.replace('\r', ' ');
        cleaned = cleaned.replace('\n', ' ');
        cleaned = cleaned.replace('\t', ' ');
        cleaned = cleaned.replace('\u0000', ' ');

        // 공백 정규화
        cleaned = cleaned.trim().replaceAll("\\s+", " ");

        // 길이 제한
        int maxLength = exportProperties.getCsv().getMaxFileNameLength();
        if (cleaned.length() > maxLength) {
            cleaned = cleaned.substring(0, maxLength);
        }

        // .csv 확장자 추가
        if (!cleaned.toLowerCase().endsWith(".csv")) {
            cleaned += ".csv";
        }

        return cleaned.isBlank() ? "grid.csv" : cleaned;
    }

    /**
     * S3 Object Key 생성
     */
    private String buildObjectKey(Long memberId, String fileName) {
        String rootPrefix = normalizePrefix(s3Props.getS3().getPrefix());
        LocalDateTime now = LocalDateTime.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString();

        String key = String.format(
                "%sgrid/%s/%d/%s/%s",
                rootPrefix,   // app/exports/dev/
                datePath,     // 2025/11/14
                memberId,     // 1, 2, ...
                uuid,
                fileName
        );

        return key.replace("//", "/");
    }

    /**
     * Prefix 정규화
     */
    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        String trimmed = prefix.trim();
        return trimmed.endsWith("/") ? trimmed : trimmed + "/";
    }

    /**
     * CSV 생성 및 S3 업로드
     */
    private void uploadCsvToS3(ExportGridRequestDTO req, String objectKey) {
        Path tmpFile = null;

        try {
            // 임시 파일 생성
            tmpFile = Files.createTempFile(
                    exportProperties.getS3Upload().getTmpPrefix(),
                    ".csv"
            );

            log.debug("Created temporary CSV file: {}", tmpFile);

            // CSV 작성
            writeCsvToFile(tmpFile, req);

            // S3 업로드
            String bucket = s3Props.getS3().getBucket();
            s3Uploader.upload(
                    bucket,
                    objectKey,
                    tmpFile,
                    exportProperties.getS3Upload().getContentType()
            );

            log.debug("Uploaded CSV to S3: bucket={}, key={}", bucket, objectKey);

        } catch (IOException e) {
            log.error("Failed to create or upload CSV file", e);
            throw new ExportException(
                    ExportException.ErrorCode.FILE_CREATION_FAILED,
                    e
            );
        } catch (Exception e) {
            log.error("Failed to upload to S3", e);
            throw new ExportException(
                    ExportException.ErrorCode.S3_UPLOAD_FAILED,
                    e
            );
        } finally {
            // 임시 파일 삭제
            if (tmpFile != null) {
                try {
                    Files.deleteIfExists(tmpFile);
                    log.debug("Deleted temporary file: {}", tmpFile);
                } catch (IOException e) {
                    log.warn("Failed to delete temporary file: {}", tmpFile, e);
                }
            }
        }
    }

    /**
     * CSV 파일 작성
     */
    private void writeCsvToFile(Path path, ExportGridRequestDTO req) throws Exception {
        String layer = validator.normalizeLayer(req.getLayer());
        List<String> columns = req.getColumns();

        // 타입 정보 조회
        Map<String, String> typeMap = gridRepository.getFrontendTypeMap(layer);
        Map<String, String> temporalMap = gridRepository.getTemporalKindMap(layer);

        // 실존 컬럼 필터링
        List<String> validColumns = columns.stream()
                .filter(col -> typeMap.containsKey(stripColumnSuffix(col)))
                .toList();

        if (validColumns.isEmpty()) {
            throw new ExportException(
                    ExportException.ErrorCode.INVALID_COLUMNS,
                    "내보낼 유효한 컬럼이 없습니다"
            );
        }

        // SQL 생성
        SqlDTO sqlDto = queryBuilder.buildSelectSQLForExport(
                layer,
                validColumns,
                req.getSortField(),
                req.getSortDirection(),
                req.getFilterModelJson(),
                req.getBaseSpecJson(),
                typeMap,
                temporalMap
        );

        // Fetch size 설정
        int fetchSize = Optional.ofNullable(req.getFetchSize())
                .filter(size -> size > 0 && size <= exportProperties.getCsv().getMaxFetchSize())
                .orElse(exportProperties.getCsv().getDefaultFetchSize());

        // CSV 작성
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(
                        Files.newOutputStream(path),
                        StandardCharsets.UTF_8
                )
        );
             CSVPrinter printer = new CSVPrinter(
                     writer,
                     CSVFormat.DEFAULT.withHeader(validColumns.toArray(new String[0]))
             )) {

            RowCallbackHandler rowHandler = rs -> {
                List<Object> row = new ArrayList<>(validColumns.size());

                for (String col : validColumns) {
                    Object value = rs.getObject(stripColumnSuffix(col));

                    // PostgreSQL PGobject 처리
                    if (value instanceof org.postgresql.util.PGobject pgObj) {
                        row.add(Objects.toString(pgObj.getValue(), ""));
                    } else {
                        row.add(Objects.toString(value, ""));
                    }
                }

                try {
                    printer.printRecord(row);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            };

            jdbcTemplate.query(con -> {
                con.setAutoCommit(false);
                var ps = con.prepareStatement(
                        sqlDto.getSql(),
                        java.sql.ResultSet.TYPE_FORWARD_ONLY,
                        java.sql.ResultSet.CONCUR_READ_ONLY
                );
                ps.setFetchSize(fetchSize);

                int idx = 1;
                for (Object arg : sqlDto.getArgs()) {
                    ps.setObject(idx++, arg);
                }

                return ps;
            }, rowHandler);

            log.debug("CSV writing completed: {} rows", validColumns.size());
        }
    }

    /**
     * 컬럼명 접미사 제거
     */
    private String stripColumnSuffix(String column) {
        return column.contains("-")
                ? column.substring(0, column.indexOf('-'))
                : column;
    }

    /**
     * ExportFile 저장
     */
    private ExportFile saveExportFile(
            Member member,
            Preset preset,
            String bucket,
            String objectKey,
            String fileName
    ) {
        try {
            ExportFile exportFile = ExportFile.builder()
                    .member(member)
                    .preset(preset)
                    .exportType("GRID")
                    .fileFormat("CSV")
                    .fileName(fileName)
                    .bucket(bucket)
                    .objectKey(objectKey)
                    .exportStatus("SUCCESS")
                    .createdAt(LocalDateTime.now())
                    .build();

            exportFileRepository.save(exportFile);

            log.debug("Saved ExportFile: exportId={}", exportFile.getExportId());

            return exportFile;

        } catch (Exception e) {
            log.error("Failed to save ExportFile", e);
            throw new ExportException(
                    ExportException.ErrorCode.DATABASE_ERROR,
                    e
            );
        }
    }
}