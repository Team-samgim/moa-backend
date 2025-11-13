package com.moa.api.dashboard.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrafficTrendRequestDTO {
    private String layer;

    @JsonProperty("timePreset")
    private String timePreset; // "1H" | "24H" | "7D"

    private DashboardRequestDTO.EpochRange range;

    private Integer step;
}