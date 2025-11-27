package com.moa.api.export.dto;

/*****************************************************************************
 CLASS NAME    : CsvPreviewDTO
 DESCRIPTION   : CSV 미리보기 응답 DTO (컬럼 리스트와 행 데이터 맵)
 AUTHOR        : 방대혁
 ******************************************************************************/

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * CSV 미리보기 DTO
 * - columns: CSV 컬럼 이름 목록
 * - rows   : 각 행을 컬럼명 → 문자열 값으로 매핑한 리스트
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CsvPreviewDTO {

    /** CSV 컬럼 이름 목록 */
    private List<String> columns;

    /** 행 데이터 (컬럼명 → 값) 리스트 */
    private List<Map<String, String>> rows;
}
