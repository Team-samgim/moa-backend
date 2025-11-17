package com.moa.api.dashboard.dto.response;

// ============================================
// 위젯 3: 지역별 트래픽 분포
// ============================================

public record CountryTrafficResponseDTO(
        String country,              // 국가명
        Double avgResponseTime,      // 평균 응답시간 (ms)
        Long requestCount            // 요청 수
) {}
