/**
 * 작성자: 정소영
 * 설명: 브라우저별 평균 페이지 로드 시간, 응답 시간, 요청 수를 담는 대시보드 응답 DTO
 */
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