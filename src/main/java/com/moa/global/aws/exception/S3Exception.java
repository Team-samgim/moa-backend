package com.moa.global.aws.exception;

/**
 * S3 커스텀 예외
 * ---------------------------------------------------------
 * - AWS S3 연동 과정에서 발생하는 모든 예외를 캡슐화한다.
 * - 비즈니스 구간에서는 AWS SDK의 상세 예외를 노출하지 않고,
 *   S3Exception + ErrorCode 형태로 일관된 예외 처리가 가능하도록 설계.
 * - 예외 메시지는 ErrorCode가 기본 제공하며,
 *   상세 원인은 cause 또는 additionalMessage로 전달 가능.
 *   AUTHOR        : 방대혁
 */
public class S3Exception extends RuntimeException {

    // 어떤 유형의 에러인지 나타내는 Enum
    private final ErrorCode errorCode;

    /**
     * 기본 생성자 — 단순 메시지.
     */
    public S3Exception(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * AWS SDK에서 발생한 실제 예외를 감싸는 생성자.
     * - root cause 기록하여 디버깅 가능
     */
    public S3Exception(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    /**
     * 추가 메시지를 붙여서 보다 구체적인 오류 전달 가능.
     * ex) INVALID_KEY: key=null
     */
    public S3Exception(ErrorCode errorCode, String additionalMessage) {
        super(errorCode.getMessage() + ": " + additionalMessage);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * S3 작업 구분별 에러 코드 정의
     * ---------------------------------------------------------
     * - ControllerAdvice에서 상태코드 매핑 가능
     * - 서비스 로직에서 구체적인 원인 구분 가능
     */
    public enum ErrorCode {
        // -------- 업로드 관련 오류 --------
        UPLOAD_FAILED("S3 업로드에 실패했습니다"),             // 업로드 실패
        INVALID_FILE("유효하지 않은 파일입니다"),             // 파일 null, MIME 불량 등
        FILE_TOO_LARGE("파일 크기가 너무 큽니다"),             // 파일 용량 초과

        // -------- 다운로드/URL 요청 오류 --------
        DOWNLOAD_FAILED("S3 다운로드에 실패했습니다"),         // 다운로드 실패
        PRESIGN_FAILED("Presigned URL 생성에 실패했습니다"),  // presigned URL 실패

        // -------- 삭제 관련 오류 --------
        DELETE_FAILED("S3 삭제에 실패했습니다"),

        // -------- 공통/기본 오류 --------
        INVALID_BUCKET("유효하지 않은 버킷입니다"),            // 존재하지 않는 버킷
        INVALID_KEY("유효하지 않은 객체 키입니다"),            // Key null, 빈 값 등
        S3_CLIENT_ERROR("S3 클라이언트 오류가 발생했습니다");   // AWS SDK 관련 오류

        private final String message;

        ErrorCode(String message) {
            this.message = message;
        }

        /**
         * 사용자용 에러 메시지 (로깅/응답에서 사용)
         */
        public String getMessage() {
            return message;
        }
    }
}
