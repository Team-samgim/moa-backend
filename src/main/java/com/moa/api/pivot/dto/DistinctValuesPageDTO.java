package com.moa.api.pivot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DistinctValuesPageDTO {

    private List<String> items;
    private String nextCursor;
    private boolean hasMore;
    private Integer totalCount;
}
