/****
 * 작성자: 정소영
 * 설명: 응답시간 통계 위젯 데이터를 담는 응답 DTO
 *      (타임스탬프, 평균 응답시간, P95, P99 포함)
 */
package com.moa.api.dashboard.dto.response;

// ============================================
// 위젯 6: 응답시간 통계
// ============================================

import java.sql.Timestamp;

public record ResponseTimeResponseDTO(
        Timestamp timestamp,
        Double avgResponseTime,
        Double p95ResponseTime,
        Double p99ResponseTime
) {}
