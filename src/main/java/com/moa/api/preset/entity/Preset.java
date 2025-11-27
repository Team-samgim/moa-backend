package com.moa.api.preset.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.moa.api.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * ==============================================================
 * Preset Entity
 * --------------------------------------------------------------
 * 검색(Search) / 피벗(Pivot) / 차트(Chart) 등
 * 사용자가 저장하는 Grid 설정(Preset)을 관리하는 엔티티.
 *
 * 하나의 Preset은 특정 사용자(member)가 만든 검색 조건(config),
 * 프리셋 타입(presetType), 이름(presetName), 즐겨찾기 여부(favorite),
 * 생성/수정 시각 등을 포함한다.
 *
 * 주요 활용:
 * - MyPage에서 사용자별 Preset 관리
 * - 검색/피벗/차트 상태 저장 및 불러오기
 * - 프리셋 즐겨찾기 관리
 * - 내보내기(Export) 기반 Preset 저장
 * ==============================================================
 * AUTHOR        : 방대혁
 */
@Entity
@Table(name = "presets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Preset {

    /** PK: 프리셋 고유 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preset_id")
    private Integer presetId;

    /**
     * 프리셋을 생성한 사용자
     * - 다대일 관계 (여러 프리셋이 동일 사용자에게 속함)
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** 프리셋 이름 (사용자 입력) */
    @Column(name = "preset_name", length = 100, nullable = false)
    private String presetName;

    /** 프리셋 타입 (SEARCH, PIVOT, CHART 등) */
    @Enumerated(EnumType.STRING)
    @Column(name = "preset_type", length = 10, nullable = false)
    private PresetType presetType;

    /**
     * 검색/피벗/차트 등의 설정 데이터(JSON)
     * PostgreSQL jsonb 타입으로 저장됨
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb", nullable = false)
    private JsonNode config;

    /** 즐겨찾기 여부 */
    @Column(name = "favorite", nullable = false)
    private boolean favorite;

    /** 프리셋 기원 (USER 직접 저장 or EXPORT 자동 생성) */
    @Enumerated(EnumType.STRING)
    @Column(name = "preset_origin", length = 10, nullable = false)
    private PresetOrigin origin;

    /** 생성 시각 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** 수정 시각 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
