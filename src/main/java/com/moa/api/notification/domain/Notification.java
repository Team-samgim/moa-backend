package com.moa.api.notification.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/*****************************************************************************
 CLASS NAME    : Notification
 DESCRIPTION   : 알림(Notification) 엔티티
 - 알림 제목/내용
 - 알림 타입 (예: EXPORT_DONE, PRESET_SHARE 등)
 - configJson: 알림 상세 설정(JSON 저장)
 - 읽음 여부(isRead)
 - 생성/수정 시간 자동 관리
 AUTHOR        : 방대혁
 ******************************************************************************/
@Entity
@Table(name = "notifications")
@DynamicUpdate // 변경된 필드만 UPDATE 쿼리에 포함
public class Notification {

    /** 기본 키 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 알림을 받은 사용자 ID */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 알림 타입 (업무 구분용) */
    @Column(name = "type", nullable = false, length = 50)
    private String type;

    /** 알림 제목 */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** 알림 상세 내용 */
    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    /**
     * config JSON (text 형태로 저장)
     * 예: {"presetId": 12, "fileName": "report.pdf"}
     */
    @Column(name = "config", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String configJson;

    /** 읽음 여부 */
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    /** 생성 시각 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** 마지막 수정 시각 */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /** INSERT 전 자동 설정 */
    @PrePersist
    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** UPDATE 전 수정 시간 갱신 */
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // === 생성자 / 빌더 / getter, setter들 ===

    protected Notification() {}

    /**
     * 기본 생성자
     * 알림 생성 시 필수 정보 입력용
     */
    public Notification(Long memberId, String type, String title, String content, String configJson) {
        this.memberId = memberId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.configJson = configJson;
        this.isRead = false;
    }

    // === Getter ===
    public Long getId() { return id; }
    public Long getMemberId() { return memberId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getConfigJson() { return configJson; }
    public boolean isRead() { return isRead; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    /**
     * 알림 읽음 처리
     * isRead = true
     */
    public void markRead() {
        this.isRead = true;
    }
}
