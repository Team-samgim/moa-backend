package com.moa.api.dashboard.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TopDomainRequestDTO {
    private String layer;

    @JsonProperty("timePreset")
    private String timePreset; // "1H" | "24H" | "7D"

    private DashboardRequestDTO.EpochRange range;

    private Integer step;

    /**
     * 조회할 도메인 개수 (기본: 10, 최대: 100)
     */
    @JsonProperty("limit")
    private Integer limit;

    /**
     * 정렬 기준 (선택사항)
     * 예: "request_count", "traffic_volume"
     */
    @JsonProperty("sortBy")
    private String sortBy;
}
