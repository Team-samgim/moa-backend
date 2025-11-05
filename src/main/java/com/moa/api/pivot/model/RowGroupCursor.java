package com.moa.api.pivot.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RowGroupCursor {
    private Double sortMetric;      // 정렬에 사용한 metric 값 (null이면 default 정렬)
    private List<String> rowKeys;   // row group key들 (예: [dst_mac, ts_server...])
}
