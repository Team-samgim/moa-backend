// 작성자: 최이서
package com.moa.api.pivot.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class DistinctValuesRequestDTO {

    private String layer;     // ex) "HTTP_PAGE"
    private String field;     // ex) "country_name_res"

    private PivotQueryRequestDTO.TimeRange timeRange;   // { type, value, now }
    private PivotQueryRequestDTO.CustomRange customRange; // { from, to }
    private PivotQueryRequestDTO.TimeDef time;

    private List<PivotQueryRequestDTO.FilterDef> filters;

    private String order;     // "asc" | "desc" (기본 asc)
    private String keyword;   // 검색어 (optional)
    private String cursor;    // 무한스크롤 커서 (optional)
    private Integer limit;
}
