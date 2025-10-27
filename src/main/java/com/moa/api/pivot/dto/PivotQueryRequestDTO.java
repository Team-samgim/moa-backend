package com.moa.api.pivot.dto;

import lombok.Data;
import java.util.List;

@Data
public class PivotQueryRequestDTO {

    private String layer;

    private TimeRange timeRange;
    private CustomRange customRange;

    private ColumnDef column;
    private List<RowDef> rows;
    private List<ValueDef> values;

    private List<FilterDef> filters;

    @Data
    public static class TimeRange {
        private String type;   // "preset" | "custom"
        private String value;  // "1h" | "2h" | "24h" | "1w"
        private String now;    // ISO string
    }

    @Data
    public static class CustomRange {
        private String from;   // ISO string
        private String to;     // ISO string
    }

    @Data
    public static class ColumnDef {
        private String field; // ex: "src_ip"
    }

    @Data
    public static class RowDef {
        private String field; // ex: "vlan_id", "country_name_res", ...
    }

    @Data
    public static class ValueDef {
        private String field;  // ex: "total"
        private String agg;    // ex: "sum", "avg"
        private String alias;  // ex: "합계: total"
    }

    @Data
    public static class FilterDef {
        private String field;  // ex: "country_name_res"
        private String op;     // ex: "=", "IN", "LIKE", ...
        private Object value;  // ex: "KR" or [1,12,21,...]
    }
}
