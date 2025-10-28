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

    @Data @Builder
    public static class ColumnField {
        private String name;                 // 예: "src_port"
        private List<String> values;         // 예: ["ip_num_1", "ip_num_2", ...]
        private List<Metric> metrics;        // 선택된 values[]에서 넘어온 metric 정보
    }

    @Data @Builder
    public static class Metric {
        private String alias;   // "합계: page_http_len_req"
        private String field;   // "page_http_len_req"
        private String agg;     // "sum"
    }

    @Data @Builder
    public static class RowGroup {
        private String rowLabel;            // "vlan_id"
        private String displayLabel;        // "vlan_id (5)" ← 화면 그대로 찍을 텍스트
        private RowInfo rowInfo;            // {count:5}
        private Map<String, Map<String, Object>> cells;
        // 예: "ip_num_1" → { "합계: page_http_len_req": 103500, ... }
        private List<RowGroupItem> items;   // 하위 실제 값들
    }

    @Data @Builder
    public static class RowGroupItem {
        private String valueLabel;          // 실제 값 "1"
        private String displayLabel;        // 화면용 라벨 "1"
        private Map<String, Map<String, Object>> cells;
    }

    @Data @Builder
    public static class RowInfo {
        private Integer count;              // distinct row 값 개수
    }

    @Data @Builder
    public static class Summary {
        private String rowCountText;        // "합계: N행"
    }
}
