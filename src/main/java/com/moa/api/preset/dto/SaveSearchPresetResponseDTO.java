package com.moa.api.preset.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ==============================================================
 * SaveSearchPresetResponseDTO
 * --------------------------------------------------------------
 * 검색(Search) 프리셋 저장 요청에 대한 응답 DTO.
 *
 * 사용자가 프리셋을 생성하거나 저장했을 때,
 * 서버는 저장된 프리셋의 고유 ID(presetId)만 반환한다.
 *
 * 클라이언트에서는 이 ID를 활용해:
 *   - 저장 성공 여부 판단
 *   - 프리셋 상세 페이지 이동
 *   - 즐겨찾기 토글 등 후속 액션 수행
 *
 * 예시 응답:
 * {
 *   "presetId": 42
 * }
 * ==============================================================
 * AUTHOR        : 방대혁
 */
@Getter
@AllArgsConstructor
public class SaveSearchPresetResponseDTO {

    /** 생성된 프리셋의 고유 ID */
    private Integer presetId;
}
