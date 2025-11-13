package com.moa.api.dashboard.controller;

import com.moa.api.dashboard.dto.request.*;
import com.moa.api.dashboard.dto.response.*;
import com.moa.api.dashboard.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard/widgets")
public class DashboardWidgetController {

    private final DashboardService widgetService;

    public DashboardWidgetController(DashboardService widgetService) {
        this.widgetService = widgetService;
    }

    /**
     * 위젯 1: 실시간 트래픽 추이
     */
    @PostMapping("/traffic-trend")
    public ResponseEntity<List<TrafficTrendResponseDTO>> getTrafficTrend(
            @RequestBody TrafficTrendRequestDTO request) {

        TimeRangeResult timeRange = extractTimeRange(
                request.getRange(),
                request.getTimePreset()
        );
        if (timeRange == null) {
            return ResponseEntity.badRequest().build();
        }

        List<TrafficTrendResponseDTO> result = widgetService.getTrafficTrend(
                timeRange.fromEpoch,
                timeRange.toEpoch
        );
        return ResponseEntity.ok(result);
    }

    /**
     * 위젯 2: TCP 에러율
     */
    @PostMapping("/tcp-error-rate")
    public ResponseEntity<TcpErrorRateResponseDTO> getTcpErrorRate(
            @RequestBody TcpErrorRateRequestDTO request) {

        TimeRangeResult timeRange = extractTimeRange(
                request.getRange(),
                request.getTimePreset()
        );
        if (timeRange == null) {
            return ResponseEntity.badRequest().build();
        }

        TcpErrorRateResponseDTO result = widgetService.getTcpErrorRate(
                timeRange.fromEpoch,
                timeRange.toEpoch
        );
        return ResponseEntity.ok(result);
    }

    /**
     * 위젯 3: 지역별 트래픽 분포 (필터 지원)
     */
    @PostMapping("/traffic-by-country")
    public ResponseEntity<List<TrafficByCountryResponseDTO>> getTrafficByCountry(
            @RequestBody TrafficByCountryRequestDTO request) {

        TimeRangeResult timeRange = extractTimeRange(
                request.getRange(),
                request.getTimePreset()
        );
        if (timeRange == null) {
            return ResponseEntity.badRequest().build();
        }

        List<TrafficByCountryResponseDTO> result;

        // 국가 필터가 있으면 필터링된 데이터 반환
        if (request.getCountries() != null && !request.getCountries().isEmpty()) {
            result = widgetService.getTrafficByCountries(
                    timeRange.fromEpoch,
                    timeRange.toEpoch,
                    request.getCountries()
            );
        } else {
            result = widgetService.getTrafficByCountry(
                    timeRange.fromEpoch,
                    timeRange.toEpoch
            );
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 위젯 4: HTTP 상태코드 분포
     */
    @PostMapping("/http-status")
    public ResponseEntity<HttpStatusCodeResponseDTO> getHttpStatus(
            @RequestBody HttpStatusCodeRequestDTO request) {

        TimeRangeResult timeRange = extractTimeRange(
                request.getRange(),
                request.getTimePreset()
        );
        if (timeRange == null) {
            return ResponseEntity.badRequest().build();
        }

        HttpStatusCodeResponseDTO result = widgetService.getHttpStatusCodeDistribution(
                timeRange.fromEpoch,
                timeRange.toEpoch
        );
        return ResponseEntity.ok(result);
    }

    /**
     * 위젯 5: Top 도메인 (필터 지원)
     */
    @PostMapping("/top-domains")
    public ResponseEntity<List<TopDomainResponseDTO>> getTopDomains(
            @RequestBody TopDomainRequestDTO request) {

        TimeRangeResult timeRange = extractTimeRange(
                request.getRange(),
                request.getTimePreset()
        );
        if (timeRange == null) {
            return ResponseEntity.badRequest().build();
        }

        List<TopDomainResponseDTO> result = widgetService.getTopDomains(
                timeRange.fromEpoch,
                timeRange.toEpoch
        );

        return ResponseEntity.ok(result);
    }

    /**
     * 위젯 6: 응답시간 통계
     */
    @PostMapping("/response-time")
    public ResponseEntity<ResponseTimeResponseDTO> getResponseTime(
            @RequestBody ResponseTimeRequestDTO request) {

        TimeRangeResult timeRange = extractTimeRange(
                request.getRange(),
                request.getTimePreset()
        );
        if (timeRange == null) {
            return ResponseEntity.badRequest().build();
        }

        ResponseTimeResponseDTO result = widgetService.getResponseTimeStats(
                timeRange.fromEpoch,
                timeRange.toEpoch
        );
        return ResponseEntity.ok(result);
    }

    /**
     * 시간 범위 추출 및 검증 (통합 버전)
     */
    private TimeRangeResult extractTimeRange(
            DashboardRequestDTO.EpochRange range,
            String timePreset) {

        Long fromEpoch = null;
        Long toEpoch = null;

        if (range != null) {
            fromEpoch = range.getFromEpoch();
            toEpoch = range.getToEpoch();
        }

        long nowEpoch = Instant.now().getEpochSecond();
        if (toEpoch == null) {
            toEpoch = nowEpoch;
        }
        if (fromEpoch == null) {
            if (timePreset != null) {
                switch (timePreset) {
                    case "1H" -> fromEpoch = toEpoch - 3600L;
                    case "24H" -> fromEpoch = toEpoch - 24L * 3600L;
                    case "7D" -> fromEpoch = toEpoch - 7L * 24L * 3600L;
                    default -> fromEpoch = toEpoch - 3600L;
                }
            } else {
                fromEpoch = toEpoch - 3600L;
            }
        }

        if (fromEpoch > toEpoch) {
            return null;
        }

        // 최대 조회 기간 제한 (30일)
        long maxRange = 30L * 24L * 3600L;
        if (toEpoch - fromEpoch > maxRange) {
            return null;
        }

        return new TimeRangeResult(fromEpoch, toEpoch);
    }

    /**
     * 내부 시간 범위 결과 객체
     */
    private static class TimeRangeResult {
        final Long fromEpoch;
        final Long toEpoch;

        TimeRangeResult(Long fromEpoch, Long toEpoch) {
            this.fromEpoch = fromEpoch;
            this.toEpoch = toEpoch;
        }
    }
}