package com.moa.api.preset.entity;

/**
 * PresetType
 * ---------------------------------------------------------
 * 프리셋의 UI/기능적 유형을 나타내는 enum.
 *
 * SEARCH : 검색 조건 기반의 프리셋 (FilterModel 포함)
 * PIVOT  : Pivot Table 구성을 저장한 프리셋
 * CHART  : 차트 설정(메트릭, 축, 스타일 등)을 저장한 프리셋
 *
 * MyPage 프리셋 목록 및 검색에서 type 필터링에 사용.
 * AUTHOR        : 방대혁
 */
public enum PresetType {
    SEARCH,   // 검색 프리셋
    PIVOT,    // 피벗 프리셋
    CHART     // 차트 프리셋
}
