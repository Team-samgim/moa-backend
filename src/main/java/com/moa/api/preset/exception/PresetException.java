package com.moa.api.preset.exception;

/**
 * Preset 커스텀 예외
 * ---------------------------------------------------------
 * 프리셋 기능에서 발생하는 모든 예외를 공통적으로 처리하기 위한 예외 클래스.
 * ErrorCode enum과 함께 사용되며, 컨트롤러 단에서는
 * PresetExceptionHandler에서 공통적으로 처리된다.
 * AUTHOR        : 방대혁
 */
public class PresetException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * 기본 생성자 - 메시지는 ErrorCode의 기본 메시지 사용
     */
    public PresetException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 예외 원인(cause) 전달용
     */
    public PresetException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    /**
     * 기본 메시지 + 추가 메시지 조합
     */
    public PresetException(ErrorCode errorCode, String additionalMessage) {
        super(errorCode.getMessage() + ": " + additionalMessage);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * 에러 코드 정의
     * ---------------------------------------------------------
     * Preset 도메인에서 발생할 수 있는 모든 에러 상황을 표현한다.
     *
     * UNAUTHORIZED         : 인증 실패
     * FORBIDDEN            : 권한 부족
     *
     * INVALID_REQUEST      : 잘못된 요청
     * INVALID_PRESET_NAME  : 이름 형식 또는 유효성 오류
     * INVALID_PRESET_TYPE  : 허용되지 않은 타입
     * INVALID_CONFIG       : config JSON 오류
     *
     * PRESET_NOT_FOUND         : 존재하지 않는 프리셋
     * PRESET_CREATION_FAILED   : 저장 실패
     * PRESET_UPDATE_FAILED     : 수정 실패
     * PRESET_DELETE_FAILED     : 삭제 실패
     *
     * DATABASE_ERROR       : DB 작업 중 오류
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
