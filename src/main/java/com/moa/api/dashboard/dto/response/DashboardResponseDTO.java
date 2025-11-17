package com.moa.api.dashboard.dto.response;

import java.sql.Timestamp;
import java.util.List;

// ============================================
// 전체 대시보드 응답
// ============================================

public record DashboardResponseDTO(

        // === 기존 위젯 (6개) ===
        List<ResponseTimeResponseDTO> responseTimeStats,              // 위젯 1
        List<TrafficTrendResponseDTO> trafficTrend,                   // 위젯 2
        List<StatusCodeResponseDTO> httpStatusCodeDistribution,       // 위젯 3
        List<TopDomainResponseDTO> topDomains,                        // 위젯 4
        List<CountryTrafficResponseDTO> trafficByCountry,             // 위젯 5
        List<ErrorPageResponseDTO> errorPages,                        // 위젯 6

        // === 추가 위젯 (4개) ===
        List<PageLoadTimeResponseDTO> pageLoadTimeTrend,              // 위젯 7
        List<ErrorRateResponseDTO> errorRateTrend,                    // 위젯 8
        List<BrowserPerfResponseDTO> browserPerformance,              // 위젯 9
        List<DevicePerfResponseDTO> devicePerformanceDistribution,    // 위젯 10

        // === 필터 옵션 (사용 가능한 값 목록) ===
        DashboardFiltersDTO availableFilters,

        // === 메타 정보 ===
        Timestamp startTime,
        Timestamp endTime
) {}