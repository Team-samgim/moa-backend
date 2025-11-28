/**
 * 작성자: 정소영
 * 설명: HTTP 상태코드 분포 위젯 데이터를 담는 응답 DTO
 *      (타임스탬프, 상태코드 그룹, 개수, 비율 포함)
 */
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