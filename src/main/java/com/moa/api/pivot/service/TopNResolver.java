package com.moa.api.pivot.service;

import com.moa.api.pivot.dto.request.PivotQueryRequestDTO;
import com.moa.api.pivot.model.PivotLayer;
import com.moa.api.pivot.model.TimeWindow;

import java.util.List;

public interface TopNResolver {

    /**
     * Pivot 필터들 중에서 TopN이 활성화된 필터를 실제 IN 필터로 치환한 리스트를 반환
     */
    List<PivotQueryRequestDTO.FilterDef> resolveFilters(
            PivotLayer layer,
            List<PivotQueryRequestDTO.ValueDef> values,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw
    );
}
