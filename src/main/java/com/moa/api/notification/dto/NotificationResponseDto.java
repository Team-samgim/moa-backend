package com.moa.api.notification.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record NotificationResponseDto(
        Long id,
        String type,
        String title,
        String content,
        Map<String, Object> config,
        boolean isRead,
        OffsetDateTime createdAt
) {
}