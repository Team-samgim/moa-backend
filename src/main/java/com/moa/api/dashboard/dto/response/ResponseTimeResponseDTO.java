package com.moa.api.dashboard.dto.response;

// ============================================
// 위젯 6: 응답시간 통계
// ============================================

public record ResponseTimeResponseDTO(
        Double minResponseTime,
        Double avgResponseTime,
        Double maxResponseTime,
        Double p50ResponseTime,   // median
        Double p95ResponseTime,
        Double p99ResponseTime,
        Long totalRequests
) {}
