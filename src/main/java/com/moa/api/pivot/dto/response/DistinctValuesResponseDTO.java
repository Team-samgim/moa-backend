package com.moa.api.pivot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DistinctValuesResponseDTO {

    private List<String> items;
    private String nextCursor;
    private boolean hasMore;
    private Integer totalCount;
}
