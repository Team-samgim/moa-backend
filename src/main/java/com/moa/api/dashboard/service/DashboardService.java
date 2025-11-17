package com.moa.api.dashboard.service;

import com.moa.api.dashboard.dto.request.DashboardRequestDTO;
import com.moa.api.dashboard.dto.response.*;
import com.moa.api.dashboard.repository.DashboardRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;

@Service
public class DashboardService {

    private final DashboardRepository dashboardRepository;

    public DashboardService(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    /**
     * 전체 대시보드 데이터 조회 (필터 지원)
     *
     * @param request 대시보드 요청 (시간 범위 + 필터)
     * @return 전체 위젯 데이터 + 사용 가능한 필터 옵션
     */
    @Cacheable(value = "dashboard",
            key = "#request.range.fromEpoch + '-' + #request.range.toEpoch + '-' + (#request.filters != null ? #request.filters.hashCode() : 0)")
    public DashboardResponseDTO getDashboardData(DashboardRequestDTO request) {

        Long startTimeUnix = request.getRange().getFromEpoch();
        Long endTimeUnix = request.getRange().getToEpoch();
        DashboardRequestDTO.DashboardFilters filters = request.getFilters();

        // 1. 위젯 데이터 조회 (필터 적용)

        // 실시간 트래픽 추이
        var trafficTrend = dashboardRepository.getTrafficTrend(startTimeUnix, endTimeUnix, filters);

        // 페이지 로드 시간
        var pageLoadTimeTrend = dashboardRepository.getPageLoadTimeTrend(startTimeUnix, endTimeUnix, filters);


        var responseTimeStats = dashboardRepository.getResponseTimeStats(startTimeUnix, endTimeUnix, filters);
        var statusCodeDistribution = dashboardRepository.getStatusCodeDistribution(startTimeUnix, endTimeUnix, filters);
        var topDomains = dashboardRepository.getTopDomains(startTimeUnix, endTimeUnix, filters);
        var trafficByCountry = dashboardRepository.getTrafficByCountry(startTimeUnix, endTimeUnix, filters);
        var errorPages = dashboardRepository.getErrorPages(startTimeUnix, endTimeUnix, filters);

        var errorRateTrend = dashboardRepository.getErrorRateTrend(startTimeUnix, endTimeUnix, filters);
        var browserPerformance = dashboardRepository.getBrowserPerformance(startTimeUnix, endTimeUnix, filters);
        var devicePerformanceDistribution = dashboardRepository.getDevicePerformanceDistribution(startTimeUnix, endTimeUnix, filters);

        // 2. 사용 가능한 필터 옵션 조회
        var availableFilters = new DashboardFiltersDTO(
                dashboardRepository.getAvailableCountries(startTimeUnix, endTimeUnix),
                dashboardRepository.getAvailableBrowsers(startTimeUnix, endTimeUnix),
                dashboardRepository.getAvailableDevices(startTimeUnix, endTimeUnix),
                dashboardRepository.getAvailableHttpHosts(startTimeUnix, endTimeUnix),
                dashboardRepository.getAvailableHttpMethods(startTimeUnix, endTimeUnix)
        );

        // 3. 응답 조합
        return new DashboardResponseDTO(
                responseTimeStats,
                trafficTrend,
                statusCodeDistribution,
                topDomains,
                trafficByCountry,
                errorPages,
                pageLoadTimeTrend,
                errorRateTrend,
                browserPerformance,
                devicePerformanceDistribution,
                availableFilters,
                Timestamp.from(Instant.ofEpochSecond(startTimeUnix)),
                Timestamp.from(Instant.ofEpochSecond(endTimeUnix))
        );
    }
}