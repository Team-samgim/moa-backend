package com.moa.api.pivot.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class PivotHeatmapTableRequestDTO {

    private String layer;
    private PivotQueryRequestDTO.TimeDef time;
    private List<PivotQueryRequestDTO.FilterDef> filters;

    private String colField;  // X축 기준 필드 (예: country_name_res)
    private String rowField;  // Y축 기준 필드 (예: http_cookie)

    private PivotQueryRequestDTO.ValueDef metric; // { field, agg, alias }

    // 페이지네이션: 행 단위(offset/limit)
    private PageDef page;     // null이면 기본값 사용

    @Data
    public static class PageDef {
        private Integer offset; // 0 기반 행 offset
        private Integer limit;  // 페이지 크기 (예: 100)
    }
}