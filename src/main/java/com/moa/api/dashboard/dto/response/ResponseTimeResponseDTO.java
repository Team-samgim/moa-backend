package com.moa.api.dashboard.dto.response;

// ============================================
// 위젯 6: 응답시간 통계
// ============================================

import java.sql.Timestamp;

public record ResponseTimeResponseDTO(
        Timestamp timestamp,
        Double avgResponseTime,
        Double p95ResponseTime,
        Double p99ResponseTime
) {}
