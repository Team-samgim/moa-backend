package com.moa.api.dashboard.dto.response;

// ============================================
// 위젯 5: Top 10 도메인
// ============================================

import java.sql.Timestamp;

public record TopDomainResponseDTO(
        Timestamp timestamp,
        String httpUri,              // URI
        Double avgResponseTime,      // 평균 응답시간 (ms)
        Long requestCount            // 요청 수
) {}