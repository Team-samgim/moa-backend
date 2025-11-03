package com.moa.api.pivot.dto;

import lombok.Data;

import java.util.List;

@Data
public class DistinctValuesRequestDTO {

    private String layer;     // ex) "HTTP_PAGE"
    private String field;     // ex) "country_name_res"

    // 기존 PivotQueryRequestDTO에 있는 구조를 재사용한다고 가정
    private PivotQueryRequestDTO.TimeRange timeRange;   // { type, value, now }
    private PivotQueryRequestDTO.CustomRange customRange; // { from, to }

    private List<PivotQueryRequestDTO.FilterDef> filters;

    private String order;     // "asc" | "desc" (기본 asc)
    private String keyword;   // 검색어 (optional)
    private String cursor;    // 무한스크롤 커서 (optional)
    private Integer limit;    // 한 번에 몇 개? (ex. 50)
}
