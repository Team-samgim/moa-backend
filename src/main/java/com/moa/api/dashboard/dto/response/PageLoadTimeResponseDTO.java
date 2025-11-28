/****
 * 작성자: 정소영
 * 설명: 페이지 로드 시간 위젯 데이터를 담는 응답 DTO
 *      (타임스탬프, 평균/최소/최대 로드 시간, P95, P99 포함)
 */
package com.moa.api.dashboard.dto.response;

import java.sql.Timestamp;

public record PageLoadTimeResponseDTO(
        Timestamp timestamp,        // 시간
        Double avgPageLoadTime,     // 평균 페이지 로드 시간 (ms)
        Double minPageLoadTime,
        Double maxPageLoadTime,
        Double p95PageLoadTime,     // P95 (ms)
        Double p99PageLoadTime      // P99 (ms)
) {}