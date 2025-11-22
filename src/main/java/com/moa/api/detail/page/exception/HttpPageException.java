package com.moa.api.detail.page.exception;

/**
 * HTTP Page 커스텀 예외
 */
public class HttpPageException extends RuntimeException {

    private final ErrorCode errorCode;

    public HttpPageException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public HttpPageException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public HttpPageException(ErrorCode errorCode, String additionalMessage) {
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
        ROW_KEY_NOT_FOUND("HTTP Page를 찾을 수 없습니다"),
        INVALID_ROW_KEY("유효하지 않은 Row Key입니다"),
        DATABASE_ERROR("데이터베이스 조회 중 오류가 발생했습니다");

        private final String message;

        ErrorCode(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}