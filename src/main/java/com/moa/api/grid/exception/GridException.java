package com.moa.api.grid.exception;

/*****************************************************************************
 CLASS NAME    : GridException
 DESCRIPTION   : Grid API 공통 예외 및 에러 코드 정의
 AUTHOR        : 방대혁
 ******************************************************************************/

/**
 * Grid API 커스텀 예외
 */
public class GridException extends RuntimeException {

    private final ErrorCode errorCode;

    public GridException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public GridException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public GridException(ErrorCode errorCode, String additionalMessage) {
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
        INVALID_FILTER_MODEL("필터 모델 JSON 파싱 실패"),
        INVALID_BASE_SPEC("Base Spec JSON 파싱 실패"),
        INVALID_COLUMN_TYPE("지원하지 않는 컬럼 타입"),
        QUERY_EXECUTION_FAILED("쿼리 실행 실패"),
        LAYER_NOT_FOUND("레이어를 찾을 수 없음"),
        METADATA_LOAD_FAILED("메타데이터 로드 실패");

        private final String message;

        ErrorCode(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
