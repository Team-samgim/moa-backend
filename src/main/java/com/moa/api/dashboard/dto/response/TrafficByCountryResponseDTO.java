package com.moa.api.dashboard.dto.response;

// ============================================
// 위젯 3: 지역별 트래픽 분포
// ============================================

public record TrafficByCountryResponseDTO(
        String countryName,
        Long trafficVolume,
        Long requestCount,
        Double percentage
) {}
