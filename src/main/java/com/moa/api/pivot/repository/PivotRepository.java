package com.moa.api.pivot.repository;

import com.moa.api.pivot.dto.request.DistinctValuesRequestDTO;
import com.moa.api.pivot.dto.request.PivotQueryRequestDTO;
import com.moa.api.pivot.dto.response.DistinctValuesResponseDTO;
import com.moa.api.pivot.dto.response.PivotQueryResponseDTO;
import com.moa.api.pivot.model.PivotFieldMeta;
import com.moa.api.pivot.model.PivotLayer;
import com.moa.api.pivot.model.PivotQueryContext;
import com.moa.api.pivot.model.TimeWindow;

import java.util.List;

public interface PivotRepository {

    List<PivotFieldMeta> findFieldMetaForLayer(PivotLayer layer);

    DistinctValuesResponseDTO pageDistinctValues(DistinctValuesRequestDTO req, TimeWindow tw);

    List<String> findTopColumnValues(
            PivotQueryContext ctx,
            String columnField
    );

    List<PivotQueryResponseDTO.RowGroup> buildRowGroups(
            PivotQueryContext ctx,
            List<PivotQueryRequestDTO.RowDef> rows,
            List<PivotQueryRequestDTO.ValueDef> values,
            String columnField,
            List<String> columnValues
    );

    List<PivotQueryResponseDTO.RowGroupItem> buildRowGroupItems(
            PivotQueryContext ctx,
            String rowField,
            List<PivotQueryRequestDTO.ValueDef> values,
            String columnField,
            List<String> columnValues,
            int offset,
            int limit,
            PivotQueryRequestDTO.SortDef sort
    );

    // 이건 아직 ctx 안 쓰고 있어도 OK — 공통 Top-N 유틸 느낌이면 그대로 둬도 됨
    List<String> findTopNDimensionValues(
            PivotLayer layer,
            String field,
            PivotQueryRequestDTO.TopNDef topN,
            PivotQueryRequestDTO.ValueDef metric,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw
    );
}
