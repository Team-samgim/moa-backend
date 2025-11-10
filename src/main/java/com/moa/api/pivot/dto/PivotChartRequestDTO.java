package com.moa.api.pivot.dto;

import lombok.Data;

import java.util.List;

@Data
public class PivotChartRequestDTO {

    private String layer;                   // ex) "HTTP_PAGE"
    private PivotQueryRequestDTO.TimeDef time;       // buildTimePayload 결과
    private List<PivotQueryRequestDTO.FilterDef> filters;

    private XDef x;
    private YDef y;

    private String chartType; // "bar" | "line" | "area" | "pie"

    @Data
    public static class XDef {
        private String field;               // X축 필드 (column 또는 row 중 하나)
        private String mode;                // "topN" | "manual"
        private TopNDef topN;               // topN 모드일 때
        private List<String> selectedItems; // manual 모드일 때
    }

    @Data
    public static class YDef {
        private List<PivotQueryRequestDTO.ValueDef> metrics; // field + agg
    }

    @Data
    public static class TopNDef {
        private Integer n;
    }
}