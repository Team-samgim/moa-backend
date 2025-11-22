package com.moa.global.aws.exception;

/**
 * S3 커스텀 예외
 */
public class S3Exception extends RuntimeException {

    private final ErrorCode errorCode;

    public S3Exception(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public S3Exception(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public S3Exception(ErrorCode errorCode, String additionalMessage) {
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
        // 업로드
        UPLOAD_FAILED("S3 업로드에 실패했습니다"),
        INVALID_FILE("유효하지 않은 파일입니다"),
        FILE_TOO_LARGE("파일 크기가 너무 큽니다"),

        // 다운로드
        DOWNLOAD_FAILED("S3 다운로드에 실패했습니다"),
        PRESIGN_FAILED("Presigned URL 생성에 실패했습니다"),

        // 삭제
        DELETE_FAILED("S3 삭제에 실패했습니다"),

        // 일반
        INVALID_BUCKET("유효하지 않은 버킷입니다"),
        INVALID_KEY("유효하지 않은 객체 키입니다"),
        S3_CLIENT_ERROR("S3 클라이언트 오류가 발생했습니다");

        private final String message;

        ErrorCode(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}