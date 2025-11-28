/**
 * 작성자: 정소영
 * 설명: 대시보드 위젯 데이터 조회를 위한 Repository
 *      (트래픽, 응답시간, 에러율, 상태코드, Top10, 국가/브라우저/디바이스 성능 등
 *       전체 위젯의 SQL 집계 로직을 포함)
 */
package com.moa.api.dashboard.repository;

import com.moa.api.dashboard.dto.request.DashboardRequestDTO;
import com.moa.api.dashboard.dto.response.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DashboardRepository - ts_server_nsec 직접 사용 + 필터 적용
 *
 * ts_server_nsec: Unix timestamp (초 단위, float8)
 * 예: 1761876000.0335863 → 2025-10-30 21:46:40.033 UTC
 *
 * 변환 불필요! 그냥 직접 비교
 *
 * 테이블명: http_page_sample
 *
 * ============================================
 * 중요: 페이지 로드 시간 vs 응답 시간
 * ============================================
 * - ts_page: 페이지 전체 로딩 시간 (사용자 체감 시간)
 * - ts_page_res: 서버 응답 시간 (서버 처리 시간)
 *
 * ============================================
 * 수정사항: 5분 단위 timestamp 추가
 * ============================================
 * - 기존에 timestamp가 없던 메서드들에 5분 단위 집계 추가
 * - 슬라이딩 윈도우를 위해 프론트엔드에서 정확한 시간 기반 필터링 가능
 */
@Repository
public class DashboardRepository {

    private final JdbcTemplate jdbcTemplate;

    public DashboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ============================================
    // 위젯 1: 페이지 로드 시간 트렌드
    // - 와이어프레임 위젯 1: "페이지 로드 시간 트렌드 (24시간)"
    // - 역할: 시간대별 페이지 로드 시간(ts_page)의 통계를 구해
    //         메인 트렌드 차트의 "페이지 로드 시간" 라인 데이터로 사용
    // - 지표: avg, min, max, p95, p99
    // ============================================

