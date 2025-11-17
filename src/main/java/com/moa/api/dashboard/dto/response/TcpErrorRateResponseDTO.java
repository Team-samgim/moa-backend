package com.moa.api.dashboard.dto.response;

// ============================================
// 위젯 2: TCP 에러율
// ============================================

public record TcpErrorRateResponseDTO(
        Double totalErrorRate,
        Double retransmissionRate,
        Double outOfOrderRate,
        Double lostSegmentRate,
        Long totalErrors,
        Long totalPackets
) {}