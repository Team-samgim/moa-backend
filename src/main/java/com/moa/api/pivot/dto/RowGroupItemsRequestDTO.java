package com.moa.api.pivot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
public class RowGroupItemsRequestDTO {

    private String layer;
    private String rowField; // ex: "dst_mac"

    private PivotQueryRequestDTO.TimeDef time;
    private PivotQueryRequestDTO.ColumnDef column;
    private List<PivotQueryRequestDTO.ValueDef> values;
    private List<PivotQueryRequestDTO.FilterDef> filters;

    private PivotQueryRequestDTO.SortDef sort;

    private String cursor;      // "offset:100" 또는 "lastValue:abc"
    private Integer limit = 50; // 기본 50개
}
