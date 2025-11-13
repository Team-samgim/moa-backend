package com.moa.api.dashboard.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "DashboardRequest")
public class DashboardRequestDTO {

    @JsonProperty("timePreset")
    @Schema(description = "시간 프리셋", example = "7D", allowableValues = {"1H","24H","7D"})
    private String timePreset; // "1H" | "24H" | "7D"

    @Schema(implementation = EpochRange.class, description = "절대시간 범위(유닉스 초)")
    private EpochRange range;

    @Schema(description = "집계 간격(초)", example = "300")
    private Integer step;

    @Getter
    @Setter
    @Schema(name = "DashboardEpochRange", description = "절대시간 범위(유닉스 초)")
    public static class EpochRange {
        @JsonProperty("fromEpoch")
        @Schema(description = "유닉스 시간, 시작(초)")
        private Long fromEpoch;

        @JsonProperty("toEpoch")
        @Schema(description = "유닉스 시간, 끝(초)")
        private Long toEpoch;
    }
}