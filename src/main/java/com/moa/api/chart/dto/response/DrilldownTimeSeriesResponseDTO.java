// 작성자: 최이서
package com.moa.api.chart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrilldownTimeSeriesResponseDTO {

    private String timeField;
    private String metricField;
    private String metricAgg;

    private Long globalMinTime;
    private Long globalMaxTime;

    private Double globalMedian;                  // 전체 값 기준 median
    private Map<String, Double> seriesMedianMap;  // rowKey별 median

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Point {
        private Long ts;      // ms epoch
        private Double value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SeriesDef {
        private String rowKey;
        private List<Point> points;
        private Double median;
    }

    private List<SeriesDef> series;
}
