package com.moa.api.grid.dto;

/*****************************************************************************
 CLASS NAME    : SearchResponseDTO
 DESCRIPTION   : 그리드 검색 응답 DTO
 AUTHOR        : 방대혁
 ******************************************************************************/

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
    private Integer total;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ColumnDTO {
        private String name;   // 컬럼명
        private String type;   // 컬럼 타입 (string, number, date, ip, mac 등)
        private String labelKo;
    }
}
