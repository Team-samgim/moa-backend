package com.moa.api.notification.validation;

import com.moa.api.notification.dto.NotificationCreateRequestDto;
import com.moa.api.notification.exception.NotificationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * ==============================================================
 * NotificationValidator
 * --------------------------------------------------------------
 * Notification 생성/조회/수정 요청에 사용되는 입력값 검증기.
 *
 * 주요 검증 항목:
 *   - type 길이, 유효한 값 여부
 *   - title/contents 길이 및 존재 여부
 *   - config 크기 제한
 *   - pagination(cursor, size) 값 검증
 *   - 알림 ID 검증
 *
 * 설계 특징:
 *   - Validation 실패 시 NotificationException을 통해 일관된 에러 처리
 *   - LENGTH/NULL 체크를 별도 메서드로 분리하여 재사용성↑
 *   - Config는 유연성을 위해 엄격하게 막지 않고 경고 수준으로 제한
 * ==============================================================
 */
@Slf4j
@Component
public class NotificationValidator {

    /* ================================
       상수 정의 (길이 제한)
       ================================ */
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_CONTENT_LENGTH = 5000;
    private static final int MAX_TYPE_LENGTH = 50;

    /**
     * 허용된 Notification 타입 목록
     * - 모니터링/시스템 이벤트와 일반 INFO 타입 포함
     * - 등록되지 않은 타입은 경고만 출력하고 허용하여 확장성 유지
     */
    private static final List<String> VALID_TYPES = List.of(
            "ANOMALY_5XX_RATIO",
            "HIGH_TRAFFIC",
            "INFO",
            "SYSTEM",
            "ALERT",
            "WARNING"
    );

    /**
     * ==============================================================
     * 알림 생성 요청 전체 검증
     * --------------------------------------------------------------
     * - type/title/content/config를 각각 개별 검증 메서드로 분리
     * - 중복 로직 제거, 유지보수성 증가
     * ==============================================================
     */
    public void validateCreateRequest(NotificationCreateRequestDto req) {

        validateType(req.type());
        validateTitle(req.title());
        validateContent(req.content());
        validateConfig(req.config());
    }

    /**
     * ==============================================================
     * TYPE 검증
     * --------------------------------------------------------------
     * - NOT NULL
     * - 길이 <= 50
     * - 유효한 type인지 검사 (등록되지 않은 경우 warn만 출력)
     * ==============================================================
     */
    public void validateType(String type) {
        if (type == null || type.isBlank()) {
            throw new NotificationException(
                    NotificationException.ErrorCode.INVALID_TYPE,
                    "알림 타입은 필수입니다"
            );
        }

        String trimmed = type.trim();

        // 길이 제한
        if (trimmed.length() > MAX_TYPE_LENGTH) {
            throw new NotificationException(
                    NotificationException.ErrorCode.INVALID_TYPE,
                    "알림 타입이 너무 깁니다 (최대 " + MAX_TYPE_LENGTH + "자)"
            );
        }

        // 사전에 정의된 타입인지 확인
        // 정의되지 않았다고 해서 막지는 않음. 시스템 확장성 때문.
        if (!VALID_TYPES.contains(trimmed)) {
            log.warn("Unknown notification type: {}", trimmed);
        }
    }

    /**
     * ==============================================================
     * TITLE 검증
     * --------------------------------------------------------------
     * - NOT NULL
     * - 길이 <= 200
     * ==============================================================
     */
    public void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new NotificationException(
                    NotificationException.ErrorCode.INVALID_TITLE,
                    "알림 제목은 필수입니다"
            );
        }

        String trimmed = title.trim();

        if (trimmed.length() > MAX_TITLE_LENGTH) {
            throw new NotificationException(
                    NotificationException.ErrorCode.INVALID_TITLE,
                    "알림 제목이 너무 깁니다 (최대 " + MAX_TITLE_LENGTH + "자)"
            );
        }
    }

    /**
     * ==============================================================
     * CONTENT 검증
     * --------------------------------------------------------------
     * - NOT NULL
     * - 길이 <= 5000
     * - Slack/Email 알림으로 확장될 때도 유효한 기준
     * ==============================================================
     */
    public void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new NotificationException(
                    NotificationException.ErrorCode.INVALID_CONTENT,
                    "알림 내용은 필수입니다"
            );
        }

        String trimmed = content.trim();

        if (trimmed.length() > MAX_CONTENT_LENGTH) {
            throw new NotificationException(
                    NotificationException.ErrorCode.INVALID_CONTENT,
                    "알림 내용이 너무 깁니다 (최대 " + MAX_CONTENT_LENGTH + "자)"
            );
        }
    }

    /**
     * ==============================================================
     * CONFIG 검증
     * --------------------------------------------------------------
     * - 프리셋/Export 등의 부가 정보를 포함
     * - null 허용
     * - key가 너무 많으면 경고만 출력 (100개 ↑)
     * --------------------------------------------------------------
     * NOTE:
     *   config는 도메인별로 추가 가능성이 크므로
     *   "엄격한 구조 검증" 대신 유연성을 유지하는 설계.
     * ==============================================================
     */
    public void validateConfig(Map<String, Object> config) {

        if (config == null) {
            return; // Config는 optional
        }

        // Map 크기 제한 (주의만 줌)
        if (config.size() > 100) {
            log.warn("Config has too many keys: {}", config.size());
        }
    }

    /**
     * ==============================================================
     * Notification ID 검증
     * --------------------------------------------------------------
     * - ID는 양수로 제한
     * - 0이나 음수 → INVALID_REQUEST
     * ==============================================================
     */
    public void validateNotificationId(Long notificationId) {
        if (notificationId == null || notificationId <= 0) {
            throw new NotificationException(
                    NotificationException.ErrorCode.INVALID_REQUEST,
                    "유효하지 않은 알림 ID: " + notificationId
            );
        }
    }

    /**
     * ==============================================================
     * 요청 Size 정규화 (Pagination)
     * --------------------------------------------------------------
     * - 최소 1
     * - 최대 100
     * - Grid/Notification API 일관성 유지
     * ==============================================================
     */
    public int normalizeSize(int size) {
        return Math.max(1, Math.min(size, 100));
    }

    /**
     * ==============================================================
     * Cursor 검증
     * --------------------------------------------------------------
     * - null은 허용 (첫 페이지 의미)
     * - 0 이하이면 INVALID_REQUEST
     * ==============================================================
     */
    public void validateCursor(Long cursor) {
        if (cursor != null && cursor <= 0) {
            throw new NotificationException(
                    NotificationException.ErrorCode.INVALID_REQUEST,
                    "유효하지 않은 커서: " + cursor
            );
        }
    }
}
