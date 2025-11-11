package com.moa.api.dashboard.dto;

// ============================================
// 위젯 3: 지역별 트래픽 분포
// ============================================

public record TrafficByCountryDTO(
        String countryName,
        Long trafficVolume,
        Long requestCount,
        Double percentage
) {}
