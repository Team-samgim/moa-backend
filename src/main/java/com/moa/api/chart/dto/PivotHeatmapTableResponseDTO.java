package com.moa.api.chart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PivotHeatmapTableResponseDTO {

    @JsonProperty("xField")
    private String xField;          // colField

    @JsonProperty("yField")
    private String yField;          // rowField

    @JsonProperty("xCategories")
    private List<String> xCategories; // 컬럼 헤더
    private List<RowDef> rows;        // 행 + 셀 값

    private long totalRowCount;     // 전체 Y 카테고리 개수
    private int offset;             // 이번 페이지 offset
    private int limit;              // limit

    private Double pageMin;         // 이번 페이지 내 셀 최소값
    private Double pageMax;         // 이번 페이지 내 셀 최대값

    // 필요하면 globalMin/globalMax도 추가 가능
    // private Double globalMin;
    // private Double globalMax;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RowDef {
        @JsonProperty("yCategory")
        private String yCategory;     // Y 축 하나의 값
        private List<Double> cells;   // xCategories와 같은 길이
        private Double rowTotal;      // 선택 (합계 등)
    }

    public static PivotHeatmapTableResponseDTO empty(String xField, String yField) {
        return PivotHeatmapTableResponseDTO.builder()
                .xField(xField)
                .yField(yField)
                .xCategories(Collections.emptyList())
                .rows(Collections.emptyList())
                .totalRowCount(0)
                .offset(0)
                .limit(0)
                .pageMin(null)
                .pageMax(null)
                .build();
    }
}