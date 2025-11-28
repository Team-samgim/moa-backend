// 작성자: 최이서
package com.moa.api.chart.dto.response;

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
public class PivotChartResponseDTO {
    // ===== 단일 차트 모드 필드 =====
    private String xField;
    private String yField;
    private List<String> xCategories;
    private List<String> yCategories;
    private List<SeriesDef> series;   // 단일 차트일 때 사용

    // ===== 다중 차트 모드 필드 =====
    private List<ChartData> charts;   // 다중 차트일 때 사용

    /**
     * 다중 차트 모드에서 각 Column 값에 대한 차트 데이터
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChartData {
        private String columnKey;      // Column 축의 특정 값 (예: "KR", "US", "JP")
        private String yField;         // Row 축 필드명
        private List<String> yCategories;  // 이 Column에 대한 Row 축 카테고리
        private List<SeriesDef> series;    // 메트릭 데이터
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SeriesDef {
        private String id;            // "page_http_len_res::sum" 같은 unique id
        private String label;         // 범례에 쓸 이름 (alias) ex) "합계: page_http_len_res"
        private String field;         // pivot value field
        private String agg;           // "sum" | "avg" | "count" ...
        private List<List<Double>> values;    // values[yIndex][xIndex] : (yCategories[yIndex], xCategories[xIndex])
    }

    public static PivotChartResponseDTO empty(String xField, String yField) {
        return PivotChartResponseDTO.builder()
                .xField(xField)
                .yField(yField)
                .xCategories(Collections.emptyList())
                .yCategories(Collections.emptyList())
                .series(Collections.emptyList())
                .build();
    }
}