package com.moa.api.dashboard.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DashboardRepository - ts_server_nsec 직접 사용
 *
 * ts_server_nsec: Unix timestamp (초 단위, float8)
 * 예: 1761876000.0335863 → 2025-10-30 21:46:40.033 UTC
 *
 * 변환 불필요! 그냥 직접 비교
 */
@Repository
public class DashboardRepository {

    private final JdbcTemplate jdbcTemplate;

    public DashboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ============================================
    // 위젯 1: 실시간 트래픽 추이
    // ============================================

    public List<Map<String, Object>> getTrafficTrend(Long startTimeUnix, Long endTimeUnix) {
        String sql = """
            SELECT 
                TO_TIMESTAMP(FLOOR(ts_server_nsec / 60) * 60) as timestamp,
                SUM(page_pkt_cnt_req) as request_count,
                SUM(page_pkt_cnt_res) as response_count
            FROM http_page_sample
            WHERE ts_server_nsec BETWEEN ? AND ?
            GROUP BY FLOOR(ts_server_nsec / 60)
            ORDER BY timestamp
            """;

        return jdbcTemplate.queryForList(sql, startTimeUnix, endTimeUnix);
    }

    // ============================================
    // 위젯 2: TCP 에러율
    // ============================================

    public Map<String, Object> getTcpErrorRate(Long startTimeUnix, Long endTimeUnix) {
        String sql = """
            WITH error_stats AS (
                SELECT 
                    COALESCE(SUM(retransmission_cnt), 0) as retrans_cnt,
                    COALESCE(SUM(out_of_order_cnt), 0) as ooo_cnt,
                    COALESCE(SUM(lost_seg_cnt), 0) as lost_cnt,
                    COALESCE(SUM(page_pkt_cnt), 0) as total_pkt
                FROM http_page_sample
                WHERE ts_server_nsec BETWEEN ? AND ?
            )
            SELECT 
                ROUND(CAST((retrans_cnt + ooo_cnt + lost_cnt) AS NUMERIC) / NULLIF(total_pkt, 0) * 100, 2) as total_error_rate,
                ROUND(CAST(retrans_cnt AS NUMERIC) / NULLIF(total_pkt, 0) * 100, 2) as retransmission_rate,
                ROUND(CAST(ooo_cnt AS NUMERIC) / NULLIF(total_pkt, 0) * 100, 2) as out_of_order_rate,
                ROUND(CAST(lost_cnt AS NUMERIC) / NULLIF(total_pkt, 0) * 100, 2) as lost_segment_rate,
                (retrans_cnt + ooo_cnt + lost_cnt) as total_errors,
                total_pkt as total_packets
            FROM error_stats
            """;

        return jdbcTemplate.queryForMap(sql, startTimeUnix, endTimeUnix);
    }

    // ============================================
    // 위젯 3: 지역별 트래픽 분포 (Top 10)
    // ============================================

    public List<Map<String, Object>> getTrafficByCountry(Long startTimeUnix, Long endTimeUnix) {
        String sql = """
            WITH country_traffic AS (
                SELECT 
                    COALESCE(NULLIF(TRIM(country_name_req), ''), 'Unknown') AS country_name,
                    SUM(page_pkt_len) AS traffic_volume,
                    COUNT(*) AS request_count
                FROM http_page_sample
                WHERE ts_server_nsec BETWEEN ? AND ?
                GROUP BY COALESCE(NULLIF(TRIM(country_name_req), ''), 'Unknown')
            ),
            total_traffic AS (
                SELECT SUM(traffic_volume) AS total
                FROM country_traffic
            )
            SELECT 
                ct.country_name,
                ct.traffic_volume,
                ct.request_count,
                ROUND(ct.traffic_volume * 100.0 / NULLIF(tt.total, 0), 1) AS percentage
            FROM country_traffic ct
            CROSS JOIN total_traffic tt
            ORDER BY ct.traffic_volume DESC
            LIMIT 10
            """;

        return jdbcTemplate.queryForList(sql, startTimeUnix, endTimeUnix);
    }

