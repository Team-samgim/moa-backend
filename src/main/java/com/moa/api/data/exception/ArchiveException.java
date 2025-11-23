package com.moa.api.data.exception;

/**
 * Archive 커스텀 예외
 */
public class ArchiveException extends RuntimeException {

    private final ErrorCode errorCode;

    public ArchiveException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ArchiveException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public ArchiveException(ErrorCode errorCode, String additionalMessage) {
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
        // CSV Export
        CSV_EXPORT_FAILED("CSV 내보내기에 실패했습니다"),
        CSV_WRITE_FAILED("CSV 쓰기에 실패했습니다"),

        // S3 Upload
        S3_UPLOAD_FAILED("S3 업로드에 실패했습니다"),

        // Database
        DELETE_FAILED("데이터 삭제에 실패했습니다"),
        QUERY_FAILED("쿼리 실행에 실패했습니다"),

        // 파일
        TEMP_FILE_CREATION_FAILED("임시 파일 생성에 실패했습니다"),
        TEMP_FILE_DELETE_FAILED("임시 파일 삭제에 실패했습니다"),

        // 일반
        ARCHIVE_FAILED("아카이브에 실패했습니다");

        private final String message;

        ErrorCode(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}