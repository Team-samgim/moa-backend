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