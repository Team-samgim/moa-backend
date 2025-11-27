package com.moa.api.notification.exception;

/**
 * Notification 커스텀 예외
 * -----------------------------------------------------------
 * 알림(Notification) 기능에서 발생할 수 있는 예외를 처리하기 위한
 * 런타임 예외 클래스이다.
 *
 * - ErrorCode(내부 enum)를 통해 일관된 에러 메시지를 제공한다.
 * - 생성자를 여러 개 두어 예외 원인(cause) 혹은 추가 메시지를 포함할 수 있다.
 * - ControllerAdvice에서 잡아서 공통 에러 응답 형태로 변환하여 사용 가능하다.
 * -----------------------------------------------------------
 * AUTHOR        : 방대혁
 */
public class NotificationException extends RuntimeException {

    /** 발생한 에러 유형 */
    private final ErrorCode errorCode;

    /**
     * 기본 생성자 (단순 에러코드 기반 예외)
     *
     * @param errorCode 발생한 에러 코드
     */
    public NotificationException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 원인(cause) 포함한 예외
     *
     * @param errorCode 에러 코드
     * @param cause 실제 예외 원인
     */
    public NotificationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    /**
     * 에러코드의 기본 메시지 + 추가적인 설명 포함
     *
     * @param errorCode 에러 코드
     * @param additionalMessage 상세 메시지
     */
    public NotificationException(ErrorCode errorCode, String additionalMessage) {
        super(errorCode.getMessage() + ": " + additionalMessage);
        this.errorCode = errorCode;
    }

    /** 에러 코드 조회 */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * ===========================================================
     * ErrorCode
     * -----------------------------------------------------------
     * 알림 관련 기능에서 사용할 에러 코드 정의.
     * 각 항목은 사용자에게 전달될 메시지를 포함한다.
     * ===========================================================
     */
    public enum ErrorCode {

        // ────────────── 인증/권한 ──────────────
        UNAUTHORIZED("인증 정보가 없습니다"),
        FORBIDDEN("접근 권한이 없습니다"),

        // ────────────── 요청 검증 ──────────────
        INVALID_REQUEST("유효하지 않은 요청입니다"),
        INVALID_TYPE("유효하지 않은 알림 타입입니다"),
        INVALID_TITLE("유효하지 않은 제목입니다"),
        INVALID_CONTENT("유효하지 않은 내용입니다"),
        INVALID_CONFIG("유효하지 않은 설정입니다"),

        // ────────────── 알림 도메인 오류 ──────────────
        NOTIFICATION_NOT_FOUND("알림을 찾을 수 없습니다"),
        NOTIFICATION_CREATION_FAILED("알림 생성에 실패했습니다"),
        NOTIFICATION_UPDATE_FAILED("알림 수정에 실패했습니다"),

        // ────────────── JSON 처리 ──────────────
        JSON_PARSE_ERROR("JSON 파싱에 실패했습니다"),
        JSON_SERIALIZE_ERROR("JSON 직렬화에 실패했습니다"),

        // ────────────── 데이터베이스 오류 ──────────────
        DATABASE_ERROR("데이터베이스 오류가 발생했습니다");

        /** 사용자에게 반환할 메시지 */
        private final String message;

        ErrorCode(String message) {
            this.message = message;
        }

        /** 메시지 조회 */
        public String getMessage() {
            return message;
        }
    }
}
