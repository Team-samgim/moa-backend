package com.moa.api.grid.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class SearchResponseDTO {
    private String layer;
    private List<String> columns;
    private List<Map<String, Object>> rows;
}