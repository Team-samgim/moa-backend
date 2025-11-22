package com.moa.api.notification.dto;

import java.util.List;

public record NotificationListResponseDto(
        List<NotificationResponseDto> items,
        Long nextCursor,
        boolean hasNext,
        long unreadCount
) {
}