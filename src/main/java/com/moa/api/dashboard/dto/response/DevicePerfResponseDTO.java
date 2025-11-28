/**
 * 작성자: 정소영
 * 설명: 디바이스 타입별 트래픽/응답성능 데이터를 담는 대시보드 응답 DTO
 *      (타임스탬프, 디바이스 유형, 요청 수, 트래픽 비율, 평균 로드/응답 시간, 평균 페이지 크기 포함)
 */
package com.moa.api.dashboard.dto.response;

import java.sql.Timestamp;

public record DevicePerfResponseDTO(
        Timestamp timestamp,
        String deviceType,           // "Desktop", "Mobile", "Tablet"
        Long requestCount,           // 요청 수
        Double trafficPercentage,    // 트래픽 비율 (%)
        Double avgPageLoadTime,      // 평균 페이지 로드 시간 (ms)
        Double avgResponseTime,      // 평균 응답 시간 (ms)
        Long avgPageSize             // 평균 페이지 크기 (bytes)
) {}
