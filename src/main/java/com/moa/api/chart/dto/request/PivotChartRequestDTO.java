package com.moa.api.chart.dto.request;

import com.moa.api.pivot.dto.request.PivotQueryRequestDTO;
import lombok.Data;

import java.util.List;

@Data
public class PivotChartRequestDTO {

    private String layer;                   // ex) "HTTP_PAGE"
    private PivotQueryRequestDTO.TimeDef time;       // buildTimePayload 결과
    private List<PivotQueryRequestDTO.FilterDef> filters;

    private AxisDef col;                          // Column 축 정의
    private AxisDef row;                          // Row 축 정의

    private PivotQueryRequestDTO.ValueDef metric;
    private String chartType; // "bar" | "line" | "area" | "pie" | "multiplePie"

    private LayoutConfig layout; // 다중 차트 레이아웃 설정

    @Data
    public static class AxisDef {
        private String field;               // 축으로 사용할 필드
        private String mode;                // "topN" | "manual"
        private TopNDef topN;               // topN 모드일 때
        private List<String> selectedItems; // manual 모드일 때
    }

    @Data
    public static class TopNDef {
        private Integer n;
    }

    @Data
    public static class LayoutConfig {
        private Integer chartsPerRow;  // 한 줄에 표시할 차트 개수 (2 or 3)
        // null이면 기존 방식(단일 차트)
        // 2이면 최대 4개 차트 (2x2)
        // 3이면 최대 6개 차트 (3x2)
    }
}