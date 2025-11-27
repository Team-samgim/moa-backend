package com.moa.api.preset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.util.List;

/**
 * ==============================================================
 * PresetListResponseDTO
 * --------------------------------------------------------------
 * 마이페이지에서 Preset 목록을 페이징 형태로 반환하기 위한 DTO.
 *
 * 포함 정보:
 *   - items       : PresetItemDTO 목록
 *   - page        : 현재 페이지 번호 (0부터 시작)
 *   - size        : 페이지 크기
 *   - totalPages  : 전체 페이지 수
 *   - totalItems  : 전체 Preset 개수
 *
 * 특징:
 *   - items는 단일 Preset 정보를 담은 PresetItemDTO 리스트
 *   - 검색/필터 조건(type, origin 등)은 Controller·Service에서 처리하고
 *     최종 결과만 이 DTO로 포장하여 응답
 *   - Lombok Builder 지원으로 서비스단에서 유연하게 생성 가능
 * ==============================================================
 * AUTHOR        : 방대혁
 */
@Getter
@Builder
@AllArgsConstructor
public class PresetListResponseDTO {

    /** PresetItemDTO 리스트 */
    private final List<PresetItemDTO> items;

    /** 현재 페이지 번호 (0-based index) */
    private final int page;

    /** 페이지당 아이템 개수 */
    private final int size;

    /** 전체 페이지 수 */
    private final int totalPages;

    /** 전체 Preset 개수 */
    private final long totalItems;
}
