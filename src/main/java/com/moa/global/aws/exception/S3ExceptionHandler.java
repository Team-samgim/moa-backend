package com.moa.global.aws.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * S3 전역 예외 핸들러
 * ------------------------------------------------------------
 * - AWS S3 작업(upload, download, delete, presign 등)에서 발생하는
 *   S3Exception을 전역적으로 처리하는 핸들러.
 * - ErrorCode에 따라 HttpStatus 매핑.
 * - Upload/Download/Delete 관련 도메인 서비스에서 발생하는 오류를
 *   일관된 JSON 형태로 클라이언트에 응답.
 * - Export, FileUpload 등 다른 API에서도 공통으로 활용 가능.
 *
 * AUTHOR        : 방대혁
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE) // 가장 우선적으로 처리
public class S3ExceptionHandler {

    /**
     * S3Exception 처리
     * ------------------------------------------------------------
     * - ErrorCode 그룹에 맞는 HTTP 상태코드를 반환한다.
     * - 상세 메시지는 S3Exception 내부의 message 사용.
     */
    @ExceptionHandler(S3Exception.class)
    public ResponseEntity<ErrorResponse> handleS3Exception(S3Exception ex) {
        log.error("S3Exception: {} - {}", ex.getErrorCode(), ex.getMessage(), ex);

        HttpStatus status = switch (ex.getErrorCode()) {
            // 잘못된 입력(파일, 버킷, 키 등)
            case INVALID_BUCKET, INVALID_KEY, INVALID_FILE, FILE_TOO_LARGE ->
                    HttpStatus.BAD_REQUEST;

            // 서버 내부 오류(AWS 통신 실패, 업로드 실패 등)
            case UPLOAD_FAILED, DELETE_FAILED, DOWNLOAD_FAILED,
                 PRESIGN_FAILED, S3_CLIENT_ERROR ->
                    HttpStatus.INTERNAL_SERVER_ERROR;
        };

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(ex.getErrorCode().name())
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(status).body(response);
    }

    /**
     * 에러 응답 DTO
     * ------------------------------------------------------------
     * - 공통 에러 응답 포맷
     * - timestamp: 오류 발생 시각
     * - status: HTTP 상태 코드
     * - error: ErrorCode 이름
     * - message: 상세 오류 메시지
     */
    @lombok.Builder
    @lombok.Getter
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
    }
}
