package com.moa.api.export.exception;

/**
 * Export 커스텀 예외
 */
public class ExportException extends RuntimeException {

    private final ErrorCode errorCode;

    public ExportException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ExportException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public ExportException(ErrorCode errorCode, String additionalMessage) {
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
        INVALID_LAYER("유효하지 않은 레이어입니다"),
        INVALID_COLUMNS("유효한 컬럼이 없습니다"),
        INVALID_FILE_NAME("유효하지 않은 파일명입니다"),

        // 파일 관련
        FILE_NOT_FOUND("파일을 찾을 수 없습니다"),
        FILE_CREATION_FAILED("파일 생성에 실패했습니다"),

        // S3 관련
        S3_UPLOAD_FAILED("S3 업로드에 실패했습니다"),
        S3_DELETE_FAILED("S3 삭제에 실패했습니다"),
        S3_DOWNLOAD_FAILED("S3 다운로드에 실패했습니다"),

        // 데이터베이스
        DATABASE_ERROR("데이터베이스 오류가 발생했습니다"),

        // 프리셋
        PRESET_NOT_FOUND("프리셋을 찾을 수 없습니다"),
        PRESET_CREATION_FAILED("프리셋 생성에 실패했습니다");

        private final String message;

        ErrorCode(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}