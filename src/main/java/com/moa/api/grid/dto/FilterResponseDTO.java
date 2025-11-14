package com.moa.api.grid.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilterResponseDTO {
    private String field;
    private List<Object> values;
    private String error;       // 기존 유지

    private Long total;         // 전체 개수
    private Integer offset;     // 현재 offset
    private Integer limit;      // 요청 limit
    private Integer nextOffset; // 다음 페이지 offset (없으면 null)
    private Boolean hasMore;    // 다음 페이지 존재 여부
}