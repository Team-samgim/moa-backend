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
 * ==============================================================
 * NotificationService
 * --------------------------------------------------------------
 * 알림(Notification) 생성/조회/수정에 대한 도메인 서비스.
 *
 * 주요 기능:
 *   - 알림 조회 (cursor-based infinite scrolling)
 *   - 알림 생성
 *   - 단건 읽음 처리
 *   - 전체 읽음 처리 (bulk update)
 *   - 프리셋/CSV Export 등 모듈 기능 완료 시 자동 알림
 *
 * 설계 특징:
 *   - Slice<T> 기반 커서 페이징으로 무한 스크롤 최적화
 *   - ObjectMapper로 configJson(JSON 문자열) 직렬화/역직렬화
 *   - Validator 계층을 통해 요청 유효성 사전 검증
 *   - NotificationException 기반의 예외 추상화
 * ==============================================================
 * AUTHOR        : 방대혁
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationValidator validator;  // 요청값 검증기
    private final ObjectMapper objectMapper;       // JSON 직렬화/역직렬화

    /**
     * ==============================================================
     * 알림 목록 조회 (무한 스크롤)
     * --------------------------------------------------------------
     * - cursor == null → 첫 페이지 조회
     * - cursor != null → cursor(ID) 보다 작은 데이터 조회
     * - Slice<T> 기반 hasNext 플래그로 클라이언트가 다음 호출 여부 판단
     *
     * @param memberId 로그인 사용자 ID
     * @param cursor 이전 페이지의 마지막 Notification ID
     * @param size 요청 페이지 크기
     * @return NotificationListResponseDto
     * ==============================================================
     */
    @Transactional(readOnly = true)
    public NotificationListResponseDto getNotifications(Long memberId, Long cursor, int size) {

        log.debug("Getting notifications: memberId={}, cursor={}, size={}",
                memberId, cursor, size);

        try {
            /* 1) 파라미터 검증 */
            validator.validateCursor(cursor);
            int normalizedSize = validator.normalizeSize(size);

            /* 2) 페이징 정보 생성 (page=0 고정) */
            PageRequest pageable = PageRequest.of(0, normalizedSize);

            /* 3) 알림 조회 — 첫 페이지 or cursor 기반 */
            Slice<Notification> slice = (cursor == null)
                    ? notificationRepository.findByMemberIdOrderByIdDesc(memberId, pageable)
                    : notificationRepository.findByMemberIdAndIdLessThanOrderByIdDesc(memberId, cursor, pageable);

            /* 4) 엔티티 리스트 → DTO 변환 */
            List<NotificationResponseDto> items = slice.getContent().stream()
                    .map(this::toDto)
                    .toList();

            /* 5) 다음 cursor 계산 */
            Long nextCursor = calculateNextCursor(slice, items);

            /* 6) 읽지 않은 알림 개수 */
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
            throw e;  // 그대로 전달
        } catch (Exception e) {
            log.error("Unexpected error while getting notifications: memberId={}", memberId, e);
            throw new NotificationException(
                    NotificationException.ErrorCode.DATABASE_ERROR,
                    e
            );
        }
    }

    /**
     * ==============================================================
     * 알림 생성
     * --------------------------------------------------------------
     * - 요청값 검증
     * - JSON 변환(config)
     * - Notification 엔티티 생성 및 저장
     *
     * @param memberId 알림을 받는 사용자 ID
     * @param req 생성 요청 DTO
     * @return NotificationResponseDto
     * ==============================================================
     */
    @Transactional
    public NotificationResponseDto createNotification(Long memberId, NotificationCreateRequestDto req) {

        log.info("Creating notification: memberId={}, type={}, title={}",
                memberId, req.type(), req.title());

        try {
            /* 1) 요청 검증 */
            validator.validateCreateRequest(req);

            /* 2) config(Map) → JSON 직렬화 */
            String configJson = serializeConfig(req.config());

            /* 3) 엔티티 생성 */
            Notification notification = new Notification(
                    memberId,
                    req.type().trim(),
                    req.title().trim(),
                    req.content().trim(),
                    configJson
            );

            /* 4) 저장 */
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
     * ==============================================================
     * 단건 읽음 처리
     * --------------------------------------------------------------
     * 1) 알림 ID 유효성 검사
     * 2) 알림 존재 여부 확인
     * 3) 사용자 본인 소유인지 체크
     * 4) 읽음 처리 후 저장
     *
     * @param memberId 로그인 사용자 ID
     * @param notificationId 읽음 처리할 알림 ID
     * ==============================================================
     */
    @Transactional
    public void markAsRead(Long memberId, Long notificationId) {

        log.info("Marking notification as read: notificationId={}, memberId={}",
                notificationId, memberId);

        try {
            /* 1) Notification ID 검증 */
            validator.validateNotificationId(notificationId);

            /* 2) 알림 조회 */
            Notification notification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new NotificationException(
                            NotificationException.ErrorCode.NOTIFICATION_NOT_FOUND,
                            "알림을 찾을 수 없습니다: notificationId=" + notificationId
                    ));

            /* 3) 소유자 확인 */
            if (!notification.getMemberId().equals(memberId)) {
                throw new NotificationException(
                        NotificationException.ErrorCode.FORBIDDEN,
                        "본인의 알림만 읽음 처리할 수 있습니다"
                );
            }

            /* 4) 읽음 처리 — 이미 읽은 상태라면 그대로 둠 */
            if (!notification.isRead()) {
                notification.markRead();            // 엔티티 상태 변경
                notificationRepository.save(notification);  // update 수행
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
     * ==============================================================
     * 전체 읽음 처리 (Bulk Update)
     * --------------------------------------------------------------
     * NotificationRepository 의 @Modifying JPQL 을 사용하여
     * UPDATE 쿼리를 한 번에 수행함 → 성능 최적화
     *
     * @param memberId 로그인 사용자 ID
     * ==============================================================
     */
    @Transactional
    public void markAllAsRead(Long memberId) {

        log.info("Marking all notifications as read: memberId={}", memberId);

        try {
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
     * ==============================================================
     * 읽지 않은 알림 개수 조회
     * --------------------------------------------------------------
     *
     * @param memberId 로그인 유저 ID
     * @return 읽지 않은 알림 개수
     * ==============================================================
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
     * ==============================================================
     * Grid CSV Export 완료 알림 전송
     * --------------------------------------------------------------
     * 외부 모듈(그리드 export)이 완료될 때 자동으로 알림을 생성.
     * 실패해도 전체 기능 흐름은 실패시키지 않도록 try-catch 내부에서 처리.
     *
     * @param memberId 사용자 ID
     * @param fileName 생성된 CSV 파일명
     * @param exportId Export 기록 ID
     * ==============================================================
     */
    @Transactional
    public void notifyGridExportCompleted(Long memberId, String fileName, Long exportId) {

        log.info("Sending grid export notification: memberId={}, fileName={}, exportId={}",
                memberId, fileName, exportId);

        try {
            /* 알림용 config 객체 */
            Map<String, Object> config = Map.of(
                    "exportId", exportId,
                    "fileName", fileName,
                    "exportType", "GRID"
            );

            NotificationCreateRequestDto request = new NotificationCreateRequestDto(
                    "INFO",
                    "그리드 CSV 내보내기 완료",
                    String.format("'%s' 파일이 성공적으로 생성되었습니다.", fileName),
                    config
            );

            /* 일반 알림 생성 로직 재사용 */
            createNotification(memberId, request);

        } catch (Exception e) {
            log.error("Failed to send grid export notification: memberId={}, exportId={}",
                    memberId, exportId, e);
        }
    }

    /**
     * ==============================================================
     * Preset 저장 완료 알림
     * --------------------------------------------------------------
     * 검색 프리셋 저장 시 사용자에게 성공 메시지 전송
     *
     * @param memberId 회원 ID
     * @param presetName 프리셋명
     * @param presetId ID
     * ==============================================================
     */
    @Transactional
    public void notifyPresetSaved(Long memberId, String presetName, Integer presetId) {

        log.info("Sending preset saved notification: memberId={}, presetName={}, presetId={}",
                memberId, presetName, presetId);

        try {
            Map<String, Object> config = Map.of(
                    "presetId", presetId,
                    "presetName", presetName,
                    "presetType", "SEARCH"
            );

            NotificationCreateRequestDto request = new NotificationCreateRequestDto(
                    "INFO",
                    "검색 프리셋 저장 완료",
                    String.format("'%s' 프리셋이 성공적으로 저장되었습니다.", presetName),
                    config
            );

            createNotification(memberId, request);

        } catch (Exception e) {
            log.error("Failed to send preset saved notification: memberId={}, presetId={}",
                    memberId, presetId, e);
        }
    }

    /**
     * ==============================================================
     * Notification → DTO 변환
     * ==============================================================
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
     * ==============================================================
     * Config(Map) → JSON 문자열 직렬화
     * ==============================================================
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
     * ==============================================================
     * JSON 문자열 → Map 역직렬화
     *
     * 실패해도 Notification 자체는 보여줘야 하므로 예외 발생시키지 않고
     * 빈 Map 반환하여 안전성 확보.
     * ==============================================================
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
     * ==============================================================
     * 다음 커서 계산
     * --------------------------------------------------------------
     * hasNext == true 이고, items 존재하면
     *   → 마지막 아이템 ID를 cursor로 사용
     *
     * @return nextCursor 또는 null
     * ==============================================================
     */
    private Long calculateNextCursor(Slice<Notification> slice, List<NotificationResponseDto> items) {
        if (!slice.hasNext() || items.isEmpty()) {
            return null;
        }

        /* 마지막 아이템의 ID를 cursor로 사용 */
        return items.get(items.size() - 1).id();
    }
}
