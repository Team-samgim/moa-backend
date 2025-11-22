package com.moa.api.notification.validation;

import com.moa.api.notification.dto.NotificationCreateRequestDto;
import com.moa.api.notification.exception.NotificationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Notification 입력값 검증기
 */
@Slf4j
@Component
public class NotificationValidator {

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_CONTENT_LENGTH = 5000;
    private static final int MAX_TYPE_LENGTH = 50;

    /**
     * 유효한 알림 타입 목록
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
     * Notification 생성 요청 검증
     */
    public void validateCreateRequest(NotificationCreateRequestDto req) {
        // Type 검증
        validateType(req.type());

        // Title 검증
        validateTitle(req.title());

        // Content 검증
        validateContent(req.content());

        // Config 검증
        validateConfig(req.config());
    }

    /**
     * Type 검증
     */
    public void validateType(String type) {
        if (type == null || type.isBlank()) {
            throw new NotificationException(
                    NotificationException.ErrorCode.INVALID_TYPE,
                    "알림 타입은 필수입니다"
            );
        }

        String trimmed = type.trim();

        if (trimmed.length() > MAX_TYPE_LENGTH) {
            throw new NotificationException(
                    NotificationException.ErrorCode.INVALID_TYPE,
                    "알림 타입이 너무 깁니다 (최대 " + MAX_TYPE_LENGTH + "자)"
            );
        }

        // 유효한 타입인지 확인 (선택사항)
        if (!VALID_TYPES.contains(trimmed)) {
            log.warn("Unknown notification type: {}", trimmed);
            // 경고만 하고 통과 (유연성 유지)
        }
    }

    /**
     * Title 검증
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
     * Content 검증
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
     * Config 검증
     */
    public void validateConfig(Map<String, Object> config) {
        // Config는 선택사항 (null 허용)
        if (config == null) {
            return;
        }

        // Config 크기 체크
        if (config.size() > 100) {
            log.warn("Config has too many keys: {}", config.size());
        }
    }

    /**
     * Notification ID 검증
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
     * 페이지 사이즈 정규화
     */
    public int normalizeSize(int size) {
        return Math.max(1, Math.min(size, 100)); // 최소 1, 최대 100
    }

    /**
     * Cursor 검증
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