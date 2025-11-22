package com.moa.api.dashboard.dto.response;

// ============================================
// 위젯 1: 실시간 트래픽 추이
// ============================================

import java.sql.Timestamp;

public record TrafficTrendResponseDTO(
        Timestamp timestamp,
        Double mbpsReq,
        Double mbpsRes,
        Long requestCount,
        Long responseCount
) {}