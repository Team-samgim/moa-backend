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
    @JsonProperty("xField")
    private String xField;

    @JsonProperty("yField")
    private String yField;

    @JsonProperty("xCategories")
    private List<String> xCategories;

    @JsonProperty("yCategories")
    private List<String> yCategories;

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
