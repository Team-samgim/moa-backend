package com.moa.api.grid.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class AggregateRequestDTO {
    private String layer;                   // ex) "ethernet"
    private String filterModel;             // 프론트에서 보내던 JSON 문자열 그대로
    private String baseSpecJson;
    private Map<String, MetricSpec> metrics; // { field: { type, ops[] } }

    @Getter @Setter
    public static class MetricSpec {
        private String type;        // "number" | "string" | "ip" | "mac"
        private List<String> ops;   // number: ["count","sum","avg","min","max"]
        // string/ip/mac: ["count","distinct","top1"]
    }
}