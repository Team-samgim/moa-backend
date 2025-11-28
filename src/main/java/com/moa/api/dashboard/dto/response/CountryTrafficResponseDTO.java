/**
 * 작성자: 정소영
 * 설명: 국가별 트래픽 분포 위젯 응답 DTO
 *      (타임스탬프, 국가명, 평균 응답시간, 요청 수 포함)
 */
package com.moa.api.dashboard.dto.response;

// ============================================
// 위젯 3: 지역별 트래픽 분포
// ============================================

import java.sql.Timestamp;

public record CountryTrafficResponseDTO(
        Timestamp timestamp,
        String country,              // 국가명
        Double avgResponseTime,      // 평균 응답시간 (ms)
        Long requestCount            // 요청 수

) {}
