package com.moa.api.data.service;

import com.moa.global.aws.S3Props;
import com.moa.global.aws.S3Uploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

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

@Service
@RequiredArgsConstructor
@Slf4j
public class HttpUriSampleArchiveService {

    private static final String TABLE_NAME = "http_uri_sample";
    private static final String TS_COLUMN = "ts_server";
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    private final JdbcTemplate jdbcTemplate;
    private final S3Uploader s3Uploader;
    private final S3Props s3Props;

    public void archiveOlderThan7Days() {
        LocalDateTime cutoff = LocalDateTime.now(ZONE_ID).minusDays(7);
        log.info("[{}] cutoff = {}", TABLE_NAME, cutoff);

        ExportResult result = exportToCsv(cutoff);

        if (result.rowCount() == 0) {
            log.info("[{}] cutoff 이전 데이터 없음. 아카이브 스킵.", TABLE_NAME);
            safeDeleteTempFile(result.file());
            return;
        }

        String s3Key = buildS3Key(cutoff);
        String bucket = s3Props.getS3().getBucket();
        String prefix = s3Props.getS3().getPrefix(); // 비어있을 수도 있음

        String fullKey = buildFullS3Key(prefix, s3Key);

        // ⬇️ 이미 만들어 둔 S3Uploader 재사용
        s3Uploader.upload(bucket, fullKey, result.file(), "text/csv");

        int deleted = deleteOldRows(cutoff);

        log.info("[{}] 아카이브 완료: cutoff={}, rows={}, deleted={}, s3Key=s3://{}/{}",
                TABLE_NAME, cutoff, result.rowCount(), deleted, bucket, fullKey);

        safeDeleteTempFile(result.file());
    }

    // ================== 내부 구현 ==================

    private record ExportResult(Path file, int rowCount) {}

    /**
     * ts_server < cutoff 인 모든 행을 CSV로 export
     */
    private ExportResult exportToCsv(LocalDateTime cutoff) {
        try {
            Path tempFile = Files.createTempFile("http_uri_sample_", ".csv");

            AtomicBoolean headerWritten = new AtomicBoolean(false);
            AtomicInteger rowCount = new AtomicInteger(0);

            try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8);
                 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

                String sql = "SELECT * FROM " + TABLE_NAME + " WHERE " + TS_COLUMN + " < ?";

                jdbcTemplate.query(con -> {
                    var ps = con.prepareStatement(sql);
                    ps.setTimestamp(1, Timestamp.valueOf(cutoff));
                    return ps;
                }, rs -> {
                    try {
                        ResultSetMetaData meta = rs.getMetaData();
                        int colCount = meta.getColumnCount();

                        // 첫 row에서 헤더 한 번만 출력
                        if (!headerWritten.get()) {
                            List<String> headers = new ArrayList<>(colCount);
                            for (int i = 1; i <= colCount; i++) {
                                headers.add(meta.getColumnLabel(i));
                            }
                            csvPrinter.printRecord(headers);
                            headerWritten.set(true);
                        }

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

            log.info("[{}] export 완료: file={}, rows={}",
                    TABLE_NAME, tempFile, rowCount.get());

            return new ExportResult(tempFile, rowCount.get());
        } catch (IOException e) {
            throw new RuntimeException(TABLE_NAME + " CSV export 실패", e);
        }
    }

    /**
     * 날짜 기준으로 S3 object key 생성
     * 예) {prefix}/http_uri_sample/2025/11/http_uri_sample_before_2025-11-17_03-00-00.csv
     */
    private String buildS3Key(LocalDateTime cutoff) {
        String datePart = cutoff.toLocalDate().toString(); // 2025-11-17
        String timePart = cutoff.format(DateTimeFormatter.ofPattern("HH-mm-ss"));

        return String.format("http_uri_sample/%d/http_uri_sample_before_%s_%s.csv",
                cutoff.getMonthValue(), datePart, timePart);
    }

    private String buildFullS3Key(String prefix, String key) {
        if (prefix == null || prefix.isBlank()) {
            return key;
        }
        String p = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
        return p + "/" + key;
    }

    private int deleteOldRows(LocalDateTime cutoff) {
        String deleteSql = "DELETE FROM " + TABLE_NAME + " WHERE " + TS_COLUMN + " < ?";
        int deleted = jdbcTemplate.update(deleteSql, Timestamp.valueOf(cutoff));
        log.info("[{}] 삭제 완료: cutoff={}, deleted={}", TABLE_NAME, cutoff, deleted);
        return deleted;
    }

    private void safeDeleteTempFile(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("임시 CSV 파일 삭제 실패: {}", file, e);
        }
    }
}