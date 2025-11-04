package com.moa.api.pivot.repository;

import com.moa.api.pivot.dto.DistinctValuesPageDTO;
import com.moa.api.pivot.dto.DistinctValuesRequestDTO;
import com.moa.api.pivot.dto.PivotQueryRequestDTO;
import com.moa.api.pivot.dto.PivotQueryResponseDTO;
import com.moa.api.pivot.model.TimeWindow;

import java.util.List;

public interface PivotRepository {

    List<String> findColumnNamesForLayer(String layer);

    DistinctValuesPageDTO pageDistinctValues(DistinctValuesRequestDTO req, TimeWindow tw);

    List<String> findTopColumnValues(
            String layer,
            String columnField,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw
    );

    List<PivotQueryResponseDTO.RowGroup> buildRowGroups(
            String layer,
            List<PivotQueryRequestDTO.RowDef> rows,
            List<PivotQueryRequestDTO.ValueDef> values,
            String columnField,
            List<String> columnValues,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw
    );
}
