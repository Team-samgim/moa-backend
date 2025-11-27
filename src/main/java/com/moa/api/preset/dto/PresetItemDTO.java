package com.moa.api.preset.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * ==============================================================
 * PresetItemDTO
 * --------------------------------------------------------------
 * 마이페이지 또는 Preset 목록 조회 시 사용하는 단일 Preset 정보 DTO.
 *
 * 포함 정보:
 *   - presetId       : Preset의 PK
 *   - presetName     : 사용자 지정 Preset 이름
 *   - presetType     : SEARCH / PIVOT / CHART 등 Preset 유형
 *   - config         : jsonb 컬럼을 그대로 JsonNode로 변환한 구조적 설정 정보
 *   - favorite       : 즐겨찾기 여부
 *   - createdAt      : Preset 생성 시각
 *   - updatedAt      : Preset 마지막 수정 시각
 *
 * 특징:
 *   - config는 JsonNode 형태이므로 프론트에서 구조적으로 그대로 활용 가능
 *   - Preset Entity → DTO 변환은 Service/Converter에서 담당
 *   - 읽기 전용 DTO이므로 Setter 없음
 * ==============================================================
 * AUTHOR        : 방대혁
 */
@Getter
@AllArgsConstructor
public class PresetItemDTO {

    /** Preset 기본키 (ID) */
    private Integer presetId;

    /** 사용자 지정 Preset 이름 */
    private String presetName;

    /** Preset 타입 (SEARCH / PIVOT / CHART 등) */
    private String presetType;

    /** jsonb 컬럼을 그대로 매핑해 제공하는 설정 정보(JsonNode) */
    private JsonNode config;

    /** 즐겨찾기 여부 */
    private boolean favorite;

    /** 생성 시각 */
    private LocalDateTime createdAt;

    /** 마지막 수정 시각 */
    private LocalDateTime updatedAt;
}
