package com.moa.api.pivot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DistinctValuesPageDTO {

    private List<String> items;   // ["KR","US","(empty)", ...]
    private String nextCursor;    // null이면 더 없음
    private boolean hasMore;      // 다음 페이지 존재 여부
}
