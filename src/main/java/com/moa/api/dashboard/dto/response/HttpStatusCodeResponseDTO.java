package com.moa.api.dashboard.dto.response;

// ============================================
// 위젯 4: HTTP 상태코드 분포
// ============================================

public record HttpStatusCodeResponseDTO(
        Long successCount,        // 2xx
        Long redirectCount,       // 3xx
        Long clientErrorCount,    // 4xx
        Long serverErrorCount,    // 5xx
        Long totalCount
) {}