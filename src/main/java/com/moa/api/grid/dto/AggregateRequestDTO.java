package com.moa.api.grid.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Map;

@Data
public class AggregateRequestDTO {

    /**
     * 레이어 (기본 ethernet)
     */
    @NotBlank(message = "레이어는 필수입니다")
    @Pattern(
            regexp = "^(http_page|http_uri|l4_tcp|ethernet|HTTP_PAGE|HTTP_URI|L4_TCP|ETHERNET)$",
            message = "지원하지 않는 레이어입니다"
    )
    private String layer = "ethernet";

    /**
     * 필드별 집계 설정
     * key: 필드명
     * value: 집계 스펙 (type, ops)
     */
    @NotNull(message = "metrics는 필수입니다")
    @Size(min = 1, message = "최소 1개 이상의 필드가 필요합니다")
    private Map<String, MetricSpec> metrics;

    /**
     * 활성 필터(JSON 문자열)
     */
    @Size(max = 10000, message = "필터 모델은 10000자를 초과할 수 없습니다")
    private String filterModel;

    /**
     * SearchDTO 형태의 baseSpec JSON
     */
    @Size(max = 10000, message = "baseSpec은 10000자를 초과할 수 없습니다")
    private String baseSpecJson;

    /**
     * 집계 스펙
     */
    @Data
    public static class MetricSpec {
        /**
         * 데이터 타입 (number, string, date 등)
         */
        @Pattern(
                regexp = "^(number|string|date|text|json|ip|mac)$",
                message = "지원하지 않는 데이터 타입입니다"
        )
        private String type;

        /**
         * 집계 연산자 목록
         * number: count, sum, avg, min, max
         * string: count, distinct, top1, top2, top3
         */
        @NotNull(message = "집계 연산자는 필수입니다")
        @Size(min = 1, message = "최소 1개 이상의 연산자가 필요합니다")
        private java.util.List<String> ops;
    }
}