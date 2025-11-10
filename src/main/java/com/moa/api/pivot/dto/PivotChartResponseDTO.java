package com.moa.api.pivot.dto;

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
    // X축에 찍힐 label들
    private String xField;            // 예: "country_name_res"
    private List<String> categories;  // 예: ["KR", "US", "JP", ...]

    // Y축 시리즈들
    private List<SeriesDef> series;   // 1개 이상

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SeriesDef {
        private String id;            // "page_http_len_res::sum" 같은 unique id
        private String label;         // 범례에 쓸 이름 (alias) ex) "합계: page_http_len_res"
        private String field;         // pivot value field
        private String agg;           // "sum" | "avg" | "count" ...
        private List<Double> data;    // categories 순서와 동일한 길이
    }

    public static PivotChartResponseDTO empty(String xField) {
        return PivotChartResponseDTO.builder()
                .xField(xField)
                .categories(Collections.emptyList())
                .series(Collections.emptyList())
                .build();
    }
}
