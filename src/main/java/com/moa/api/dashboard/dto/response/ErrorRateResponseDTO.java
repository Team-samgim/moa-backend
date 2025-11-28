/**
 * 작성자: 정소영
 * 설명: 전체 요청 수 및 4xx/5xx 에러 비율을 포함한 에러율 위젯 응답 DTO
 */
package com.moa.api.dashboard.dto.response;

import java.sql.Timestamp;

public record ErrorRateResponseDTO(
        Timestamp timestamp,         // 시간
        Long totalCount,             // 전체 요청 수
        Long errorCount,             // 에러 수
        Long clientErrorCount,       // 4xx 에러 수
        Long serverErrorCount,       // 5xx 에러 수
        Double errorRate,            // 에러율 (%)
        Double clientErrorRate,      // 4xx 에러율 (%)
        Double serverErrorRate       // 5xx 에러율 (%)
) {}
