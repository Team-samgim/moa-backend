package com.moa.api.dashboard.dto.response;

// ============================================
// 위젯 4: HTTP 상태코드 분포
// ============================================

import java.sql.Timestamp;

public record StatusCodeResponseDTO(
        Timestamp timestamp,
        String statusGroup,    // "2xx", "3xx", "4xx", "5xx"
        Long count,
        Double percentage
) {}