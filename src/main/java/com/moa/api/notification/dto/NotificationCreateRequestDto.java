package com.moa.api.notification.dto;

import java.util.Map;

public record NotificationCreateRequestDto(
        String type,
        String title,
        String content,
        Map<String, Object> config
) {
}