/**
 * 작성자: 정소영
 * 설명: 국가별 트래픽 위젯 조회를 위한 요청 DTO
 *      (레이어, 시간 프리셋, 조회 범위, 스텝, 국가 목록, 지도 타입 포함)
 */
package com.moa.api.dashboard.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TrafficByCountryRequestDTO {
    private String layer;

    @JsonProperty("timePreset")
    private String timePreset; // "1H" | "24H" | "7D"

    private DashboardRequestDTO.EpochRange range;

    private Integer step;

    /**
     * 특정 국가만 조회 (선택사항)
     * 예: ["KR", "US", "JP"]
     */
    @JsonProperty("countries")
    private List<String> countries;

    /**
     * 지도 타입 (선택사항)
     * 예: "heatmap", "bubble", "choropleth"
     */
    @JsonProperty("mapType")
    private String mapType;
}