package com.moa.api.grid.dto;

/*****************************************************************************
 CLASS NAME    : AggregateResponseDTO
 DESCRIPTION   : Grid 집계 응답 DTO (필드별 집계 결과 맵)
 AUTHOR        : 방대혁
 ******************************************************************************/

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class AggregateResponseDTO {

    /**
     * 집계 결과 맵
     * key   : 필드명
     * value : { count: .., sum: .., top1: { value, count }, ... }
     */
    private Map<String, Object> aggregates;
}
