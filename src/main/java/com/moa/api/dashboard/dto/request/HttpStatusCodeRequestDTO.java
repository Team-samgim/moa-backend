/**
 * 작성자: 정소영
 * 설명: 대시보드 HTTP 상태코드 위젯 조회를 위한 요청 DTO (레이어, 시간 프리셋, 조회 범위, 스텝 정보 포함)
 */
package com.moa.api.dashboard.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HttpStatusCodeRequestDTO {
    private String layer;

    @JsonProperty("timePreset")
    private String timePreset; // "1H" | "24H" | "7D"

    private DashboardRequestDTO.EpochRange range;

    private Integer step;
}
