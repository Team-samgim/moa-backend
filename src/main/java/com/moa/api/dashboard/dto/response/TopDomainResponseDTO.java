/**
 * 작성자: 정소영
 * 설명: Top 10 도메인 위젯 데이터를 담는 응답 DTO
 *      (타임스탬프, URI, 평균 응답시간, 요청 수 포함)
 */
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