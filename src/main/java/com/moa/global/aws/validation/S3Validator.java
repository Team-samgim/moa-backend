package com.moa.global.aws.validation;

import com.moa.global.aws.exception.S3Exception;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * S3 입력값 검증기
 * ------------------------------------------------------------
 * - AWS S3 작업(upload/download/delete/presign 등) 수행 전 입력값 검증.
 * - 버킷 이름 / 객체 키 / 파일 경로 / Content-Type 등 사전 유효성 체크.
 * - AWS 규격에 맞도록 버킷/키 형식 제한.
 * - 보안 위험 요소(경로 순회, Null 파일 등) 방지.
 *
 * AUTHOR        : 방대혁
 */
@Slf4j
@Component
public class S3Validator {

    // S3 객체 키 규칙: 1~1024자
    private static final int MAX_KEY_LENGTH = 1024;

    // 버킷 이름 규칙: 3~63자, 소문자/숫자/하이픈
    private static final Pattern BUCKET_PATTERN =
            Pattern.compile("^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$");

    // 파일 크기 제한 (기본 5GB)
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024 * 1024;

    /**
     * 버킷 이름 검증
     */
    public void validateBucket(String bucket) {
        if (bucket == null || bucket.isBlank()) {
            throw new S3Exception(
                    S3Exception.ErrorCode.INVALID_BUCKET,
                    "버킷 이름이 비어있습니다"
            );
        }

        String trimmed = bucket.trim();

        if (!BUCKET_PATTERN.matcher(trimmed).matches()) {
            throw new S3Exception(
                    S3Exception.ErrorCode.INVALID_BUCKET,
                    "유효하지 않은 버킷 이름 형식: " + trimmed
            );
        }
    }

    /**
     * 객체 키 검증
     */
    public void validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw new S3Exception(
                    S3Exception.ErrorCode.INVALID_KEY,
                    "객체 키가 비어있습니다"
            );
        }

        String trimmed = key.trim();

        if (trimmed.length() > MAX_KEY_LENGTH) {
            throw new S3Exception(
                    S3Exception.ErrorCode.INVALID_KEY,
                    "객체 키가 너무 깁니다 (최대 " + MAX_KEY_LENGTH + "자)"
            );
        }

        // 위험한 경로 순회 패턴 방지
        if (trimmed.contains("../") || trimmed.contains("..\\")) {
            throw new S3Exception(
                    S3Exception.ErrorCode.INVALID_KEY,
                    "객체 키에 경로 순회 패턴이 포함되어 있습니다"
            );
        }
    }

    /**
     * 파일 검증
     */
    public void validateFile(Path file) {
        if (file == null) {
            throw new S3Exception(
                    S3Exception.ErrorCode.INVALID_FILE,
                    "파일이 null입니다"
            );
        }

        if (!Files.exists(file)) {
            throw new S3Exception(
                    S3Exception.ErrorCode.INVALID_FILE,
                    "파일이 존재하지 않습니다: " + file
            );
        }

        if (!Files.isRegularFile(file)) {
            throw new S3Exception(
                    S3Exception.ErrorCode.INVALID_FILE,
                    "일반 파일이 아닙니다: " + file
            );
        }

        if (!Files.isReadable(file)) {
            throw new S3Exception(
                    S3Exception.ErrorCode.INVALID_FILE,
                    "파일을 읽을 수 없습니다: " + file
            );
        }

        try {
            long size = Files.size(file);

            if (size > MAX_FILE_SIZE) {
                throw new S3Exception(
                        S3Exception.ErrorCode.FILE_TOO_LARGE,
                        String.format("파일 크기가 너무 큽니다: %d bytes (최대 %d bytes)",
                                size, MAX_FILE_SIZE)
                );
            }

            if (size == 0) {
                log.warn("Empty file being uploaded: {}", file);
            }

        } catch (Exception e) {
            if (e instanceof S3Exception ex) {
                throw ex;
            }
            throw new S3Exception(
                    S3Exception.ErrorCode.INVALID_FILE,
                    "파일 크기를 확인할 수 없습니다: " + e.getMessage()
            );
        }
    }

    /**
     * Content-Type 검증
     */
    public void validateContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            log.warn("Content-Type is null or blank, will use default");
            return;
        }

        // 최소한의 MIME 타입 형식 체크
        if (!contentType.contains("/")) {
            throw new S3Exception(
                    S3Exception.ErrorCode.INVALID_FILE,
                    "유효하지 않은 Content-Type 형식: " + contentType
            );
        }
    }
}
