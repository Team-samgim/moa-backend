package com.moa.api.data.service;

import com.moa.api.data.exception.ArchiveException;
import com.moa.global.aws.S3Props;
import com.moa.global.aws.S3Uploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/*****************************************************************************
 CLASS NAME    : AbstractSampleArchiveService
 DESCRIPTION   : 샘플 데이터 아카이브 공통 로직을 담당하는 추상 서비스
 - CSV Export → S3 업로드 → 삭제를 템플릿 메서드 패턴으로 공통화
 - 서브 클래스는 테이블명만 구현하여 재사용
 AUTHOR        : 방대혁
 ******************************************************************************/
/**
 * Sample Archive Service 추상 클래스
 *
 * 템플릿 메서드 패턴을 사용하여 중복 코드 제거
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractSampleArchiveService {

    protected static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    protected static final String TS_COLUMN = "ts_server";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH-mm-ss");

    protected final JdbcTemplate jdbcTemplate;
    protected final S3Uploader s3Uploader;
    protected final S3Props s3Props;

    /**
     * 테이블 이름 반환 (서브클래스에서 구현)
     */
    protected abstract String getTableName();

    /**
     * 7일 이전 데이터 아카이브
     */
    public void archiveOlderThan7Days() {
        String tableName = getTableName();
        LocalDateTime cutoff = LocalDateTime.now(ZONE_ID).minusDays(7);

        log.info("[{}] Starting archive: cutoff={}", tableName, cutoff);

        try {
            // 1) CSV Export
            ExportResult result = exportToCsv(cutoff);

            if (result.rowCount() == 0) {
                log.info("[{}] No data to archive (cutoff={})", tableName, cutoff);
                safeDeleteTempFile(result.file());
                return;
            }

            // 2) S3 Upload
            String s3Key = uploadToS3(result.file(), cutoff);

            // 3) Delete Old Rows
            int deleted = deleteOldRows(cutoff);

            // 4) Cleanup
            safeDeleteTempFile(result.file());

            log.info("[{}] Archive completed: cutoff={}, exported={}, deleted={}, s3Key={}",
                    tableName, cutoff, result.rowCount(), deleted, s3Key);

        } catch (ArchiveException e) {
            throw e;
        } catch (Exception e) {
            log.error("[{}] Archive failed: cutoff={}", tableName, cutoff, e);
            throw new ArchiveException(
                    ArchiveException.ErrorCode.ARCHIVE_FAILED,
                    e
            );
        }
    }

    /**
     * CSV Export
     */
    private ExportResult exportToCsv(LocalDateTime cutoff) {
        String tableName = getTableName();

        try {
            Path tempFile = createTempFile();

            AtomicBoolean headerWritten = new AtomicBoolean(false);
            AtomicInteger rowCount = new AtomicInteger(0);

            try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8);
                 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

                String sql = buildSelectSql();

                jdbcTemplate.query(con -> {
                    var ps = con.prepareStatement(sql);
                    ps.setTimestamp(1, Timestamp.valueOf(cutoff));
                    return ps;
                }, rs -> {
                    try {
                        ResultSetMetaData meta = rs.getMetaData();
                        int colCount = meta.getColumnCount();

                        // 헤더 작성 (첫 row에서만)
                        if (!headerWritten.get()) {
                            List<String> headers = new ArrayList<>(colCount);
                            for (int i = 1; i <= colCount; i++) {
                                headers.add(meta.getColumnLabel(i));
                            }
                            csvPrinter.printRecord(headers);
                            headerWritten.set(true);
                        }

                        // 데이터 작성
                        List<Object> values = new ArrayList<>(colCount);
                        for (int i = 1; i <= colCount; i++) {
                            values.add(rs.getObject(i));
                        }
                        csvPrinter.printRecord(values);
                        rowCount.incrementAndGet();

                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });

            } catch (UncheckedIOException e) {
                throw e.getCause();
            }

            log.info("[{}] CSV export completed: file={}, rows={}",
                    tableName, tempFile, rowCount.get());

            return new ExportResult(tempFile, rowCount.get());

        } catch (IOException e) {
            log.error("[{}] CSV export failed", tableName, e);
            throw new ArchiveException(
                    ArchiveException.ErrorCode.CSV_EXPORT_FAILED,
                    e
            );
        }
    }

    /**
     * S3 Upload
     */
    private String uploadToS3(Path file, LocalDateTime cutoff) {
        String tableName = getTableName();

        try {
            String s3Key = buildS3Key(cutoff);
            String bucket = s3Props.getS3().getBucket();
            String prefix = s3Props.getS3().getNormalizedPrefix();

            String fullKey = buildFullS3Key(prefix, s3Key);

            s3Uploader.upload(bucket, fullKey, file, "text/csv; charset=utf-8");

            String s3Url = "s3://" + bucket + "/" + fullKey;

            log.info("[{}] S3 upload completed: {}", tableName, s3Url);

            return s3Url;

        } catch (Exception e) {
            log.error("[{}] S3 upload failed", tableName, e);
            throw new ArchiveException(
                    ArchiveException.ErrorCode.S3_UPLOAD_FAILED,
                    e
            );
        }
    }

    /**
     * Delete Old Rows
     */
    private int deleteOldRows(LocalDateTime cutoff) {
        String tableName = getTableName();

        try {
            String deleteSql = buildDeleteSql();
            int deleted = jdbcTemplate.update(deleteSql, Timestamp.valueOf(cutoff));

            log.info("[{}] Delete completed: cutoff={}, deleted={}",
                    tableName, cutoff, deleted);

            return deleted;

        } catch (Exception e) {
            log.error("[{}] Delete failed: cutoff={}", tableName, cutoff, e);
            throw new ArchiveException(
                    ArchiveException.ErrorCode.DELETE_FAILED,
                    e
            );
        }
    }

    /**
     * 임시 파일 생성
     */
    private Path createTempFile() {
        String tableName = getTableName();

        try {
            return Files.createTempFile(tableName + "_", ".csv");
        } catch (IOException e) {
            log.error("[{}] Temp file creation failed", tableName, e);
            throw new ArchiveException(
                    ArchiveException.ErrorCode.TEMP_FILE_CREATION_FAILED,
                    e
            );
        }
    }

    /**
     * SELECT SQL 생성
     */
    private String buildSelectSql() {
        return String.format(
                "SELECT * FROM %s WHERE %s < ?",
                getTableName(),
                TS_COLUMN
        );
    }

    /**
     * DELETE SQL 생성
     */
    private String buildDeleteSql() {
        return String.format(
                "DELETE FROM %s WHERE %s < ?",
                getTableName(),
                TS_COLUMN
        );
    }

    /**
     * S3 객체 키 생성
     */
    private String buildS3Key(LocalDateTime cutoff) {
        String tableName = getTableName();
        String datePart = cutoff.format(DATE_FORMATTER);
        String timePart = cutoff.format(TIME_FORMATTER);
        int monthValue = cutoff.getMonthValue();

        return String.format(
                "%s/%d/%s_before_%s_%s.csv",
                tableName,
                monthValue,
                tableName,
                datePart,
                timePart
        );
    }

    /**
     * 전체 S3 키 생성 (prefix 포함)
     */
    private String buildFullS3Key(String prefix, String key) {
        if (prefix == null || prefix.isBlank()) {
            return key;
        }

        // Prefix는 이미 정규화되어 있음 (S3Props.getNormalizedPrefix())
        return prefix + key;
    }

    /**
     * 임시 파일 안전하게 삭제
     */
    private void safeDeleteTempFile(Path file) {
        if (file == null) {
            return;
        }

        try {
            Files.deleteIfExists(file);
            log.debug("Temp file deleted: {}", file);
        } catch (IOException e) {
            log.warn("Failed to delete temp file: {}", file, e);
        }
    }

    /**
     * Export 결과
     */
    protected record ExportResult(Path file, int rowCount) {}
}
