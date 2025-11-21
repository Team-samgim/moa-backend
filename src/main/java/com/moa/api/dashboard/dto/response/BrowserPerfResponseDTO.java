package com.moa.api.dashboard.dto.response;

import lombok.Builder;

import java.sql.Timestamp;

public record BrowserPerfResponseDTO(
        Timestamp timestamp,
        String browser,              // "Chrome", "Safari", "Firefox"
        Double avgPageLoadTime,      // 평균 페이지 로드 시간 (ms)
        Double avgResponseTime,      // 평균 응답 시간 (ms)
        Long requestCount            // 요청 수
) {}