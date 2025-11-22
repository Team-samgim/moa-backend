package com.moa.api.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moa.api.notification.domain.Notification;
import com.moa.api.notification.dto.NotificationCreateRequestDto;
import com.moa.api.notification.dto.NotificationListResponseDto;
import com.moa.api.notification.dto.NotificationResponseDto;
import com.moa.api.notification.exception.NotificationException;
import com.moa.api.notification.repository.NotificationRepository;
import com.moa.api.notification.validation.NotificationValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Notification Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationValidator validator;
    private final ObjectMapper objectMapper;

    /**
     * 알림 목록 조회 (무한 스크롤)
     *
     * @param memberId 회원 ID
     * @param cursor 커서 (이전 마지막 알림 ID)
     * @param size 페이지 크기
     * @return 알림 목록
     */
    @Transactional(readOnly = true)
    public NotificationListResponseDto getNotifications(Long memberId, Long cursor, int size) {
        log.debug("Getting notifications: memberId={}, cursor={}, size={}",
                memberId, cursor, size);

        try {
            // 1) 파라미터 검증
            validator.validateCursor(cursor);
            int normalizedSize = validator.normalizeSize(size);

            // 2) 알림 조회
            PageRequest pageable = PageRequest.of(0, normalizedSize);

            Slice<Notification> slice;
            if (cursor == null) {
                // 첫 페이지
                slice = notificationRepository.findByMemberIdOrderByIdDesc(memberId, pageable);
            } else {
                // 다음 페이지 (cursor-based pagination)
                slice = notificationRepository.findByMemberIdAndIdLessThanOrderByIdDesc(
                        memberId,
                        cursor,
                        pageable
                );
            }

            // 3) DTO 변환
            List<NotificationResponseDto> items = slice.getContent().stream()
                    .map(this::toDto)
                    .toList();

            // 4) 다음 커서 계산
            Long nextCursor = calculateNextCursor(slice, items);

            // 5) 읽지 않은 개수 조회
            long unreadCount = notificationRepository.countByMemberIdAndIsReadFalse(memberId);

            log.debug("Found {} notifications for memberId={}, unreadCount={}",
                    items.size(), memberId, unreadCount);

            return new NotificationListResponseDto(
                    items,
                    nextCursor,
                    slice.hasNext(),
                    unreadCount
            );

        } catch (NotificationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while getting notifications: memberId={}", memberId, e);
            throw new NotificationException(
                    NotificationException.ErrorCode.DATABASE_ERROR,
                    e
            );
        }
    }

    /**
     * 알림 생성
     *
     * @param memberId 회원 ID
     * @param req 알림 생성 요청
     * @return 생성된 알림
     */
    @Transactional
    public NotificationResponseDto createNotification(Long memberId, NotificationCreateRequestDto req) {
        log.info("Creating notification: memberId={}, type={}, title={}",
                memberId, req.type(), req.title());

        try {
            // 1) 검증
            validator.validateCreateRequest(req);

            // 2) Config를 JSON으로 변환
            String configJson = serializeConfig(req.config());

            // 3) 알림 생성
            Notification notification = new Notification(
                    memberId,
                    req.type().trim(),
                    req.title().trim(),
                    req.content().trim(),
                    configJson
            );

            // 4) 저장
            Notification saved = notificationRepository.save(notification);

            log.info("Notification created successfully: notificationId={}, memberId={}",
                    saved.getId(), memberId);

            return toDto(saved);

        } catch (NotificationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while creating notification: memberId={}", memberId, e);
            throw new NotificationException(
                    NotificationException.ErrorCode.NOTIFICATION_CREATION_FAILED,
                    e
            );
        }
    }

    /**
     * 단건 읽음 처리
     *
     * @param memberId 회원 ID
     * @param notificationId 알림 ID
     */
    @Transactional
    public void markAsRead(Long memberId, Long notificationId) {
        log.info("Marking notification as read: notificationId={}, memberId={}",
                notificationId, memberId);

        try {
            // 1) Notification ID 검증
            validator.validateNotificationId(notificationId);

            // 2) 알림 조회
            Notification notification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new NotificationException(
                            NotificationException.ErrorCode.NOTIFICATION_NOT_FOUND,
                            "알림을 찾을 수 없습니다: notificationId=" + notificationId
                    ));

            // 3) 소유자 확인
            if (!notification.getMemberId().equals(memberId)) {
                throw new NotificationException(
                        NotificationException.ErrorCode.FORBIDDEN,
                        "본인의 알림만 읽음 처리할 수 있습니다"
                );
            }

            // 4) 읽음 처리 (이미 읽은 경우 스킵)
            if (!notification.isRead()) {
                notification.markRead();
                notificationRepository.save(notification);
                log.info("Notification marked as read: notificationId={}", notificationId);
            } else {
                log.debug("Notification already read: notificationId={}", notificationId);
            }

        } catch (NotificationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while marking notification as read: notificationId={}",
                    notificationId, e);
            throw new NotificationException(
                    NotificationException.ErrorCode.NOTIFICATION_UPDATE_FAILED,
                    e
            );
        }
    }

    /**
     * 전체 읽음 처리
     *
     * @param memberId 회원 ID
     */
    @Transactional
    public void markAllAsRead(Long memberId) {
        log.info("Marking all notifications as read: memberId={}", memberId);

        try {
            // Bulk Update 쿼리 사용
            int updatedCount = notificationRepository.markAllAsReadByMemberId(memberId);

            log.info("Marked {} notifications as read for memberId={}", updatedCount, memberId);

        } catch (Exception e) {
            log.error("Unexpected error while marking all notifications as read: memberId={}",
                    memberId, e);
            throw new NotificationException(
                    NotificationException.ErrorCode.NOTIFICATION_UPDATE_FAILED,
                    e
            );
        }
    }

    /**
     * 읽지 않은 알림 개수 조회
     *
     * @param memberId 회원 ID
     * @return 읽지 않은 알림 개수
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long memberId) {
        log.debug("Getting unread count: memberId={}", memberId);

        try {
            long count = notificationRepository.countByMemberIdAndIsReadFalse(memberId);

            log.debug("Unread count for memberId={}: {}", memberId, count);

            return count;

        } catch (Exception e) {
            log.error("Unexpected error while getting unread count: memberId={}", memberId, e);
            throw new NotificationException(
                    NotificationException.ErrorCode.DATABASE_ERROR,
                    e
            );
        }
    }

    /**
     * Notification -> DTO 변환
     */
    private NotificationResponseDto toDto(Notification notification) {
        Map<String, Object> config = deserializeConfig(notification.getConfigJson());

        return new NotificationResponseDto(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                config,
                notification.isRead(),
                notification.getCreatedAt()
        );
    }

    /**
     * Config Map -> JSON String 변환
     */
    private String serializeConfig(Map<String, Object> config) {
        if (config == null || config.isEmpty()) {
            return "{}";
        }

        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize config to JSON", e);
            throw new NotificationException(
                    NotificationException.ErrorCode.JSON_SERIALIZE_ERROR,
                    e
            );
        }
    }

    /**
     * JSON String -> Config Map 변환
     */
    private Map<String, Object> deserializeConfig(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to deserialize config JSON, returning empty map: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 다음 커서 계산
     */
    private Long calculateNextCursor(Slice<Notification> slice, List<NotificationResponseDto> items) {
        if (!slice.hasNext() || items.isEmpty()) {
            return null;
        }

        return items.get(items.size() - 1).id();
    }
}