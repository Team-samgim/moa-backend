package com.moa.api.preset.exception;

/**
 * Preset 커스텀 예외
 */
public class PresetException extends RuntimeException {

    private final ErrorCode errorCode;

    public PresetException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public PresetException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public PresetException(ErrorCode errorCode, String additionalMessage) {
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
        INVALID_PRESET_NAME("유효하지 않은 프리셋 이름입니다"),
        INVALID_PRESET_TYPE("유효하지 않은 프리셋 타입입니다"),
        INVALID_CONFIG("유효하지 않은 설정입니다"),

        // 프리셋 관련
        PRESET_NOT_FOUND("프리셋을 찾을 수 없습니다"),
        PRESET_CREATION_FAILED("프리셋 생성에 실패했습니다"),
        PRESET_UPDATE_FAILED("프리셋 수정에 실패했습니다"),
        PRESET_DELETE_FAILED("프리셋 삭제에 실패했습니다"),

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