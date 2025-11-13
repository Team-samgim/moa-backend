package com.moa.api.pivot.repository;

import com.moa.api.pivot.dto.*;
import com.moa.api.pivot.model.PivotFieldMeta;
import com.moa.api.pivot.model.TimeWindow;

import java.util.List;

public interface PivotRepository {

    List<PivotFieldMeta> findFieldMetaForLayer(String layer);

    DistinctValuesPageDTO pageDistinctValues(DistinctValuesRequestDTO req, TimeWindow tw);

    List<String> findTopColumnValues(
            String layer,
            String columnField,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw
    );

    /** 화면 상단 row group 리스트 + summary만 (subRows X) */
    List<PivotQueryResponseDTO.RowGroup> buildRowGroups(
            String layer,
            List<PivotQueryRequestDTO.RowDef> rows,
            List<PivotQueryRequestDTO.ValueDef> values,
            String columnField,
            List<String> columnValues,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw
    );

    /** 특정 rowField에 대한 subRows + breakdown만 */
    List<PivotQueryResponseDTO.RowGroupItem> buildRowGroupItems(
            String layer,
            String rowField,
            List<PivotQueryRequestDTO.ValueDef> values,
            String columnField,
            List<String> columnValues,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw,
            int offset,
            int limit,
            PivotQueryRequestDTO.SortDef sort
    );

    List<String> findTopNDimensionValues(
            String layer,
            String field,   // dimension (row/column)
            PivotQueryRequestDTO.TopNDef topN,
            PivotQueryRequestDTO.ValueDef metric,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw
    );

    PivotChartResponseDTO getChart(PivotChartRequestDTO req, TimeWindow tw);

    PivotHeatmapTableResponseDTO getHeatmapTable(PivotHeatmapTableRequestDTO req, TimeWindow tw);
}
