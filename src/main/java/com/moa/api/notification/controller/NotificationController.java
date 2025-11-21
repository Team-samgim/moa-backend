package com.moa.api.notification.controller;

import com.moa.api.notification.dto.NotificationCreateRequestDto;
import com.moa.api.notification.dto.NotificationListResponseDto;
import com.moa.api.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // 알림 목록 (무한스크롤)
    @GetMapping
    public NotificationListResponseDto getNotifications(
            Authentication auth,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long memberId = (Long) auth.getPrincipal();
        return notificationService.getNotifications(memberId, cursor, size);
    }

    // (옵션) 알림 생성 – 시연용/테스트용
    @PostMapping
    public void createNotification(
            Authentication auth,
            @RequestBody NotificationCreateRequestDto req
    ) {
        Long memberId = (Long) auth.getPrincipal();
        notificationService.createNotification(memberId, req);
    }

    // 단건 읽음 처리
    @PatchMapping("/{id}/read")
    public void markAsRead(
            Authentication auth,
            @PathVariable Long id
    ) {
        Long memberId = (Long) auth.getPrincipal();
        notificationService.markAsRead(memberId, id);
    }

    // 전체 읽음 처리
    @PostMapping("/read-all")
    public void markAllAsRead(Authentication auth) {
        Long memberId = (Long) auth.getPrincipal();
        notificationService.markAllAsRead(memberId);
    }

    // (선택) 안 읽은 개수만 따로
    @GetMapping("/unread-count")
    public long getUnreadCount(Authentication auth) {
        Long memberId = (Long) auth.getPrincipal();
        return notificationService
                .getNotifications(memberId, null, 1)
                .unreadCount();
    }
}
