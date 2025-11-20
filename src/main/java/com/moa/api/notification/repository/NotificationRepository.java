package com.moa.api.notification.repository;

import com.moa.api.notification.domain.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 첫 페이지 (cursor 없이)
    Slice<Notification> findByMemberIdOrderByIdDesc(Long memberId, Pageable pageable);

    // cursor 기반 무한스크롤 (id < cursor)
    Slice<Notification> findByMemberIdAndIdLessThanOrderByIdDesc(Long memberId, Long id, Pageable pageable);

    long countByMemberIdAndIsReadFalse(Long memberId);
}