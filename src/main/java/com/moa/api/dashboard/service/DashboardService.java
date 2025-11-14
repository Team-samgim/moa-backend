package com.moa.api.dashboard.service;

import com.moa.api.dashboard.dto.response.*;
import com.moa.api.dashboard.repository.DashboardRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DashboardService - Unix timestamp 처리
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final DashboardRepository repository;

    public DashboardService(DashboardRepository repository) {
        this.repository = repository;
    }

    /**
     * 전체 대시보드 데이터 조회
     *
     * @param startTimeUnix 시작 시간 (Unix timestamp 초)
     * @param endTimeUnix 종료 시간 (Unix timestamp 초)
     * @return 6개 위젯 데이터
     */
    @Cacheable(value = "dashboard", key = "#startTimeUnix + '-' + #endTimeUnix")
    public DashboardResponseDTO getDashboardData(Long startTimeUnix, Long endTimeUnix) {
        return new DashboardResponseDTO(
                getTrafficTrend(startTimeUnix, endTimeUnix),
                getTcpErrorRate(startTimeUnix, endTimeUnix),
                getTrafficByCountry(startTimeUnix, endTimeUnix),
                getHttpStatusCodeDistribution(startTimeUnix, endTimeUnix),
                getTopDomains(startTimeUnix, endTimeUnix),
                getResponseTimeStats(startTimeUnix, endTimeUnix),
                Timestamp.from(Instant.ofEpochSecond(startTimeUnix)),
                Timestamp.from(Instant.ofEpochSecond(endTimeUnix))
        );
    }

    // ============================================
    // 위젯 1: 실시간 트래픽 추이
    // ============================================

    public List<TrafficTrendResponseDTO> getTrafficTrend(Long startTimeUnix, Long endTimeUnix) {
        List<Map<String, Object>> results = repository.getTrafficTrend(startTimeUnix, endTimeUnix);
        List<TrafficTrendResponseDTO> trends = new ArrayList<>();

        for (Map<String, Object> row : results) {
            trends.add(new TrafficTrendResponseDTO(
                    (Timestamp) row.get("timestamp"),
                    getLong(row, "request_count"),
                    getLong(row, "response_count")
            ));
        }

        return trends;
    }

    // ============================================
    // 위젯 2: TCP 에러율
    // ============================================

    public TcpErrorRateResponseDTO getTcpErrorRate(Long startTimeUnix, Long endTimeUnix) {
        try {
            Map<String, Object> result = repository.getTcpErrorRate(startTimeUnix, endTimeUnix);

            return new TcpErrorRateResponseDTO(
                    getDouble(result, "total_error_rate"),
                    getDouble(result, "retransmission_rate"),
                    getDouble(result, "out_of_order_rate"),
                    getDouble(result, "lost_segment_rate"),
                    getLong(result, "total_errors"),
                    getLong(result, "total_packets")
            );
        } catch (Exception e) {
            return new TcpErrorRateResponseDTO(0.0, 0.0, 0.0, 0.0, 0L, 0L);
        }
    }

    // ============================================
    // 위젯 3: 지역별 트래픽 분포
    // ============================================

    @Cacheable(value = "trafficByCountry", key = "#startTimeUnix + '-' + #endTimeUnix")
    public List<TrafficByCountryResponseDTO> getTrafficByCountry(Long startTimeUnix, Long endTimeUnix) {
        List<Map<String, Object>> results = repository.getTrafficByCountry(startTimeUnix, endTimeUnix);
        List<TrafficByCountryResponseDTO> countries = new ArrayList<>();

        for (Map<String, Object> row : results) {
            countries.add(new TrafficByCountryResponseDTO(
                    (String) row.get("country_name"),
                    getLong(row, "traffic_volume"),
                    getLong(row, "request_count"),
                    getDouble(row, "percentage")
            ));
        }

        return countries;
    }

    /**
     * ✅ 특정 국가만 필터링해서 조회
     */
    @Cacheable(value = "trafficByCountries",
            key = "#startTimeUnix + '-' + #endTimeUnix + '-' + #countryNames.hashCode()")
    public List<TrafficByCountryResponseDTO> getTrafficByCountries(
            Long startTimeUnix,
            Long endTimeUnix,
            List<String> countryNames) {

        // DB에서 특정 국가만 조회
        List<Map<String, Object>> results = repository.getTrafficByCountries(
                startTimeUnix,
                endTimeUnix,
                countryNames  // ✅ "South Korea", "United States of America" 전달
        );

        List<TrafficByCountryResponseDTO> countries = new ArrayList<>();
        for (Map<String, Object> row : results) {
            countries.add(new TrafficByCountryResponseDTO(
                    (String) row.get("country_name"),  // ✅ 전체 이름 그대로 반환
                    getLong(row, "traffic_volume"),
                    getLong(row, "request_count"),
                    getDouble(row, "percentage")
            ));
        }

        return countries;
    }

    // ============================================
    // 위젯 4: HTTP 상태코드 분포
    // ============================================

    public HttpStatusCodeResponseDTO getHttpStatusCodeDistribution(Long startTimeUnix, Long endTimeUnix) {
        try {
            Map<String, Object> result = repository.getHttpStatusCodeDistribution(startTimeUnix, endTimeUnix);

            return new HttpStatusCodeResponseDTO(
                    getLong(result, "success_count"),
                    getLong(result, "redirect_count"),
                    getLong(result, "client_error_count"),
                    getLong(result, "server_error_count"),
                    getLong(result, "total_count")
            );
        } catch (Exception e) {
            return new HttpStatusCodeResponseDTO(0L, 0L, 0L, 0L, 0L);
        }
    }

    // ============================================
    // 위젯 5: Top 10 도메인
    // ============================================

    public List<TopDomainResponseDTO> getTopDomains(Long startTimeUnix, Long endTimeUnix) {
        List<Map<String, Object>> results = repository.getTopDomains(startTimeUnix, endTimeUnix);
        List<TopDomainResponseDTO> domains = new ArrayList<>();

        for (Map<String, Object> row : results) {
            domains.add(new TopDomainResponseDTO(
                    (String) row.get("domain"),
                    getLong(row, "request_count"),
                    getLong(row, "traffic_volume"),
                    getDouble(row, "percentage")
            ));
        }

        return domains;
    }

    // ============================================
    // 위젯 6: 응답시간 통계
    // ============================================

    public ResponseTimeResponseDTO getResponseTimeStats(Long startTimeUnix, Long endTimeUnix) {
        try {
            Map<String, Object> result = repository.getResponseTimeStats(startTimeUnix, endTimeUnix);

            return new ResponseTimeResponseDTO(
                    getDouble(result, "min_response_time"),
                    getDouble(result, "avg_response_time"),
                    getDouble(result, "max_response_time"),
                    getDouble(result, "p50_response_time"),
                    getDouble(result, "p95_response_time"),
                    getDouble(result, "p99_response_time"),
                    getLong(result, "total_requests")
            );
        } catch (Exception e) {
            return new ResponseTimeResponseDTO(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0L);
        }
    }

    // ============================================
    // 헬퍼 메서드 - 타입 변환
    // ============================================

    private Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0L;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof BigDecimal) return ((BigDecimal) value).longValue();
        return Long.parseLong(value.toString());
    }

    private Double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0.0;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Float) return ((Float) value).doubleValue();
        if (value instanceof BigDecimal) return ((BigDecimal) value).doubleValue();
        return Double.parseDouble(value.toString());
    }
}