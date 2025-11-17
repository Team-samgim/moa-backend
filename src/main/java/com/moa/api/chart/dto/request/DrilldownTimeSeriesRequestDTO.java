package com.moa.api.chart.dto.request;

import com.moa.api.pivot.dto.request.PivotQueryRequestDTO;
import lombok.Data;

import java.util.List;

@Data
public class DrilldownTimeSeriesRequestDTO {

    private String layer;

    private PivotQueryRequestDTO.TimeDef time;     // buildTimePayload 결과
    private List<PivotQueryRequestDTO.FilterDef> filters;

    private String colField;
    private String rowField;

    private String selectedColKey;    // 사용자가 클릭한 column 그룹
    private List<String> rowKeys;     // 드릴다운 시 포함할 row (보통 yCategories 전체)

    private String timeField;         // "ts_server_nesc"
    private PivotQueryRequestDTO.ValueDef metric;

    private String timeBucket;        // "RAW" | "MINUTE" ... (나중 확장용)
}
