package com.moa.api.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardRequestDTO {
    private String layer;

    @JsonProperty("timePreset")
    private String timePreset; // "1H" | "24H" | "7D"

    private TimeRange range;

    private Integer step;

    @Getter
    @Setter
    public static class TimeRange {
        @JsonProperty("fromEpoch")
        private Long fromEpoch;

        @JsonProperty("toEpoch")
        private Long toEpoch;
    }
}