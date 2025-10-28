package com.moa.api.grid.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class DistinctValueResponseDTO {
    private String field;
    private List<Object> values;
    private String error;
}