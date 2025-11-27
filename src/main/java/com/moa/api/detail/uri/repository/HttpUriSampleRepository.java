package com.moa.api.detail.uri.repository;

/*****************************************************************************
 INTERFACE NAME : HttpUriSampleRepository
 DESCRIPTION    : HTTP URI 샘플 데이터 Repository (http_uri_sample 단건 조회)
 AUTHOR         : 방대혁
 ******************************************************************************/
/**
 * HTTP URI Repository
 * 실제 CSV 파일의 191개 컬럼 전체 매핑
 *
 * 주요 특징:
 * - HTTP 요청/응답 완전 분석
 * - TCP 에러 분석 (재전송, 손실, 순서 오류 등)
 * - 성능 메트릭 (Mbps, PPS, 지연시간)
 * - User Agent 파싱
 * - 지리 정보
 */

import com.moa.api.detail.uri.entity.HttpUriSample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface HttpUriSampleRepository extends JpaRepository<HttpUriSample, String> {

    @Query(value = """
            select
              row_key as rowKey,
              
              -- 엔드포인트
              src_ip::text as srcIp,
              dst_ip::text as dstIp,
              src_port as srcPort,
              dst_port as dstPort,
              src_mac::text as srcMac,
              dst_mac::text as dstMac,
              
              -- 타임스탬프
              ts_frame_arrival as tsFrameArrival,
              ts_frame_landoff as tsFrameLandoff,
              ts_server as tsServer,
              ts_server_nsec as tsServerNsec,
              ts_first as tsFirst,
              
              -- 요청/응답 시간 분석
              ts_req_pkt_first as tsReqPktFirst,
              ts_req_pkt_push as tsReqPktPush,
              ts_req_pkt_last as tsReqPktLast,
              ts_res_pkt_first as tsResPktFirst,
              ts_res_pkt_push as tsResPktPush,
              ts_res_pkt_last as tsResPktLast,
              
              -- 지연 시간
              ts_res_delay_transfer as tsResDelayTransfer,
              ts_rsq_delay_response as tsRsqDelayResponse,
              ts_res_process_first as tsResProcessFirst,
              ts_res_process_push as tsResProcessPush,
              ts_res_process_last as tsResProcessLast,
              ts_req_delay_transfer as tsReqDelayTransfer,
              
              -- 페이지/URI 인덱스
              page_idx as pageIdx,
              uri_idx as uriIdx,
              
              -- 패킷 길이 (전체)
              pkt_len as pktLen,
              pkt_len_req as pktLenReq,
              pkt_len_res as pktLenRes,
              
              -- HTTP 길이
              http_len as httpLen,
              http_len_req as httpLenReq,
              http_len_res as httpLenRes,
              http_header_len_req as httpHeaderLenReq,
              http_header_len_res as httpHeaderLenRes,
              http_content_len_req as httpContentLenReq,
              http_content_len_res as httpContentLenRes,
              
              -- TCP 길이
              tcp_len as tcpLen,
              tcp_len_req as tcpLenReq,
              tcp_len_res as tcpLenRes,
              
              -- 재전송 (바이트)
              retransmission_len as retransmissionLen,
              retransmission_len_req as retransmissionLenReq,
              retransmission_len_res as retransmissionLenRes,
              
              -- 빠른 재전송 (바이트)
              fast_retransmission_len as fastRetransmissionLen,
              fast_retransmission_len_req as fastRetransmissionLenReq,
              fast_retransmission_len_res as FastRetransmissionLenRes,
              
              -- 순서 오류 (바이트)
              out_of_order_len as outOfOrderLen,
              out_of_order_len_req as outOfOrderLenReq,
              out_of_order_len_res as outOfOrderLenRes,
              
              -- 패킷 손실 (바이트)
              lost_seg_len as lostSegLen,
              lost_seg_len_req as lostSegLenReq,
              lost_seg_len_res as lostSegLenRes,
              
              -- ACK 손실 (바이트)
              ack_lost_len as ackLostLen,
              ack_lost_len_req as ackLostLenReq,
              ack_lost_len_res as ackLostLenRes,
              
              -- Window Update (바이트)
              win_update_len as winUpdateLen,
              win_update_len_req as winUpdateLenReq,
              win_update_len_res as winUpdateLenRes,
              
              -- 중복 ACK (바이트)
              dup_ack_len as dupAckLen,
              dup_ack_len_req as dupAckLenReq,
              dup_ack_len_res as dupAckLenRes,
              
              -- Zero Window (바이트)
              zero_win_len as zeroWinLen,
              zero_win_len_req as zeroWinLenReq,
              zero_win_len_res as zeroWinLenRes,
              
              -- 체크섬 에러 (바이트)
              checksum_error_len as checksumErrorLen,
              checksum_error_len_req as checksumErrorLenReq,
              checksum_error_len_res as checksumErrorLenRes,
              
              -- 패킷 카운트
              pkt_cnt as pktCnt,
              pkt_cnt_req as pktCntReq,
              pkt_cnt_res as pktCntRes,
              
              -- HTTP 카운트
              http_cnt as httpCnt,
              http_cnt_req as httpCntReq,
              http_cnt_res as httpCntRes,
              
              -- RTT/RTO
              ack_rtt_cnt_req as ackRttCntReq,
              ack_rtt_cnt_res as ackRttCntRes,
              ack_rto_cnt_req as ackRtoCntReq,
              ack_rto_cnt_res as ackRtoCntRes,
              
              -- 재전송 카운트
              retransmission_cnt as retransmissionCnt,
              retransmission_cnt_req as retransmissionCntReq,
              retransmission_cnt_res as retransmissionCntRes,
              
              -- 순서 오류 카운트
              out_of_order_cnt as outOfOrderCnt,
              out_of_order_cnt_req as outOfOrderCntReq,
              out_of_order_cnt_res as outOfOrderCntRes,
              
              -- 패킷 손실 카운트
              lost_seg_cnt as lostSegCnt,
              lost_seg_cnt_req as lostSegCntReq,
              lost_seg_cnt_res as lostSegCntRes,
              
              -- ACK 손실 카운트
              ack_lost_cnt as ackLostCnt,
              ack_lost_cnt_req as ackLostCntReq,
              ack_lost_cnt_res as ackLostCntRes,
              
              -- Window Update 카운트
              win_update_cnt as winUpdateCnt,
              win_update_cnt_req as winUpdateCntReq,
              win_update_cnt_res as winUpdateCntRes,
              
              -- 중복 ACK 카운트
              dup_ack_cnt as dupAckCnt,
              dup_ack_cnt_req as dupAckCntReq,
              dup_ack_cnt_res as dupAckCntRes,
              
              -- Zero Window 카운트
              zero_win_cnt as zeroWinCnt,
              zero_win_cnt_req as zeroWinCntReq,
              zero_win_cnt_res as zeroWinCntRes,
              
              -- Window Full 카운트
              window_full_cnt as windowFullCnt,
              window_full_cnt_req as windowFullCntReq,
              window_full_cnt_res as windowFullCntRes,
              
              -- 체크섬 에러 카운트
              checksum_error_cnt as checksumErrorCnt,
              checksum_error_cnt_req as checksumErrorCntReq,
              checksum_error_cnt_res as checksumErrorCntRes,
              
              -- TCP 카운트
              tcp_cnt as tcpCnt,
              tcp_cnt_req as tcpCntReq,
              tcp_cnt_res as tcpCntRes,
              
              -- TCP 에러
              tcp_error_cnt as tcpErrorCnt,
              tcp_error_cnt_req as tcpErrorCntReq,
              tcp_error_cnt_res as tcpErrorCntRes,
              tcp_error_len as tcpErrorLen,
              tcp_error_len_req as tcpErrorLenReq,
              tcp_error_len_res as tcpErrorLenRes,
              
              -- 요청/응답 카운트
              req_cnt as reqCnt,
              res_cnt as ResCnt,
              
              -- Content Type 카운트
              content_type_html_cnt_req as contentTypeHtmlCntReq,
              content_type_html_cnt_res as contentTypeHtmlCntRes,
              content_type_css_cnt_req as contentTypeCssCntReq,
              content_type_css_cnt_res as contentTypeCssCntRes,
              content_type_js_cnt_req as contentTypeJsCntReq,
              content_type_js_cnt_res as contentTypeJsCntRes,
              content_type_img_cnt_req as contentTypeImgCntReq,
              content_type_img_cnt_res as contentTypeImgCntRes,
              content_type_oth_cnt_req as contentTypeOthCntReq,
              content_type_oth_cnt_res as contentTypeOthCntRes,
              
              -- Referer 카운트
              referer_cnt as refererCnt,
              
              -- 상태 플래그
              is_stopped_transaction as isStoppedTransaction,
              is_stopped_transaction_req as isStoppedTransactionReq,
              is_stopped_transaction_res as isStoppedTransactionRes,
              is_incomplete as isIncomplete,
              is_incomplete_req as isIncompleteReq,
              is_incomplete_res as isIncompleteRes,
              is_timeout as isTimeout,
              is_timeout_req as isTimeoutReq,
              is_timeout_res as isTimeoutRes,
              is_https as isHttps,
              
              -- 성능 메트릭 (Mbps)
              mbps as mbps,
              mbps_req as mbpsReq,
              mbps_res as mbpsRes,
              mbps_min as mbpsMin,
              mbps_min_req as mbpsMinReq,
              mbps_min_res as mbpsMinRes,
              mbps_max as mbpsMax,
              mbps_max_req as mbpsMaxReq,
              mbps_max_res as mbpsMaxRes,
              
              -- 성능 메트릭 (PPS)
              pps as pps,
              pps_req as ppsReq,
              pps_res as ppsRes,
              pps_min as ppsMin,
              pps_min_req as ppsMinReq,
              pps_min_res as ppsMinRes,
              pps_max as ppsMax,
              pps_max_req as ppsMaxReq,
              pps_max_res as ppsMaxRes,
              
              -- TCP 에러 비율
              tcp_error_percentage as tcpErrorPercentage,
              tcp_error_percentage_req as tcpErrorPercentageReq,
              tcp_error_percentage_res as tcpErrorPercentageRes,
              tcp_error_len_percentage as tcpErrorLenPercentage,
              tcp_error_len_percentage_req as tcpErrorLenPercentageReq,
              tcp_error_len_percentage_res as tcpErrorLenPercentageRes,
              
              -- 지리 정보
              country_name_req as countryNameReq,
              country_name_res as countryNameRes,
              continent_name_req as continentNameReq,
              continent_name_res as continentNameRes,
              domestic_primary_name_req as domesticPrimaryNameReq,
              domestic_primary_name_res as domesticPrimaryNameRes,
              domestic_sub1_name_req as domesticSub1NameReq,
              domestic_sub1_name_res as domesticSub1NameRes,
              domestic_sub2_name_req as domesticSub2NameReq,
              domestic_sub2_name_res as domesticSub2NameRes,
              
              -- 센서 정보
              sensor_device_name as sensorDeviceName,
              
              -- 프로토콜
              ndpi_protocol_app as ndpiProtocolApp,
              ndpi_protocol_master as ndpiProtocolMaster,
              
              -- HTTP 정보
              http_method as httpMethod,
              http_version as httpVersion,
              http_version_req as httpVersionReq,
              http_version_res as httpVersionRes,
              http_res_phrase as httpResPhrase,
              http_content_type as httpContentType,
              http_user_agent as httpUserAgent,
              http_cookie as httpCookie,
              http_location as httpLocation,
              http_host as httpHost,
              http_uri as httpUri,
              http_uri_split as httpUriSplit,
              http_referer as httpReferer,
              
              -- User Agent 파싱
              user_agent_software_name as userAgentSoftwareName,
              user_agent_operating_system_name as userAgentOperatingSystemName,
              user_agent_operating_platform as userAgentOperatingPlatform,
              user_agent_software_type as userAgentSoftwareType,
              user_agent_hardware_type as userAgentHardwareType,
              user_agent_layout_engine_name as userAgentLayoutEngineName
              
            from http_uri_sample
            where row_key = :rowKey
            """, nativeQuery = true)
    Optional<HttpUriRowSlice> findSlice(@Param("rowKey") String rowKey);
}
