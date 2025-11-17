package com.moa.api.dashboard.dto.response;

public record DevicePerfResponseDTO(
        String deviceType,           // "Desktop", "Mobile", "Tablet"
        Long requestCount,           // 요청 수
        Double trafficPercentage,    // 트래픽 비율 (%)
        Double avgPageLoadTime,      // 평균 페이지 로드 시간 (ms)
        Double avgResponseTime,      // 평균 응답 시간 (ms)
        Long avgPageSize             // 평균 페이지 크기 (bytes)
) {}
