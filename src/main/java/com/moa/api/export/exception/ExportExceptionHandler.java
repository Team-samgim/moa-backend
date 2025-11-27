package com.moa.api.export.exception;

/*****************************************************************************
 CLASS NAME    : ExportExceptionHandler
 DESCRIPTION   : Export API 전역 예외 처리 클래스
 - ExportException, NoSuchElementException, IllegalArgumentException,
 기타 예외를 공통 포맷(ErrorResponse)으로 변환
 AUTHOR        : 방대혁
 ******************************************************************************/

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

/**
 * Export 전역 예외 핸들러
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.moa.api.export")
public class ExportExceptionHandler {

    /**
     * Export 커스텀 예외 처리
     */
    @ExceptionHandler(ExportException.class)
    public ResponseEntity<ErrorResponse> handleExportException(ExportException ex) {
        log.error("ExportException: {} - {}", ex.getErrorCode(), ex.getMessage(), ex);

        HttpStatus status = switch (ex.getErrorCode()) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN, FILE_NOT_FOUND, PRESET_NOT_FOUND -> HttpStatus.FORBIDDEN;
            case INVALID_REQUEST, INVALID_LAYER, INVALID_COLUMNS, INVALID_FILE_NAME ->
                    HttpStatus.BAD_REQUEST;
            case S3_UPLOAD_FAILED, S3_DELETE_FAILED, S3_DOWNLOAD_FAILED,
                 FILE_CREATION_FAILED, DATABASE_ERROR, PRESET_CREATION_FAILED ->
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
     * NoSuchElementException 처리
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElementException(NoSuchElementException ex) {
        log.error("NoSuchElementException: {}", ex.getMessage(), ex);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("NOT_FOUND")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("IllegalArgumentException: {}", ex.getMessage(), ex);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("BAD_REQUEST")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 기타 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("Unexpected error occurred in Export API", ex);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("INTERNAL_SERVER_ERROR")
                .message("서버 내부 오류가 발생했습니다")
                .build();

        return ResponseEntity.internalServerError().body(response);
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
