package com.moa.api.pivot.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class PivotQueryRequestDTO {

    private String layer;

    private TimeRange timeRange;
    private CustomRange customRange;

    private ColumnDef column;
    private List<RowDef> rows;
    private List<ValueDef> values;

    private List<FilterDef> filters;

    private TimeDef time;
    private SortDef sort;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    @Data
    public static class TimeRange {
        private String type;   // "preset" | "custom"
        private String value;
        private String now;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    @Data
    public static class CustomRange {
        private String from;
        private String to;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    @Data
    public static class ColumnDef {
        private String field;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    @Data
    public static class RowDef {
        private String field;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    @Data
    public static class ValueDef {
        private String field;  // ex: "total"
        private String agg;    // ex: "sum", "avg"
        private String alias;  // ex: "합계: total"
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    @Data
    public static class FilterDef {
        private String field;  // ex: "country_name_res"
        private String op;     // ex: "=", "IN", "LIKE", ...
        private Object value;  // ex: "KR" or [1,12,21,...]
        private TopNDef topN;
        private String order;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    @Data
    public static class TopNDef {
        private Boolean enabled;   // true/false
        private Integer n;         // 예: 5
        private String mode;       // "top" | "bottom"
        private String valueKey;   // 프론트는 metric alias를 보냄 (예: "합계: page_http_len_res")
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    @Data
    public static class TimeDef {
        private String field;
        private Long fromEpoch;
        private Long toEpoch;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    @Data
    public static class SortDef {
        private String mode;        // "value" 정도로 시작
        private String columnValue; // 어떤 columnField.values 값인지 (예: "South Korea")
        private String valueField;  // ValueDef.field (ex: "page_http_len_res")
        private String agg;         // ValueDef.agg   (ex: "sum")
        private String direction;   // "asc" | "desc"
    }
}
