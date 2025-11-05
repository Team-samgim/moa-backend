package com.moa.api.grid.repository;

import com.moa.api.grid.dto.DistinctPage;

public interface GridRepositoryCustom {

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
            int limit,
            String orderBy,
            String order,
            String baseSpecJson
    );
}
