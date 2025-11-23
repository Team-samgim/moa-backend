package com.moa.api.notification.repository;

import com.moa.api.notification.domain.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Notification Repository
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 회원의 알림 목록 조회 (첫 페이지)
     */
    Slice<Notification> findByMemberIdOrderByIdDesc(Long memberId, Pageable pageable);

    /**
     * 회원의 알림 목록 조회 (커서 기반)
     */
    Slice<Notification> findByMemberIdAndIdLessThanOrderByIdDesc(
            Long memberId,
            Long id,
            Pageable pageable
    );

    /**
     * 회원의 읽지 않은 알림 개수
     */
    long countByMemberIdAndIsReadFalse(Long memberId);

    /**
     * 회원의 모든 알림을 읽음 처리 (Bulk Update)
     *
     * @param memberId 회원 ID
     * @return 업데이트된 행 수
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE n.memberId = :memberId AND n.isRead = false")
    int markAllAsReadByMemberId(@Param("memberId") Long memberId);
}