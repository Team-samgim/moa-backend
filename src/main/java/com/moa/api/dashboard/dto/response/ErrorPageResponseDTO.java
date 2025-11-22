package com.moa.api.dashboard.dto.response;

// ============================================
// 위젯 2: 에러 페이지
// ============================================

import java.sql.Timestamp;

public record ErrorPageResponseDTO(
        Timestamp timestamp,
        String httpUri,              // "/api/v1/users"
        Integer httpResCode,         // 500, 404, 401 등
        Long errorCount,             // 에러 발생 횟수
        Double avgResponseTime,      // 평균 응답시간 (ms)
        String severity              // "high", "medium", "low"
) {}