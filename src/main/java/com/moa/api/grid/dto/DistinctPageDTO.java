package com.moa.api.grid.dto;

import java.util.List;

public record DistinctPageDTO(
        List<String> values,
        long total,
        int offset,
        int limit,
        Integer nextOffset
) {}
