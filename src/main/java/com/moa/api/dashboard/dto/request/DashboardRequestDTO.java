/**
 * 작성자: 정소영
 * 설명: 대시보드 조회 요청 정보(시간 프리셋, 절대 범위, 스텝, 필터 조건)를 담는 DTO
 */
package com.moa.api.dashboard.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;


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

    @JsonProperty("filters")
    @Schema(description = "필터 조건", implementation = DashboardFilters.class)
    private DashboardFilters filters;

    /**
     * 작성자: 정소영
     * 설명: 대시보드 조회 시 국가, 브라우저, 디바이스, URI 등 다양한 조건을 전달하는 필터 DTO
     */
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

    @Getter
    @Setter
    @Schema(name = "DashboardFilters", description = "대시보드 필터 조건")
    public static class DashboardFilters {

        // 사용자 기준 필터
        @Schema(description = "국가 목록 (다중 선택)", example = "[\"Korea\", \"USA\"]")
        private List<String> countries;

        @Schema(description = "브라우저 목록 (다중 선택)", example = "[\"Chrome\", \"Safari\"]")
        private List<String> browsers;

        @Schema(description = "디바이스 타입 목록 (다중 선택)", example = "[\"Desktop\", \"Mobile\"]")
        private List<String> devices;

        // 페이지 기준 필터
        @Schema(description = "HTTP 호스트 (도메인)", example = "example.com")
        private String httpHost;

        @Schema(description = "HTTP URI 패턴", example = "/api")
        private String httpUri;

        @Schema(description = "HTTP Method 목록", example = "[\"GET\", \"POST\"]")
        private List<String> httpMethods;

        // 성능 기준 필터
        @Schema(description = "HTTP 응답 코드 필터", implementation = NumericFilter.class)
        private NumericFilter httpResCode;

        @Schema(description = "응답 시간 필터 (ms)", implementation = NumericFilter.class)
        private NumericFilter responseTime;

        @Schema(description = "페이지 로드 시간 필터 (ms)", implementation = NumericFilter.class)
        private NumericFilter pageLoadTime;
    }

    @Getter
    @Setter
    @Schema(name = "NumericFilter", description = "숫자 범위 필터")
    public static class NumericFilter {
        @Schema(description = "연산자", example = ">=", allowableValues = {"=", ">", ">=", "<", "<="})
        private String operator; // "=", ">", ">=", "<", "<="

        @Schema(description = "값", example = "400")
        private Double value;
    }
}