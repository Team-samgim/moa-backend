package com.moa.api.detail.page.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/*****************************************************************************
 CLASS NAME    : HttpPageExceptionHandler
 DESCRIPTION   : HTTP Page 전역 예외 핸들러
 - HttpPageException 및 기타 예외를 공통 포맷으로 응답
 AUTHOR        : 방대혁
 ******************************************************************************/
/**
 * HTTP Page 전역 예외 핸들러
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.moa.api.detail.page")
public class HttpPageExceptionHandler {

    /**
     * HttpPage 커스텀 예외 처리
     */
    @ExceptionHandler(HttpPageException.class)
    public ResponseEntity<ErrorResponse> handleHttpPageException(HttpPageException ex) {
        log.error("HttpPageException: {} - {}", ex.getErrorCode(), ex.getMessage(), ex);

        HttpStatus status = switch (ex.getErrorCode()) {
            case ROW_KEY_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_ROW_KEY -> HttpStatus.BAD_REQUEST;
            case DATABASE_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
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
     * 기타 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("Unexpected error occurred in HTTP Page API", ex);

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
