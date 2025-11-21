package com.moa.api.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moa.api.notification.domain.Notification;
import com.moa.api.notification.dto.NotificationCreateRequestDto;
import com.moa.api.notification.dto.NotificationListResponseDto;
import com.moa.api.notification.dto.NotificationResponseDto;
import com.moa.api.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    // 알림 목록 (무한스크롤)
    @Transactional(readOnly = true)
    public NotificationListResponseDto getNotifications(Long memberId, Long cursor, int size) {
        var pageable = PageRequest.of(0, size);

        Slice<Notification> slice;
        if (cursor == null) {
            slice = notificationRepository.findByMemberIdOrderByIdDesc(memberId, pageable);
        } else {
            slice = notificationRepository.findByMemberIdAndIdLessThanOrderByIdDesc(memberId, cursor, pageable);
        }

        List<NotificationResponseDto> items = slice.getContent().stream()
                .map(this::toDto)
                .toList();

        Long nextCursor = slice.hasNext() && !items.isEmpty()
                ? items.get(items.size() - 1).id()
                : null;

        long unreadCount = notificationRepository.countByMemberIdAndIsReadFalse(memberId);

        return new NotificationListResponseDto(items, nextCursor, slice.hasNext(), unreadCount);
    }

    // 알림 생성 (시스템 내부에서 호출)
    public NotificationResponseDto createNotification(Long memberId, NotificationCreateRequestDto req) {
        String configJson = toJson(req.config());
        Notification notification = new Notification(
                memberId,
                req.type(),
                req.title(),
                req.content(),
                configJson
        );
        Notification saved = notificationRepository.save(notification);
        return toDto(saved);
    }

    // 단건 읽음 처리
    public void markAsRead(Long memberId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다. id=" + notificationId));

        if (!notification.getMemberId().equals(memberId)) {
            throw new IllegalStateException("본인 알림만 읽음 처리할 수 있습니다.");
        }

        if (!notification.isRead()) {
            notification.markRead();
            notificationRepository.save(notification); // 명시적으로 저장
            log.debug("알림 읽음 처리 완료: notificationId={}, memberId={}", notificationId, memberId);
        }
    }

    // 전체 읽음 처리
    public void markAllAsRead(Long memberId) {
        // 간단하게: 현재 페이지 전체 불러와서 isRead true로 변경
        // (실 서비스면 bulk update 쿼리로 바꾸는 것도 가능)
        List<Notification> notifications =
                notificationRepository.findByMemberIdOrderByIdDesc(memberId, PageRequest.of(0, 1000))
                        .getContent();

        int updatedCount = 0;
        for (Notification n : notifications) {
            if (!n.isRead()) {
                n.markRead();
                updatedCount++;
            }
        }

        if (updatedCount > 0) {
            notificationRepository.saveAll(notifications); // 명시적으로 저장
            log.debug("전체 알림 읽음 처리 완료: memberId={}, updatedCount={}", memberId, updatedCount);
        }
    }

    // ===== 내부 유틸 =====

    private NotificationResponseDto toDto(Notification n) {
        Map<String, Object> config = fromJson(n.getConfigJson());
        return new NotificationResponseDto(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getContent(),
                config,
                n.isRead(),
                n.getCreatedAt()
        );
    }

    private String toJson(Map<String, Object> map) {
        if (map == null) return "{}";
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private Map<String, Object> fromJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}