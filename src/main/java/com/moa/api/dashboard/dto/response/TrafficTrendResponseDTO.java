/**
 * 작성자: 정소영
 * 설명: 실시간 트래픽 추이(Trend) 위젯 데이터를 담는 응답 DTO
 *      (타임스탬프, 요청/응답 Mbps, 요청 수, 응답 수 포함)
 */
package com.moa.api.dashboard.dto.response;

// ============================================
// 위젯 1: 실시간 트래픽 추이
// ============================================

import java.sql.Timestamp;

public record TrafficTrendResponseDTO(
        Timestamp timestamp,
        Double mbpsReq,
        Double mbpsRes,
        Long requestCount,
        Long responseCount
) {}