package com.moa.api.preset.dto;

import com.moa.api.preset.entity.PresetOrigin;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * ==============================================================
 * SaveSearchPresetRequestDTO
 * --------------------------------------------------------------
 * 검색(Search) Preset을 저장할 때 사용하는 요청 DTO.
 *
 * 프론트에서 전달되는 검색 상태(config)와 Preset 이름, 타입 등을
 * 받아 Service → Entity 저장으로 이어지는 입력 구조.
 *
 * 주요 역할:
 *   - 검색 화면에서 "현재 상태를 프리셋으로 저장"할 때 필요한 정보를 전달
 *   - presetType은 SEARCH, PIVOT, CHART 등 유형을 의미하지만
 *     이 DTO는 검색(Search) 기준이므로 보통 "SEARCH" 사용
 *   - favorite은 optional — 값이 없으면 기본 false 처리 가능
 *   - origin은 USER(사용자 생성), EXPORT(내보내기 결과로 생성) 구분
 *
 * 사용 예시(JSON):
 * {
 *   "presetName": "내 5xx 에러 필터",
 *   "presetType": "SEARCH",
 *   "config": { ... 현재 검색 조건 ... },
 *   "favorite": true,
 *   "origin": "USER"
 * }
 * ==============================================================
 * AUTHOR        : 방대혁
 */
@Getter
@Setter
public class SaveSearchPresetRequestDTO {

    /** 프리셋 이름 (필수) */
    private String presetName;

    /** 프리셋 타입 (예: "SEARCH") */
    private String presetType;

    /** 검색 상태 전체를 포함하는 JSON 구조 (필수) */
    private Map<String, Object> config;

    /** 즐겨찾기 여부 (optional) */
    private Boolean favorite;

    /** Preset 생성 출처(USER, EXPORT) */
    private PresetOrigin origin;
}
