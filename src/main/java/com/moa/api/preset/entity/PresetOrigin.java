package com.moa.api.preset.entity;

/**
 * PresetOrigin
 * ---------------------------------------------------------
 * 프리셋이 어떤 방식으로 생성되었는지 구분하기 위한 enum.
 *
 * USER   : 사용자가 직접 저장한 프리셋
 * EXPORT : CSV / Excel 등 Grid Export 기능에서 자동 생성된 프리셋
 *
 * MyPage Preset 관리 기능에서 필터링 및統合 조회에 사용됨.
 * AUTHOR        : 방대혁
 */
public enum PresetOrigin {
    USER,   // 사용자가 직접 저장
    EXPORT  // Export 기능으로 자동 생성
}
