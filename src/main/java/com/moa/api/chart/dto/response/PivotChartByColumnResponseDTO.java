// 작성자: 최이서
package com.moa.api.chart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PivotChartByColumnResponseDTO {

    private String colField;     // 예: "http_host"
    private String rowField;     // 예: "status_code"

    private String metricField;  // 예: "page_http_len_res"
    private String metricAgg;    // 예: "sum"
    private String metricLabel;  // 예: "합계: page_http_len_res"

    private List<ColumnChart> charts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ColumnChart {
        private String colKey;               // 예: "a.example.com"
        private List<String> rowCategories;  // 이 컬럼에서의 row TOP-N
        private List<Double> values;        // rowCategories와 인덱스 맞춰서 metric 값
    }
}
