package com.moa.api.grid.repository;

/*****************************************************************************
 CLASS NAME    : GridRepositoryCustom
 DESCRIPTION   : Grid 데이터 DISTINCT 조회, 컬럼 메타, 집계 쿼리용 커스텀 Repository 인터페이스
 AUTHOR        : 방대혁
 ******************************************************************************/
/**
 * Grid Repository Custom
 */

import com.moa.api.grid.dto.*;

import java.util.List;
import java.util.Map;

public interface GridRepositoryCustom {

    /** DISTINCT 필터용 값 조회 (다른 필터 반영 포함) */
    DistinctPageDTO getDistinctValuesPaged(
            String layer,
            String column,
            String filterModel,
            boolean includeSelf,
            String search,
            int offset,
            int limit,
            String orderBy,
            String order,
            String baseSpecJson
    );

    /** 컬럼 메타 조회 */
    List<SearchResponseDTO.ColumnDTO> getColumnsWithType(String layer);

    /** 프론트 타입 맵(string/number/date/...) */
    Map<String, String> getFrontendTypeMap(String layer);

    /** 시계열 원시 타입 맵(timestamptz/timestamp/date/...) */
    Map<String, String> getTemporalKindMap(String layer);

    /** 집계 */
    AggregateResponseDTO aggregate(AggregateRequestDTO req);
}
