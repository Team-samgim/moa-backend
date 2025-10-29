package com.moa.api.grid.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchResponseDTO {
    private String layer;
    private List<ColumnDTO> columns;
    private List<Map<String, Object>> rows;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ColumnDTO {
        private String name;  // 컬럼명
        private String type;  // 컬럼 타입 (string, number, date, ip, mac 등)
    }
}