    public List<PageLoadTimeResponseDTO> getPageLoadTimeTrend(
            Long startTimeUnix,
            Long endTimeUnix,
            DashboardRequestDTO.DashboardFilters filters) {

        String whereClause = buildWhereClause(filters);

        String sql = """
        SELECT 
            TO_TIMESTAMP(FLOOR(ts_server_nsec / 60) * 60) as timestamp,
            ROUND(AVG(ts_page)::numeric, 2) as avg_page_load_time,
            ROUND(MIN(ts_page)::numeric, 2) as min_page_load_time,
            ROUND(MAX(ts_page)::numeric, 2) as max_page_load_time,
            ROUND(PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY ts_page)::numeric, 2) as p95_page_load_time,
            ROUND(PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY ts_page)::numeric, 2) as p99_page_load_time
        FROM http_page_sample
        WHERE ts_server_nsec BETWEEN ? AND ?
            AND ts_page IS NOT NULL
            AND ts_page > 0
        """ + whereClause + """
        GROUP BY FLOOR(ts_server_nsec / 60)
        ORDER BY timestamp
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                        new PageLoadTimeResponseDTO(
                                rs.getTimestamp("timestamp"),
                                rs.getDouble("avg_page_load_time"),
                                rs.getDouble("min_page_load_time"),
                                rs.getDouble("max_page_load_time"),
                                rs.getDouble("p95_page_load_time"),
                                rs.getDouble("p99_page_load_time")
                        )
                , startTimeUnix, endTimeUnix);
    }

    // ============================================
    // 서버 응답 시간 트렌드 (추가 데이터)
    // - 역할: 시간대별 서버 응답 시간(ts_page_res)의 평균, p95, p99를 구해
    //         서버 성능 모니터링에 사용
    // - 지표: avg, p95, p99
    // - 참고: 페이지 로드 시간과는 다른 개념!
    // ============================================

    public List<ResponseTimeResponseDTO> getResponseTimeStats(
            Long startTimeUnix,
            Long endTimeUnix,
            DashboardRequestDTO.DashboardFilters filters) {

        String whereClause = buildWhereClause(filters);

        String sql = """
            SELECT 
                TO_TIMESTAMP(FLOOR(ts_server_nsec / 60) * 60) as timestamp,
                ROUND(AVG(ts_page_res)::numeric, 2) as avg_response_time,
                ROUND(PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY ts_page_res)::numeric, 2) as p95_response_time,
                ROUND(PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY ts_page_res)::numeric, 2) as p99_response_time
            FROM http_page_sample
            WHERE ts_server_nsec BETWEEN ? AND ?
                AND ts_page_res IS NOT NULL
                AND ts_page_res > 0
            """ + whereClause + """
            GROUP BY FLOOR(ts_server_nsec / 60)
            ORDER BY timestamp
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                        new ResponseTimeResponseDTO(
                                rs.getTimestamp("timestamp"),
                                Double.valueOf(rs.getDouble("avg_response_time")),
                                Double.valueOf(rs.getDouble("p95_response_time")),
                                Double.valueOf(rs.getDouble("p99_response_time"))
                        )
                , startTimeUnix, endTimeUnix);
    }

    // ============================================
    // 위젯 2: 에러율 트렌드
    // - 와이어프레임 위젯 2: "에러율 트렌드"
    // - 역할: 전체 요청 대비 4xx, 5xx 에러 비율을 시간대별로 계산해
    //         에러율 라인 차트(전체 에러율, 4xx, 5xx)에 사용하는 데이터 제공
    // ============================================

    public List<ErrorRateResponseDTO> getErrorRateTrend(Long startTimeUnix, Long endTimeUnix, DashboardRequestDTO.DashboardFilters filters) {
        String whereClause = buildWhereClause(filters);

        String sql = """
            SELECT 
                TO_TIMESTAMP(FLOOR(ts_server_nsec / 60) * 60) as timestamp,
                COUNT(*) as total_count,
                SUM(CASE WHEN http_res_code::integer >= 400 THEN 1 ELSE 0 END) as error_count,
                SUM(CASE WHEN http_res_code::integer BETWEEN 400 AND 499 THEN 1 ELSE 0 END) as client_error_count,
                SUM(CASE WHEN http_res_code::integer BETWEEN 500 AND 599 THEN 1 ELSE 0 END) as server_error_count
            FROM http_page_sample
            WHERE ts_server_nsec BETWEEN ? AND ?
                AND http_res_code IS NOT NULL
            """ + whereClause + """
            GROUP BY FLOOR(ts_server_nsec / 60)
            ORDER BY timestamp
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            long totalCount = rs.getLong("total_count");
            long errorCount = rs.getLong("error_count");
            long clientErrorCount = rs.getLong("client_error_count");
            long serverErrorCount = rs.getLong("server_error_count");

            double errorRate = totalCount > 0 ? (errorCount * 100.0 / totalCount) : 0;
            double clientErrorRate = totalCount > 0 ? (clientErrorCount * 100.0 / totalCount) : 0;
            double serverErrorRate = totalCount > 0 ? (serverErrorCount * 100.0 / totalCount) : 0;

            return new ErrorRateResponseDTO(
                    rs.getTimestamp("timestamp"),
                    Long.valueOf(totalCount),
                    Long.valueOf(errorCount),
                    Long.valueOf(clientErrorCount),
                    Long.valueOf(serverErrorCount),
                    Double.valueOf(errorRate),
                    Double.valueOf(clientErrorRate),
                    Double.valueOf(serverErrorRate)
            );
        }, startTimeUnix, endTimeUnix);
    }


    // ============================================
    // 위젯 5: 트래픽 추이 (트래픽 모니터링)
    // - 와이어프레임 위젯 5: "트래픽 추이"
    // - 역할: 분 단위로 집계된 요청/응답 Mbps(mbps_req, mbps_res)와
    //         요청/응답 건수(page_http_cnt_req, page_http_cnt_res)를 조회해
    //         Request/Response 트래픽 시계열 그래프에 사용
    // ============================================

    public List<TrafficTrendResponseDTO> getTrafficTrend(Long startTimeUnix, Long endTimeUnix, DashboardRequestDTO.DashboardFilters filters) {
        String whereClause = buildWhereClause(filters);

        String sql = """
            SELECT 
                TO_TIMESTAMP(FLOOR(ts_server_nsec / 60) * 60) as timestamp,
                ROUND(AVG(mbps_req)::numeric, 2) as mbps_req,
                ROUND(AVG(mbps_res)::numeric, 2) as mbps_res,
                COALESCE(SUM(page_http_cnt_req), 0) as request_count,
                COALESCE(SUM(page_http_cnt_res), 0) as response_count
            FROM http_page_sample
            WHERE ts_server_nsec BETWEEN ? AND ?
            """ + whereClause + """
            GROUP BY FLOOR(ts_server_nsec / 60)
            ORDER BY timestamp
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                        new TrafficTrendResponseDTO(
                                rs.getTimestamp("timestamp"),
                                Double.valueOf(rs.getDouble("mbps_req")),
                                Double.valueOf(rs.getDouble("mbps_res")),
                                Long.valueOf(rs.getLong("request_count")),
                                Long.valueOf(rs.getLong("response_count"))
                        )
                , startTimeUnix, endTimeUnix);
    }

    // ============================================
    // 위젯 3: HTTP 상태 코드 분포 (도넛 차트)
    // - 와이어프레임 위젯 3: "HTTP 상태 코드 분포"
    // - 역할: 2xx, 3xx, 4xx, 5xx 응답 코드 비율을 시간대별로 집계
    // - ⭐ 수정: 5분 단위 timestamp 추가
    // ============================================

    public List<StatusCodeResponseDTO> getStatusCodeDistribution(Long startTimeUnix, Long endTimeUnix, DashboardRequestDTO.DashboardFilters filters) {
        String whereClause = buildWhereClause(filters);

        String sql = """
            SELECT 
                TO_TIMESTAMP(FLOOR(ts_server_nsec / 300) * 300) as timestamp,
                COALESCE(SUM(res_code_2xx_cnt), 0) as success_count,
                COALESCE(SUM(res_code_3xx_cnt), 0) as redirect_count,
                COALESCE(SUM(res_code_4xx_cnt), 0) as client_error_count,
                COALESCE(SUM(res_code_5xx_cnt), 0) as server_error_count
            FROM http_page_sample
            WHERE ts_server_nsec BETWEEN ? AND ?
            """ + whereClause + """
            GROUP BY FLOOR(ts_server_nsec / 300)
            ORDER BY timestamp
            """;

        List<StatusCodeResponseDTO> result = new ArrayList<>();

        jdbcTemplate.query(sql, (rs) -> {
            java.sql.Timestamp timestamp = rs.getTimestamp("timestamp");
            long successCount = rs.getLong("success_count");
            long redirectCount = rs.getLong("redirect_count");
            long clientErrorCount = rs.getLong("client_error_count");
            long serverErrorCount = rs.getLong("server_error_count");
            long total = successCount + redirectCount + clientErrorCount + serverErrorCount;

            // 각 상태 그룹별로 4개 행 추가 (기존 DTO 구조 유지)
            result.add(new StatusCodeResponseDTO(timestamp, "2xx", successCount,
                    total > 0 ? Math.round(successCount * 1000.0 / total) / 10.0 : 0.0));
            result.add(new StatusCodeResponseDTO(timestamp, "3xx", redirectCount,
                    total > 0 ? Math.round(redirectCount * 1000.0 / total) / 10.0 : 0.0));
            result.add(new StatusCodeResponseDTO(timestamp, "4xx", clientErrorCount,
                    total > 0 ? Math.round(clientErrorCount * 1000.0 / total) / 10.0 : 0.0));
            result.add(new StatusCodeResponseDTO(timestamp, "5xx", serverErrorCount,
                    total > 0 ? Math.round(serverErrorCount * 1000.0 / total) / 10.0 : 0.0));
        }, startTimeUnix, endTimeUnix);

        return result;
    }

    // ============================================
    // 위젯 6: 느린 페이지 Top 10
    // - 와이어프레임 위젯 6: "느린 페이지 Top 10"
    // - 역할: URI(http_uri) 별 평균 응답 시간(ts_page_res)을 내림차순 정렬해
    //         가장 느린 페이지 상위 10개를 표/바 차트로 보여주는 데이터 제공
    // - ⭐ 수정: 5분 단위 timestamp 추가
    // ============================================

    public List<TopDomainResponseDTO> getTopDomains(Long startTimeUnix, Long endTimeUnix, DashboardRequestDTO.DashboardFilters filters) {
        String whereClause = buildWhereClause(filters);

        String sql = """
            SELECT 
                TO_TIMESTAMP(FLOOR(ts_server_nsec / 300) * 300) as timestamp,
                http_uri,
                ROUND(AVG(ts_page_res)::numeric, 2) as avg_response_time,
                COUNT(*) as request_count
            FROM http_page_sample
            WHERE ts_server_nsec BETWEEN ? AND ?
                AND http_uri IS NOT NULL
                AND http_uri != ''
                AND ts_page_res IS NOT NULL
                AND ts_page_res > 0
            """ + whereClause + """
            GROUP BY FLOOR(ts_server_nsec / 300), http_uri
            ORDER BY timestamp, avg_response_time DESC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                        new TopDomainResponseDTO(
                                rs.getTimestamp("timestamp"),
                                rs.getString("http_uri"),
                                Double.valueOf(rs.getDouble("avg_response_time")),
                                Long.valueOf(rs.getLong("request_count"))
                        )
                , startTimeUnix, endTimeUnix);
    }

    // ============================================
    // 위젯 8: 지역별 응답 시간 히트맵
    // - 와이어프레임 위젯 8: "지역별 응답 시간 히트맵"
    // - 역할: 국가(country_name_req)별 평균 응답 시간(ts_page_res)과 요청 수를 집계해
    //         세계 지도/히트맵 위젯에 사용
    // - ⭐ 수정: 5분 단위 timestamp 추가
    // ============================================

    public List<CountryTrafficResponseDTO> getTrafficByCountry(Long startTimeUnix, Long endTimeUnix, DashboardRequestDTO.DashboardFilters filters) {
        String whereClause = buildWhereClause(filters);

        String sql = """
            SELECT 
                TO_TIMESTAMP(FLOOR(ts_server_nsec / 300) * 300) as timestamp,
                COALESCE(NULLIF(TRIM(country_name_req), ''), 'Unknown') as country,
                ROUND(AVG(ts_page_res)::numeric, 2) as avg_response_time,
                COUNT(*) as request_count
            FROM http_page_sample
            WHERE ts_server_nsec BETWEEN ? AND ?
                AND ts_page_res IS NOT NULL
                AND ts_page_res > 0
            """ + whereClause + """
            GROUP BY FLOOR(ts_server_nsec / 300), COALESCE(NULLIF(TRIM(country_name_req), ''), 'Unknown')
            ORDER BY timestamp, request_count DESC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                        new CountryTrafficResponseDTO(
                                rs.getTimestamp("timestamp"),
                                rs.getString("country"),
                                Double.valueOf(rs.getDouble("avg_response_time")),
                                Long.valueOf(rs.getLong("request_count"))
                        )
                , startTimeUnix, endTimeUnix);
    }

    // ============================================
    // 위젯 7: 에러 페이지 Top 10
    // - 와이어프레임 위젯 7: "에러 페이지 Top 10"
    // - 역할: HTTP 응답 코드 400 이상(http_res_code >= 400)인 요청을 대상으로
    //         URI별 에러 발생 건수와 평균 응답 시간을 집계해
    //         에러가 많이 발생하는 페이지 상위 10개를 보여주는 데이터 제공
    // - ⭐ 수정: 5분 단위 timestamp 추가
    // ============================================

    public List<ErrorPageResponseDTO> getErrorPages(Long startTimeUnix, Long endTimeUnix, DashboardRequestDTO.DashboardFilters filters) {
        String whereClause = buildWhereClause(filters);

        String sql = """
            SELECT
                TO_TIMESTAMP(FLOOR(ts_server_nsec / 300) * 300) as timestamp,
                http_uri,
                http_res_code::integer as http_res_code,
                COUNT(*) as error_count,
                ROUND(AVG(ts_page_res)::numeric, 2) as avg_response_time
            FROM http_page_sample
            WHERE ts_server_nsec BETWEEN ? AND ?
                AND http_res_code::integer >= 400
                AND http_uri IS NOT NULL
                AND http_uri != ''
            """ + whereClause + """
            GROUP BY FLOOR(ts_server_nsec / 300), http_uri, http_res_code
            ORDER BY timestamp, error_count DESC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            int resCode = rs.getInt("http_res_code");
            String severity = resCode >= 500 ? "high" : "medium";

            return new ErrorPageResponseDTO(
                    rs.getTimestamp("timestamp"),
                    rs.getString("http_uri"),
                    Integer.valueOf(resCode),
                    Long.valueOf(rs.getLong("error_count")),
                    Double.valueOf(rs.getDouble("avg_response_time")),
                    severity
            );
        }, startTimeUnix, endTimeUnix);
    }

    // ============================================
    // 위젯 9: 브라우저별 성능
    // - 와이어프레임 위젯 9: "브라우저별 성능"
    // - 역할: 브라우저 이름(user_agent_software_name)별 페이지 로드 시간(ts_page)과
    //         응답 시간(ts_page_res), 요청 수를 집계해
    //         브라우저별 성능 비교 바 차트에 사용하는 데이터 제공
    // - ⭐ 수정: 5분 단위 timestamp 추가
    // ============================================

    public List<BrowserPerfResponseDTO> getBrowserPerformance(Long startTimeUnix, Long endTimeUnix, DashboardRequestDTO.DashboardFilters filters) {
        String whereClause = buildWhereClause(filters);

        String sql = """
            SELECT 
                TO_TIMESTAMP(FLOOR(ts_server_nsec / 300) * 300) as timestamp,
                user_agent_software_name as browser,
                ROUND(AVG(ts_page)::numeric, 2) as avg_page_load_time,
                ROUND(AVG(ts_page_res)::numeric, 2) as avg_response_time,
                COUNT(*) as request_count
            FROM http_page_sample
            WHERE ts_server_nsec BETWEEN ? AND ?
                AND user_agent_software_name IS NOT NULL
                AND user_agent_software_name != ''
                AND ts_page IS NOT NULL
                AND ts_page > 0
            """ + whereClause + """
            GROUP BY FLOOR(ts_server_nsec / 300), user_agent_software_name
            ORDER BY timestamp, request_count DESC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                        new BrowserPerfResponseDTO(
                                rs.getTimestamp("timestamp"),
                                rs.getString("browser"),
                                Double.valueOf(rs.getDouble("avg_page_load_time")),
                                Double.valueOf(rs.getDouble("avg_response_time")),
                                Long.valueOf(rs.getLong("request_count"))
                        )
                , startTimeUnix, endTimeUnix);
    }

    // ============================================
    // 위젯 12: 디바이스별 성능 분포
    // - 와이어프레임 위젯 12: "디바이스별 성능 분포"
    // - 역할: 디바이스 타입(user_agent_hardware_type)별 요청 수, 평균 페이지 로드/응답 시간,
    //         평균 페이지 크기(page_http_len)를 계산해
    //         Desktop/Mobile/Tablet 성능 비교 차트에 사용하는 데이터 제공
    // - ⭐ 수정: 5분 단위 timestamp 추가
    // ============================================

    public List<DevicePerfResponseDTO> getDevicePerformanceDistribution(Long startTimeUnix, Long endTimeUnix, DashboardRequestDTO.DashboardFilters filters) {
        String whereClause = buildWhereClause(filters);

        String sql = """
            SELECT 
                TO_TIMESTAMP(FLOOR(ts_server_nsec / 300) * 300) as timestamp,
                COALESCE(user_agent_hardware_type, 'Unknown') as device_type,
                COUNT(*) as request_count,
                ROUND(AVG(ts_page)::numeric, 2) as avg_page_load_time,
                ROUND(AVG(ts_page_res)::numeric, 2) as avg_response_time,
                ROUND(AVG(page_http_len)::numeric, 0) as avg_page_size
            FROM http_page_sample
            WHERE ts_server_nsec BETWEEN ? AND ?
                AND ts_page IS NOT NULL
                AND ts_page > 0
            """ + whereClause + """
            GROUP BY FLOOR(ts_server_nsec / 300), COALESCE(user_agent_hardware_type, 'Unknown')
            ORDER BY timestamp, request_count DESC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                        new DevicePerfResponseDTO(
                                rs.getTimestamp("timestamp"),
                                rs.getString("device_type"),
                                Long.valueOf(rs.getLong("request_count")),
                                null,  // traffic_percentage는 프론트에서 계산
                                Double.valueOf(rs.getDouble("avg_page_load_time")),
                                Double.valueOf(rs.getDouble("avg_response_time")),
                                Long.valueOf(rs.getLong("avg_page_size"))
                        )
                , startTimeUnix, endTimeUnix);
    }

    // ============================================
    // 기존 메서드 (필요 시 유지)
    // ============================================

    /**
     * 위젯 4: TCP 에러율 게이지
     * - 와이어프레임 위젯 4 "TCP 에러율 게이지"에서 사용하는 데이터 소스
     * - 재전송, 순서 오류, 손실 패킷(retransmission_cnt, out_of_order_cnt, lost_seg_cnt)을 합산해
     *   전체 패킷 대비 TCP 에러율 및 상세 비율을 계산
     */
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

    /**
     * 지역별 트래픽 Top N 조회 (지도/필터 서브 쿼리)
     * - 지역별 응답 시간 히트맵 위젯과 국가 필터에서 재사용 가능한
     *   국가 단위 트래픽, 요청 수, 비율 데이터를 제공
     */
    public List<Map<String, Object>> getTrafficByCountries(
            Long startTimeUnix,
            Long endTimeUnix,
            List<String> countryNames) {

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

        List<Object> params = new ArrayList<>();
        params.add(startTimeUnix);
        params.add(endTimeUnix);
        params.addAll(countryNames);

        return jdbcTemplate.queryForList(sql, params.toArray());
    }


    // ============================================
    // 필터 옵션 조회 메서드들
    // ============================================
    /**
     * 사용 가능한 국가 목록 조회
     */
    public List<String> getAvailableCountries(Long startTimeUnix, Long endTimeUnix) {
        String sql = """
        SELECT DISTINCT COALESCE(NULLIF(TRIM(country_name_req), ''), 'Unknown') as country
        FROM http_page_sample
        WHERE ts_server_nsec BETWEEN ? AND ?
            AND country_name_req IS NOT NULL
        ORDER BY country
        LIMIT 50
        """;

        return jdbcTemplate.queryForList(sql, String.class, startTimeUnix, endTimeUnix);
    }

    /**
     * 사용 가능한 브라우저 목록 조회
     */
    public List<String> getAvailableBrowsers(Long startTimeUnix, Long endTimeUnix) {
        String sql = """
        SELECT DISTINCT user_agent_software_name as browser
        FROM http_page_sample
        WHERE ts_server_nsec BETWEEN ? AND ?
            AND user_agent_software_name IS NOT NULL
            AND user_agent_software_name != ''
        ORDER BY browser
        LIMIT 20
        """;

        return jdbcTemplate.queryForList(sql, String.class, startTimeUnix, endTimeUnix);
    }

    /**
     * 사용 가능한 디바이스 타입 목록 조회
     */
    public List<String> getAvailableDevices(Long startTimeUnix, Long endTimeUnix) {
        String sql = """
        SELECT DISTINCT COALESCE(user_agent_hardware_type, 'Unknown') as device
        FROM http_page_sample
        WHERE ts_server_nsec BETWEEN ? AND ?
            AND user_agent_hardware_type IS NOT NULL
            AND user_agent_hardware_type != ''
        ORDER BY device
        """;

        return jdbcTemplate.queryForList(sql, String.class, startTimeUnix, endTimeUnix);
    }

    /**
     * 사용 가능한 HTTP 호스트 목록 조회 (상위 30개)
     */
    public List<String> getAvailableHttpHosts(Long startTimeUnix, Long endTimeUnix) {
        String sql = """
        SELECT http_host
        FROM http_page_sample
        WHERE ts_server_nsec BETWEEN ? AND ?
            AND http_host IS NOT NULL
            AND http_host != ''
        GROUP BY http_host
        ORDER BY COUNT(*) DESC
        LIMIT 30
        """;

        return jdbcTemplate.queryForList(sql, String.class, startTimeUnix, endTimeUnix);
    }

    /**
     * 사용 가능한 HTTP Method 목록 조회
     */
    public List<String> getAvailableHttpMethods(Long startTimeUnix, Long endTimeUnix) {
        String sql = """
        SELECT DISTINCT http_method
        FROM http_page_sample
        WHERE ts_server_nsec BETWEEN ? AND ?
            AND http_method IS NOT NULL
            AND http_method != ''
        ORDER BY http_method
        """;

        return jdbcTemplate.queryForList(sql, String.class, startTimeUnix, endTimeUnix);
    }

    /**
     * WHERE 절 필터 조건 빌더
     */
    private String buildWhereClause(DashboardRequestDTO.DashboardFilters filters) {
        if (filters == null) {
            return "";
        }

        StringBuilder where = new StringBuilder();

        // 국가 필터
        if (filters.getCountries() != null && !filters.getCountries().isEmpty()) {
            where.append(" AND country_name_req IN (");
            where.append(String.join(",", filters.getCountries().stream()
                    .map(c -> "'" + c.replace("'", "''") + "'")
                    .toList()));
            where.append(")");
        }

        // 브라우저 필터
        if (filters.getBrowsers() != null && !filters.getBrowsers().isEmpty()) {
            where.append(" AND user_agent_software_name IN (");
            where.append(String.join(",", filters.getBrowsers().stream()
                    .map(b -> "'" + b.replace("'", "''") + "'")
                    .toList()));
            where.append(")");
        }

        // 디바이스 필터
        if (filters.getDevices() != null && !filters.getDevices().isEmpty()) {
            where.append(" AND user_agent_hardware_type IN (");
            where.append(String.join(",", filters.getDevices().stream()
                    .map(d -> "'" + d.replace("'", "''") + "'")
                    .toList()));
            where.append(")");
        }

        // HTTP 호스트 필터
        if (filters.getHttpHost() != null && !filters.getHttpHost().isBlank()) {
            where.append(" AND http_host LIKE '%").append(filters.getHttpHost().replace("'", "''")).append("%'");
        }

        // HTTP URI 필터
        if (filters.getHttpUri() != null && !filters.getHttpUri().isBlank()) {
            where.append(" AND http_uri LIKE '%").append(filters.getHttpUri().replace("'", "''")).append("%'");
        }

        // HTTP Method 필터
        if (filters.getHttpMethods() != null && !filters.getHttpMethods().isEmpty()) {
            where.append(" AND http_method IN (");
            where.append(String.join(",", filters.getHttpMethods().stream()
                    .map(m -> "'" + m.replace("'", "''") + "'")
                    .toList()));
            where.append(")");
        }

        // HTTP 응답 코드 필터
        if (filters.getHttpResCode() != null) {
            var filter = filters.getHttpResCode();
            where.append(" AND http_res_code::integer ").append(filter.getOperator()).append(" ").append(filter.getValue());
        }

        // 응답 시간 필터
        if (filters.getResponseTime() != null) {
            var filter = filters.getResponseTime();
            where.append(" AND ts_page_res ").append(filter.getOperator()).append(" ").append(filter.getValue());
        }

        // 페이지 로드 시간 필터
        if (filters.getPageLoadTime() != null) {
            var filter = filters.getPageLoadTime();
            where.append(" AND ts_page ").append(filter.getOperator()).append(" ").append(filter.getValue());
        }

        return where.toString();
    }

}