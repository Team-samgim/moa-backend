package com.moa.api.grid.dto;

/*****************************************************************************
 CLASS NAME    : DistinctPageDTO
 DESCRIPTION   : DISTINCT 값 페이지 응답 DTO
 AUTHOR        : 방대혁
 ******************************************************************************/

import java.util.List;

/**
 * DISTINCT 값 페이지 DTO
 * values     : 결과 값 리스트
 * total      : 전체 건수
 * offset     : 현재 오프셋
 * limit      : 페이지 크기
 * nextOffset : 다음 페이지 오프셋 (없으면 null)
 */
public record DistinctPageDTO(
        List<String> values,
        long total,
        int offset,
        int limit,
        Integer nextOffset
) {}
