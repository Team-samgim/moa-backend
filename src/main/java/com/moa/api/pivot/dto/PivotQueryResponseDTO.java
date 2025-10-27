package com.moa.api.pivot.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class PivotQueryResponseDTO {

    private ColumnField columnField;
    private List<RowGroup> rowGroups;
    private Summary summary;

    @Data
    @Builder
    public static class ColumnField {
        private String name;
        private List<String> values;
        private List<Metric> metrics;       // ex: [{alias:"합계: total", field:"total", agg:"sum"}, ...]
    }

    @Data
    @Builder
    public static class Metric {
        private String alias;   // "합계: total"
        private String field;   // "total"
        private String agg;     // "sum"
    }

    @Data
    @Builder
    public static class RowGroupItem {
        private String valueLabel;
        private String displayLabel;
        private Map<String, Map<String, Object>> cells;
    }

    @Data
    @Builder
    public static class RowGroup {
        private String rowLabel;
        private String displayLabel;
        private RowInfo rowInfo;
        private Map<String, Map<String, Object>> cells;
        private List<RowGroupItem> items;
    }

    @Data
    @Builder
    public static class RowInfo {
        private Integer count; // 12
    }

    @Data
    @Builder
    public static class Summary {
        private String rowCountText; // "합계: 35행"
    }
}
