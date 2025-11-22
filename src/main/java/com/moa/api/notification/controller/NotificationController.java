package com.moa.api.notification.controller;

import com.moa.api.notification.dto.NotificationCreateRequestDto;
import com.moa.api.notification.dto.NotificationListResponseDto;
import com.moa.api.notification.dto.NotificationResponseDto;
import com.moa.api.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Notification Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 알림 목록 조회 (무한 스크롤)
     *
     * @param auth 인증 정보
     * @param cursor 커서 (이전 마지막 알림 ID)
     * @param size 페이지 크기 (기본 20, 최대 100)
     * @return 알림 목록
     */
    @GetMapping
    public ResponseEntity<NotificationListResponseDto> getNotifications(
            Authentication auth,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long memberId = (Long) auth.getPrincipal();

        log.info("GET /api/notifications - memberId={}, cursor={}, size={}",
                memberId, cursor, size);

        NotificationListResponseDto response = notificationService.getNotifications(
                memberId,
                cursor,
                size
        );

        log.debug("Found {} notifications for memberId={} (hasNext={})",
                response.items().size(), memberId, response.hasNext());

        return ResponseEntity.ok(response);
    }

    /**
     * 알림 생성 (테스트/시연용)
     *
     * @param auth 인증 정보
     * @param req 알림 생성 요청
     * @return 생성된 알림
     */
    @PostMapping
    public ResponseEntity<NotificationResponseDto> createNotification(
            Authentication auth,
            @RequestBody NotificationCreateRequestDto req
    ) {
        Long memberId = (Long) auth.getPrincipal();

        log.info("POST /api/notifications - memberId={}, type={}", memberId, req.type());

        NotificationResponseDto response = notificationService.createNotification(
                memberId,
                req
        );

        log.info("Notification created: notificationId={}", response.id());

        return ResponseEntity.ok(response);
    }

    /**
     * 단건 읽음 처리
     *
     * @param auth 인증 정보
     * @param notificationId 알림 ID
     * @return 성공 응답
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            Authentication auth,
            @PathVariable("notificationId") Long notificationId
    ) {
        Long memberId = (Long) auth.getPrincipal();

        log.info("PATCH /api/notifications/{}/read - memberId={}", notificationId, memberId);

        notificationService.markAsRead(memberId, notificationId);

        log.info("Notification marked as read: notificationId={}", notificationId);

        return ResponseEntity.ok(Map.of("ok", true));
    }

    /**
     * 전체 읽음 처리
     *
     * @param auth 인증 정보
     * @return 성공 응답
     */
    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(Authentication auth) {
        Long memberId = (Long) auth.getPrincipal();

        log.info("POST /api/notifications/read-all - memberId={}", memberId);

        notificationService.markAllAsRead(memberId);

        log.info("All notifications marked as read: memberId={}", memberId);

        return ResponseEntity.ok(Map.of("ok", true));
    }

    /**
     * 읽지 않은 알림 개수 조회
     *
     * @param auth 인증 정보
     * @return 읽지 않은 알림 개수
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication auth) {
        Long memberId = (Long) auth.getPrincipal();

        log.debug("GET /api/notifications/unread-count - memberId={}", memberId);

        long count = notificationService.getUnreadCount(memberId);

        log.debug("Unread count for memberId={}: {}", memberId, count);

        return ResponseEntity.ok(Map.of("unreadCount", count));
    }
}