package com.moa.api.preset.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * Preset 전역 예외 핸들러
 * ---------------------------------------------------------
 * - com.moa.api.preset 패키지 내에서 발생하는 모든 예외를 처리한다.
 * - PresetException의 ErrorCode에 따라 적절한 HTTP Status를 매핑한다.
 * - IllegalArgumentException, IllegalStateException 등 일반적인 예외도 별도 처리하여
 *   클라이언트에게 일관된 에러 응답을 제공한다.
 *   AUTHOR        : 방대혁
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.moa.api.preset")
public class PresetExceptionHandler {

    /**
     * Preset 커스텀 예외 처리
     * ---------------------------------------------------------
     * PresetException(ErrorCode 포함)을 캐치하여
     * 상황에 맞는 HTTP 상태 코드로 변환해준다.
     *
     * 매핑 규칙:
     *   - 인증 오류 → 401
     *   - 권한/존재하지 않음 → 403
     *   - 잘못된 요청 → 400
     *   - 생성/수정/삭제 실패, DB 오류 → 500
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
     * ---------------------------------------------------------
     * 컨트롤러 또는 서비스에서 잘못된 파라미터로 발생하는 일반적인 예외.
     * 클라이언트 요청 문제이므로 400 BAD_REQUEST로 반환한다.
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
     * ---------------------------------------------------------
     * 애플리케이션 상태가 잘못되었을 때 발생.
     * 보통 로직 오류나 불가능한 상태이므로 500 또는 403 중 선택 가능한데,
     * 여기서는 안정성을 위해 500으로 반환한다.
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
     * ---------------------------------------------------------
     * 예상하지 못한 런타임 오류를 위한 fallback 핸들러.
     * 상세 정보는 서버 로그로만 남기고, 클라이언트에는 간단한 메시지만 제공한다.
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
     * ---------------------------------------------------------
     * 모든 에러 응답은 동일한 구조로 내려가며,
     * timestamp / status / error / message 필드를 포함한다.
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
