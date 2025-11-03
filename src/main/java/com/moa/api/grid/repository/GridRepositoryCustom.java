package com.moa.api.grid.repository;

import com.moa.api.grid.dto.DistinctPage;

import java.util.List;
import java.util.Map;

public interface GridRepositoryCustom {

    /**
     * 메인 그리드 데이터 조회 (필터/정렬/페이징 포함)
     */
    List<Map<String, Object>> getGridData(
            String layer,
            String sortField,
            String sortDirection,
            String filterModel,
            int offset,
            int limit
    );

    /**
     * DISTINCT 필터용 값 조회 (다른 필터 반영 포함)
     */
    DistinctPage getDistinctValuesPaged(
            String layer,
            String column,
            String filterModel,
            boolean includeSelf,
            String search,
            int offset,
            int limit
    );
}
