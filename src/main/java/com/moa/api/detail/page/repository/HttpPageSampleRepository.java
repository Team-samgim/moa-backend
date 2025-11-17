package com.moa.api.detail.page.repository;

import com.moa.api.detail.page.entity.HttpPageSample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * HTTP Page Repository
 * 235개 컬럼 조회
 */
@Repository
public interface HttpPageSampleRepository extends JpaRepository<HttpPageSample, String> {

    @Query(value = """
        SELECT
            -- 기본 정보 (12개)
            row_key,
            src_ip::text,
            dst_ip::text,
            src_port,
            dst_port,
            ts_frame_arrival,
            ts_frame_landoff,
            page_idx,
            ts_server,
            ts_server_nsec,
            src_mac::text,
            dst_mac::text,
            
            -- HTTP 길이 (9개)
            page_http_len,
            page_http_len_req,
            page_http_len_res,
            page_http_header_len_req,
            page_http_header_len_res,
            page_http_content_len_req,
            page_http_content_len_res,
            http_content_length,
            http_content_length_req,
            
            -- 패킷/TCP 길이 (9개)
            page_pkt_len,
            page_pkt_len_req,
            page_pkt_len_res,
            page_tcp_len,
            page_tcp_len_req,
            page_tcp_len_res,
            conn_err_session_len,
            req_conn_err_session_len,
            res_conn_err_session_len,
            
            -- TCP 에러 바이트 (24개)
            retransmission_len,
            retransmission_len_req,
            retransmission_len_res,
            out_of_order_len,
            out_of_order_len_req,
            out_of_order_len_res,
            lost_seg_len,
            lost_seg_len_req,
            lost_seg_len_res,
            ack_lost_len,
            ack_lost_len_req,
            ack_lost_len_res,
            win_update_len,
            win_update_len_req,
            win_update_len_res,
            dup_ack_len,
            dup_ack_len_req,
            dup_ack_len_res,
            zero_win_len,
            zero_win_len_req,
            zero_win_len_res,
            checksum_error_len,
            checksum_error_len_req,
            checksum_error_len_res,
            
            -- RTT/Ack 카운트 (4개)
            page_rtt_conn_cnt_req,
            page_rtt_conn_cnt_res,
            page_rtt_ack_cnt_req,
            page_rtt_ack_cnt_res,
            
            -- 페이지 카운트 (15개)
            page_req_making_cnt,
            page_http_cnt,
            page_http_cnt_req,
            page_http_cnt_res,
            page_pkt_cnt,
            page_pkt_cnt_req,
            page_pkt_cnt_res,
            page_session_cnt,
            page_tcp_connect_cnt,
            conn_err_pkt_cnt,
            conn_err_session_cnt,
            page_tcp_cnt,
            page_tcp_cnt_req,
            page_tcp_cnt_res,
            page_error_cnt,
            
            -- TCP 에러 카운트 (30개)
            retransmission_cnt,
            retransmission_cnt_req,
            retransmission_cnt_res,
            out_of_order_cnt,
            out_of_order_cnt_req,
            out_of_order_cnt_res,
            lost_seg_cnt,
            lost_seg_cnt_req,
            lost_seg_cnt_res,
            ack_lost_cnt,
            ack_lost_cnt_req,
            ack_lost_cnt_res,
            win_update_cnt,
            win_update_cnt_req,
            win_update_cnt_res,
            dup_ack_cnt,
            dup_ack_cnt_req,
            dup_ack_cnt_res,
            zero_win_cnt,
            zero_win_cnt_req,
            zero_win_cnt_res,
            window_full_cnt,
            window_full_cnt_req,
            window_full_cnt_res,
            tcp_error_cnt,
            tcp_error_cnt_req,
            tcp_error_cnt_res,
            tcp_error_len,
            tcp_error_len_req,
            tcp_error_len_res,
            
            -- HTTP 메소드 (20개)
            req_method_get_cnt,
            req_method_put_cnt,
            req_method_head_cnt,
            req_method_post_cnt,
            req_method_trace_cnt,
            req_method_delete_cnt,
            req_method_options_cnt,
            req_method_patch_cnt,
            req_method_connect_cnt,
            req_method_oth_cnt,
            req_method_get_cnt_error,
            req_method_put_cnt_error,
            req_method_head_cnt_error,
            req_method_post_cnt_error,
            req_method_trace_cnt_error,
            req_method_delete_cnt_error,
            req_method_options_cnt_error,
            req_method_patch_cnt_error,
            req_method_connect_cnt_error,
            req_method_oth_cnt_error,
            
            -- 응답 코드 (10개)
            res_code_1xx_cnt AS resCode1xxCnt,
            res_code_2xx_cnt AS resCode2xxCnt,
            res_code_304_cnt AS resCode304Cnt,
            res_code_3xx_cnt AS resCode3xxCnt,
            res_code_401_cnt AS resCode401Cnt,
            res_code_403_cnt AS resCode403Cnt,
            res_code_404_cnt AS resCode404Cnt,
            res_code_4xx_cnt AS resCode4xxCnt,
            res_code_5xx_cnt AS resCode5xxCnt,
            res_code_oth_cnt AS resCodeOthCnt,
            
            -- 트랜잭션 상태 (11개)
            stopped_transaction_cnt,
            stopped_transaction_cnt_req,
            stopped_transaction_cnt_res,
            incomplete_cnt,
            incomplete_cnt_req,
            incomplete_cnt_res,
            timeout_cnt,
            timeout_cnt_req,
            timeout_cnt_res,
            ts_page_rto_cnt_req,
            ts_page_rto_cnt_res,
            
            -- Content Type (10개)
            content_type_html_cnt_req,
            content_type_html_cnt_res,
            content_type_css_cnt_req,
            content_type_css_cnt_res,
            content_type_js_cnt_req,
            content_type_js_cnt_res,
            content_type_img_cnt_req,
            content_type_img_cnt_res,
            content_type_oth_cnt_req,
            content_type_oth_cnt_res,
            
            -- URI 카운트 (3개)
            uri_cnt,
            http_uri_cnt,
            https_uri_cnt,
            
            -- 시간 메트릭 (22개)
            ts_first,
            ts_page_begin,
            ts_page_end,
            ts_page_req_syn,
            ts_page,
            ts_page_gap,
            ts_page_res_init,
            ts_page_res_init_gap,
            ts_page_res_app,
            ts_page_res_app_gap,
            ts_page_res,
            ts_page_res_gap,
            ts_page_transfer_req,
            ts_page_transfer_req_gap,
            ts_page_transfer_res,
            ts_page_transfer_res_gap,
            ts_page_req_making_sum,
            ts_page_req_making_avg,
            ts_page_tcp_connect_sum,
            ts_page_tcp_connect_min,
            ts_page_tcp_connect_max,
            ts_page_tcp_connect_avg,
            
            -- 성능 메트릭 (18개)
            mbps,
            mbps_req,
            mbps_res,
            pps,
            pps_req,
            pps_res,
            mbps_min,
            mbps_min_req,
            mbps_min_res,
            pps_min,
            pps_min_req,
            pps_min_res,
            mbps_max,
            mbps_max_req,
            mbps_max_res,
            pps_max,
            pps_max_req,
            pps_max_res,
            
            -- 에러 비율 (4개)
            tcp_error_percentage,
            tcp_error_percentage_req,
            tcp_error_percentage_res,
            page_error_percentage,
            
            -- 지리 정보 (10개)
            country_name_req,
            country_name_res,
            continent_name_req,
            continent_name_res,
            domestic_primary_name_req,
            domestic_primary_name_res,
            domestic_sub1_name_req,
            domestic_sub1_name_res,
            domestic_sub2_name_req,
            domestic_sub2_name_res,
            
            -- HTTP 정보 (18개)
            ndpi_protocol_app,
            ndpi_protocol_master,
            sensor_device_name,
            http_method,
            http_version,
            http_version_req,
            http_version_res,
            http_res_code,
            http_res_phrase,
            http_content_type,
            http_user_agent,
            http_cookie,
            http_location,
            http_host,
            http_uri,
            http_uri_split,
            http_referer,
            is_https,
            
            -- User Agent (6개)
            user_agent_software_name,
            user_agent_operating_system_name,
            user_agent_operating_platform,
            user_agent_software_type,
            user_agent_hardware_type,
            user_agent_layout_engine_name
            
        FROM http_page_sample
        WHERE row_key = :rowKey
        """, nativeQuery = true)
    Optional<HttpPageRowSlice> findSlice(@Param("rowKey") String rowKey);
}
