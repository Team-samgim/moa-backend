package com.moa.api.preset.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.moa.api.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "presets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Preset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preset_id")
    private Integer presetId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "preset_name", length = 100, nullable = false)
    private String presetName;

    @Column(name = "preset_type", length = 10, nullable = false) // GRID|PIVOT|CHART
    private String presetType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb", nullable = false)
    private JsonNode config; // JSON 문자열

    @Column(name = "favorite", nullable = false)
    private boolean favorite;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
