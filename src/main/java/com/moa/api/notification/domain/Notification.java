package com.moa.api.notification.domain;


import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "notifications")
@DynamicUpdate // 변경된 필드만 UPDATE 쿼리에 포함
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "config", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String configJson; // JSON 문자열로 저장 (DTO에서 Map으로 변환)

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // === 생성자 / 빌더 / getter, setter들 ===
    protected Notification() {}

    public Notification(Long memberId, String type, String title, String content, String configJson) {
        this.memberId = memberId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.configJson = configJson;
        this.isRead = false;
    }

    // getter / setter …
    public Long getId() { return id; }
    public Long getMemberId() { return memberId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getConfigJson() { return configJson; }
    public boolean isRead() { return isRead; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void markRead() {
        this.isRead = true;
    }
}