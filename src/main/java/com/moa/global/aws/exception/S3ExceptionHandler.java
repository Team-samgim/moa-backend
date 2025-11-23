package com.moa.global.aws.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * S3 전역 예외 핸들러
 *
 * 주의: 이 핸들러는 선택사항입니다.
 * 각 도메인(Export 등)의 ExceptionHandler에서 S3Exception을 처리해도 됩니다.
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE) // 다른 핸들러보다 우선 처리
public class S3ExceptionHandler {

    /**
     * S3 예외 처리
     */
    @ExceptionHandler(S3Exception.class)
    public ResponseEntity<ErrorResponse> handleS3Exception(S3Exception ex) {
        log.error("S3Exception: {} - {}", ex.getErrorCode(), ex.getMessage(), ex);

        HttpStatus status = switch (ex.getErrorCode()) {
            case INVALID_BUCKET, INVALID_KEY, INVALID_FILE, FILE_TOO_LARGE ->
                    HttpStatus.BAD_REQUEST;
            case UPLOAD_FAILED, DELETE_FAILED, DOWNLOAD_FAILED, PRESIGN_FAILED, S3_CLIENT_ERROR ->
                    HttpStatus.INTERNAL_SERVER_ERROR;
        };

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(ex.getErrorCode().name())
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(status).body(response);
    }

    /**
     * 에러 응답 DTO
     */
    @lombok.Builder
    @lombok.Getter
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
    }
}