package com.moa.api.dashboard.dto;

// ============================================
// 위젯 5: Top 10 도메인
// ============================================

public record TopDomainDTO(
        String domain,
        Long requestCount,
        Long trafficVolume,
        Double percentage
) {}