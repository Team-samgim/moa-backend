package com.moa.api.notification.exception;

/**
 * Notification 커스텀 예외
 */
public class NotificationException extends RuntimeException {

    private final ErrorCode errorCode;

    public NotificationException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public NotificationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public NotificationException(ErrorCode errorCode, String additionalMessage) {
        super(errorCode.getMessage() + ": " + additionalMessage);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * 에러 코드 정의
     */
    public enum ErrorCode {
        // 인증/권한
        UNAUTHORIZED("인증 정보가 없습니다"),
        FORBIDDEN("접근 권한이 없습니다"),

        // 요청 검증
        INVALID_REQUEST("유효하지 않은 요청입니다"),
        INVALID_TYPE("유효하지 않은 알림 타입입니다"),
        INVALID_TITLE("유효하지 않은 제목입니다"),
        INVALID_CONTENT("유효하지 않은 내용입니다"),
        INVALID_CONFIG("유효하지 않은 설정입니다"),

        // 알림 관련
        NOTIFICATION_NOT_FOUND("알림을 찾을 수 없습니다"),
        NOTIFICATION_CREATION_FAILED("알림 생성에 실패했습니다"),
        NOTIFICATION_UPDATE_FAILED("알림 수정에 실패했습니다"),

        // JSON 처리
        JSON_PARSE_ERROR("JSON 파싱에 실패했습니다"),
        JSON_SERIALIZE_ERROR("JSON 직렬화에 실패했습니다"),

        // 데이터베이스
        DATABASE_ERROR("데이터베이스 오류가 발생했습니다");

        private final String message;

        ErrorCode(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}