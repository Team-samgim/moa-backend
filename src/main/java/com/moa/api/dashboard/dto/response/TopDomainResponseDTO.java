package com.moa.api.dashboard.dto.response;

// ============================================
// 위젯 5: Top 10 도메인
// ============================================

public record TopDomainResponseDTO(
        String domain,
        Long requestCount,
        Long trafficVolume,
        Double percentage
) {}