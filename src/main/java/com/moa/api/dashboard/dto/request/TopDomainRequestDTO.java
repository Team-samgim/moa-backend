/**
 * 작성자: 정소영
 * 설명: 대시보드 Top Domain 위젯 조회를 위한 요청 DTO (레이어, 시간 프리셋, 조회 범위, 스텝, 정렬 기준 포함)
 */
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
