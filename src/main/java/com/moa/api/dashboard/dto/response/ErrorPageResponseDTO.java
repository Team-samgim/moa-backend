/****
 * 작성자: 정소영
 * 설명: 에러 페이지 위젯 데이터를 담는 응답 DTO
 *      (타임스탬프, URI, 상태코드, 에러 횟수, 평균 응답시간, 심각도 포함)
 */
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