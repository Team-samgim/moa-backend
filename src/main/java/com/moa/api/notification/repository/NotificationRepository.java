package com.moa.api.notification.repository;

import com.moa.api.notification.domain.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * ===========================================================
 * NotificationRepository
 * -----------------------------------------------------------
 * Notification 엔티티 전용 Spring Data JPA Repository.
 *
 * 지원 기능:
 *  - 회원별 알림 목록 조회 (커서 기반 / 첫 페이지)
 *  - 읽지 않은 알림 개수 조회
 *  - 전체 알림 읽음 처리 (Bulk Update)
 *
 * 특징:
 *  - Slice<T> 기반 조회를 사용하여 무한 스크롤 페이징을 효율적으로 처리.
 *  - Bulk Update(@Modifying) 사용하여 전체 읽음 처리 시 성능 최적화.
 * ===========================================================
 * AUTHOR        : 방대혁
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * =====================================================
     * 회원의 알림 목록 조회 (첫 페이지)
     * -----------------------------------------------------
     * 사용 예:
     *   GET /api/notifications?size=20
     *
     * Pageable 의 sort 설정은 무시하고,
     * 메서드명 기반 정렬(OrderByIdDesc)이 우선된다.
     *
     * @param memberId 회원 ID
     * @param pageable PageRequest(page=0, size=N)
     * @return Slice<Notification> (hasNext 포함)
     * =====================================================
     */
    Slice<Notification> findByMemberIdOrderByIdDesc(Long memberId, Pageable pageable);

    /**
     * =====================================================
     * 회원의 알림 목록 조회 (커서 기반)
     * -----------------------------------------------------
     * 사용 예:
     *   GET /api/notifications?cursor=100&size=20
     *
     * cursor(ID) 보다 작은 알림들을 조회하여 무한 스크롤 구현.
     * ORDER BY id DESC 형태로 최신순 유지.
     *
     * @param memberId 회원 ID
     * @param id cursor (마지막 조회 알림 ID)
     * @param pageable PageRequest(size=N)
     * @return Slice<Notification>
     * =====================================================
     */
    Slice<Notification> findByMemberIdAndIdLessThanOrderByIdDesc(
            Long memberId,
            Long id,
            Pageable pageable
    );

    /**
     * =====================================================
     * 읽지 않은 알림 개수 조회
     * -----------------------------------------------------
     * 알림 아이콘 뱃지 표시 등에 사용.
     *
     * @param memberId 회원 ID
     * @return 읽지 않은 알림 수
     * =====================================================
     */
    long countByMemberIdAndIsReadFalse(Long memberId);

    /**
     * =====================================================
     * 전체 읽음 처리 (Bulk Update)
     * -----------------------------------------------------
     * JPA의 Dirty Checking 기반 update가 아니라,
     * 직접 UPDATE 쿼리를 실행하여 한 번에 처리 → 성능 최적화.
     *
     * @Modifying 필요 (update 수행)
     * @Transactional(readOnly = false) 를 호출 측에서 보장해야 함.
     *
     *   UPDATE notifications
     *   SET is_read = true, updated_at = CURRENT_TIMESTAMP
     *   WHERE member_id = ? AND is_read = false
     *
     * @param memberId 회원 ID
     * @return 업데이트된 row 개수
     * =====================================================
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE n.memberId = :memberId AND n.isRead = false")
    int markAllAsReadByMemberId(@Param("memberId") Long memberId);
}
