package com.moa.api.dashboard.dto;

// ============================================
// 위젯 6: 응답시간 통계
// ============================================

public record ResponseTimeDTO(
        Double minResponseTime,
        Double avgResponseTime,
        Double maxResponseTime,
        Double p50ResponseTime,   // median
        Double p95ResponseTime,
        Double p99ResponseTime,
        Long totalRequests
) {}