    /**
     * ✅ 특정 국가들의 트래픽 조회 (필터링)
     */
    public List<Map<String, Object>> getTrafficByCountries(
            Long startTimeUnix,
            Long endTimeUnix,
            List<String> countryNames) {

        // IN 절을 위한 placeholder 생성
        String placeholders = String.join(",",
                countryNames.stream()
                        .map(c -> "?")
                        .toList()
        );

        String sql = """
            WITH country_traffic AS (
                SELECT 
                    COALESCE(country_name_req, 'Unknown') as country_name,
                    SUM(page_pkt_len) as traffic_volume,
                    COUNT(*) as request_count
                FROM http_page_sample
                WHERE ts_server_nsec BETWEEN ? AND ?
                    AND COALESCE(country_name_req, 'Unknown') IN (%s)
                GROUP BY country_name_req
            ),
            total_traffic AS (
                SELECT SUM(traffic_volume) as total
                FROM country_traffic
            )
            SELECT 
                ct.country_name,
                ct.traffic_volume,
                ct.request_count,
                ROUND(ct.traffic_volume * 100.0 / NULLIF(tt.total, 0), 1) as percentage
            FROM country_traffic ct, total_traffic tt
            ORDER BY ct.traffic_volume DESC
            """.formatted(placeholders);

        // 파라미터 배열 생성
        List<Object> params = new ArrayList<>();
        params.add(startTimeUnix);
        params.add(endTimeUnix);
        params.addAll(countryNames);

        return jdbcTemplate.queryForList(sql, params.toArray());
    }

    // ============================================
    // 위젯 4: HTTP 상태코드 분포
    // ============================================

    public Map<String, Object> getHttpStatusCodeDistribution(Long startTimeUnix, Long endTimeUnix) {
        String sql = """
            SELECT 
                COALESCE(SUM(res_code_2xx_cnt), 0) as success_count,
                COALESCE(SUM(res_code_3xx_cnt), 0) as redirect_count,
                COALESCE(SUM(res_code_4xx_cnt), 0) as client_error_count,
                COALESCE(SUM(res_code_5xx_cnt), 0) as server_error_count,
                COALESCE(SUM(res_code_2xx_cnt + res_code_3xx_cnt + res_code_4xx_cnt + res_code_5xx_cnt), 0) as total_count
            FROM http_page_sample
            WHERE ts_server_nsec BETWEEN ? AND ?
            """;

        return jdbcTemplate.queryForMap(sql, startTimeUnix, endTimeUnix);
    }

    // ============================================
    // 위젯 5: Top 10 도메인
    // ============================================

    public List<Map<String, Object>> getTopDomains(Long startTimeUnix, Long endTimeUnix) {
        String sql = """
            WITH domain_stats AS (
                SELECT 
                    http_host as domain,
                    COUNT(*) as request_count,
                    SUM(page_pkt_len) as traffic_volume
                FROM http_page_sample
                WHERE ts_server_nsec BETWEEN ? AND ?
                    AND http_host IS NOT NULL
                    AND http_host != ''
                GROUP BY http_host
            ),
            total_requests AS (
                SELECT SUM(request_count) as total
                FROM domain_stats
            )
            SELECT 
                ds.domain,
                ds.request_count,
                ds.traffic_volume,
                ROUND(ds.request_count * 100.0 / NULLIF(tr.total, 0), 1) as percentage
            FROM domain_stats ds, total_requests tr
            ORDER BY ds.request_count DESC
            LIMIT 10
            """;

        return jdbcTemplate.queryForList(sql, startTimeUnix, endTimeUnix);
    }

    // ============================================
    // 위젯 6: 응답시간 통계
    // ============================================

    public Map<String, Object> getResponseTimeStats(Long startTimeUnix, Long endTimeUnix) {
        String sql = """
            SELECT 
                ROUND(MIN(ts_page_res)::numeric, 2) as min_response_time,
                ROUND(AVG(ts_page_res)::numeric, 2) as avg_response_time,
                ROUND(MAX(ts_page_res)::numeric, 2) as max_response_time,
                ROUND(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY ts_page_res)::numeric, 2) as p50_response_time,
                ROUND(PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY ts_page_res)::numeric, 2) as p95_response_time,
                ROUND(PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY ts_page_res)::numeric, 2) as p99_response_time,
                COUNT(*) as total_requests
            FROM http_page_sample
            WHERE ts_server_nsec BETWEEN ? AND ?
                AND ts_page_res IS NOT NULL
                AND ts_page_res > 0
            """;

        return jdbcTemplate.queryForMap(sql, startTimeUnix, endTimeUnix);
    }
}