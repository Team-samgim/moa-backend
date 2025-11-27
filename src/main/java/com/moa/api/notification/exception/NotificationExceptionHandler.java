package com.moa.api.notification.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * ===========================================================
 * NotificationExceptionHandler
 * -----------------------------------------------------------
 * Notification API 전용 예외 처리 핸들러.
 *
 * - basePackages="com.moa.api.notification" 로 지정하여
 *   알림 관련 API에서 발생한 예외만 처리한다.
 *
 * - NotificationException (도메인 커스텀 예외)를 HTTP 상태코드와 함께
 *   일관된 JSON 형태로 클라이언트에 응답한다.
 *
 * - 일반적인 IllegalArgumentException, IllegalStateException 및
 *   예기치 못한 Exception 에 대해서도 공통 형태로 응답한다.
 *
 * 구조:
 * 1) NotificationException 전용 핸들러
 * 2) IllegalArgumentException
 * 3) IllegalStateException
 * 4) 모든 기타 예외 처리
 *
 * 반환되는 응답은 ErrorResponse DTO로 통일된다.
 * ===========================================================
 * AUTHOR        : 방대혁
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.moa.api.notification")
public class NotificationExceptionHandler {

    /**
     * =====================================================
     * NotificationException 처리
     * -----------------------------------------------------
     * Notification 비즈니스 로직에서 정의한 ErrorCode에 따라
     * 적절한 HTTP status를 매핑하여 JSON 반환.
     * =====================================================
     */
    @ExceptionHandler(NotificationException.class)
    public ResponseEntity<ErrorResponse> handleNotificationException(NotificationException ex) {
        log.error("NotificationException: {} - {}", ex.getErrorCode(), ex.getMessage(), ex);

        // ErrorCode → HTTP status 매핑
        HttpStatus status = switch (ex.getErrorCode()) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN, NOTIFICATION_NOT_FOUND -> HttpStatus.FORBIDDEN;
            case INVALID_REQUEST, INVALID_TYPE, INVALID_TITLE, INVALID_CONTENT, INVALID_CONFIG ->
                    HttpStatus.BAD_REQUEST;
            case NOTIFICATION_CREATION_FAILED, NOTIFICATION_UPDATE_FAILED, DATABASE_ERROR,
                 JSON_PARSE_ERROR, JSON_SERIALIZE_ERROR ->
                    HttpStatus.INTERNAL_SERVER_ERROR;
        };

        // 공통 ErrorResponse 구성
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(ex.getErrorCode().name())
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(status).body(response);
    }

    /**
     * =====================================================
     * IllegalArgumentException 처리
     * -----------------------------------------------------
     * 잘못된 파라미터/입력값 등이 들어온 경우 발생.
     * HTTP 400(BAD_REQUEST)로 응답.
     * =====================================================
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
     * =====================================================
     * IllegalStateException 처리
     * -----------------------------------------------------
     * 허용되지 않은 상태에서 호출된 경우 주로 발생.
     * HTTP 403(FORBIDDEN)로 응답.
     * =====================================================
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        log.error("IllegalStateException: {}", ex.getMessage(), ex);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("FORBIDDEN")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * =====================================================
     * 기타 모든 전체 예외 처리 (catch-all)
     * -----------------------------------------------------
     * 예상하지 못한 예외가 발생해도 API는 안정적으로
     * 500(INTERNAL_SERVER_ERROR) 형태로 응답한다.
     * =====================================================
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("Unexpected error occurred in Notification API", ex);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("INTERNAL_SERVER_ERROR")
                .message("서버 내부 오류가 발생했습니다")
                .build();

        return ResponseEntity.internalServerError().body(response);
    }

    /**
     * =====================================================
     * ErrorResponse DTO
     * -----------------------------------------------------
     * 모든 에러 응답은 동일한 구조로 내려가도록 정의한 DTO.
     * timestamp   : 오류 발생 시간
     * status      : HTTP 상태 코드
     * error       : 에러 코드명
     * message     : 상세 메시지
     * =====================================================
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
