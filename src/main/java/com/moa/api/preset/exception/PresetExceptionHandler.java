package com.moa.api.preset.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * Preset 전역 예외 핸들러
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.moa.api.preset")
public class PresetExceptionHandler {

    /**
     * Preset 커스텀 예외 처리
     */
    @ExceptionHandler(PresetException.class)
    public ResponseEntity<ErrorResponse> handlePresetException(PresetException ex) {
        log.error("PresetException: {} - {}", ex.getErrorCode(), ex.getMessage(), ex);

        HttpStatus status = switch (ex.getErrorCode()) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN, PRESET_NOT_FOUND -> HttpStatus.FORBIDDEN;
            case INVALID_REQUEST, INVALID_PRESET_NAME, INVALID_PRESET_TYPE, INVALID_CONFIG ->
                    HttpStatus.BAD_REQUEST;
            case PRESET_CREATION_FAILED, PRESET_UPDATE_FAILED, PRESET_DELETE_FAILED, DATABASE_ERROR ->
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
     * IllegalStateException 처리
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        log.error("IllegalStateException: {}", ex.getMessage(), ex);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("INTERNAL_SERVER_ERROR")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.internalServerError().body(response);
    }

    /**
     * 기타 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("Unexpected error occurred in Preset API", ex);

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