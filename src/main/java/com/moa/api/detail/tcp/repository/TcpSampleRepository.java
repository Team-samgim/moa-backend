package com.moa.api.detail.tcp.repository;

import com.moa.api.detail.tcp.entity.TcpSample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * TCP Sample Repository
 * ✅ 실제 DB 컬럼에 맞춰 수정 완료
 *
 * 주요 변경사항:
 * - ts_first, ts_last 제거 (DB에 없음)
 * - 모든 실제 존재하는 컬럼만 조회
 * - 121개 컬럼 전체 매핑
 */
public interface TcpSampleRepository extends JpaRepository<TcpSample, String> {

    @Query(value = """
            select
              row_key  as rowKey,
              flow_identifier as flowIdentifier,
            
              -- 타임스탬프 (실제 존재하는 것만)
              ts_sample_begin as tsSampleBegin,
              ts_sample_end   as tsSampleEnd,
              ts_expired      as tsExpired,
              ts_server       as tsServer,
              ts_server_nsec  as tsServerNsec,
            
              -- 트래픽 통계 (전체)
              len_delta      as lenDelta,
              len_req_delta  as lenReqDelta,
              len_res_delta  as lenResDelta,
              pkts_delta     as pktsDelta,
              pkts_req_delta as pktsReqDelta,
              pkts_res_delta as pktsResDelta,
            
              -- PDU (페이로드만)
              len_pdu_delta      as lenPduDelta,
              len_pdu_req_delta  as lenPduReqDelta,
              len_pdu_res_delta  as lenPduResDelta,
              pkts_pdu_delta     as pktsPduDelta,
              pkts_pdu_req_delta as pktsPduReqDelta,
              pkts_pdu_res_delta as pktsPduResDelta,
            
              -- 재전송 (바이트)
              retransmission_len_delta     as retransmissionLenDelta,
              retransmission_len_req_delta as retransmissionLenReqDelta,
              retransmission_len_res_delta as retransmissionLenResDelta,
            
              -- 재전송 (카운트)
              retransmission_cnt_delta     as retransmissionCntDelta,
              retransmission_cnt_req_delta as retransmissionCntReqDelta,
              retransmission_cnt_res_delta as retransmissionCntResDelta,
            
              -- 순서 오류 (카운트)
              out_of_order_cnt_delta     as outOfOrderCntDelta,
              out_of_order_cnt_req_delta as outOfOrderCntReqDelta,
              out_of_order_cnt_res_delta as outOfOrderCntResDelta,
            
              -- 순서 오류 (바이트)
              out_of_order_len_delta     as outOfOrderLenDelta,
              out_of_order_len_req_delta as outOfOrderLenReqDelta,
              out_of_order_len_res_delta as outOfOrderLenResDelta,
            
              -- 패킷 손실 (카운트)
              lost_seg_cnt_delta     as lostSegCntDelta,
              lost_seg_cnt_req_delta as lostSegCntReqDelta,
              lost_seg_cnt_res_delta as lostSegCntResDelta,
            
              -- 패킷 손실 (바이트)
              lost_seg_len_delta     as lostSegLenDelta,
              lost_seg_len_req_delta as lostSegLenReqDelta,
              lost_seg_len_res_delta as lostSegLenResDelta,
            
              -- 체크섬 에러 (카운트)
              checksum_error_cnt_delta     as checksumErrorCntDelta,
              checksum_error_cnt_req_delta as checksumErrorCntReqDelta,
              checksum_error_cnt_res_delta as checksumErrorCntResDelta,
            
              -- 체크섬 에러 (바이트)
              checksum_error_len_delta     as checksumErrorLenDelta,
              checksum_error_len_req_delta as checksumErrorLenReqDelta,
              checksum_error_len_res_delta as checksumErrorLenResDelta,
            
              -- 중복 ACK (카운트)
              dup_ack_cnt_delta     as dupAckCntDelta,
              dup_ack_cnt_req_delta as dupAckCntReqDelta,
              dup_ack_cnt_res_delta as dupAckCntResDelta,
            
              -- 중복 ACK (바이트)
              dup_ack_len_delta     as dupAckLenDelta,
              dup_ack_len_req_delta as dupAckLenReqDelta,
              dup_ack_len_res_delta as dupAckLenResDelta,
            
              -- ACK 손실 (카운트)
              ack_lost_cnt_delta     as ackLostCntDelta,
              ack_lost_cnt_req_delta as ackLostCntReqDelta,
              ack_lost_cnt_res_delta as ackLostCntResDelta,
            
              -- ACK 손실 (바이트)
              ack_lost_len_delta     as ackLostLenDelta,
              ack_lost_len_req_delta as ackLostLenReqDelta,
              ack_lost_len_res_delta as ackLostLenResDelta,
            
              -- Zero Window (카운트)
              zero_win_cnt_delta      as zeroWinCntDelta,
              zero_win_cnt_req_delta  as zeroWinCntReqDelta,
              zero_win_cnt_res_delta  as zeroWinCntResDelta,
            
              -- Zero Window (바이트)
              zero_win_len_delta      as zeroWinLenDelta,
              zero_win_len_req_delta  as zeroWinLenReqDelta,
              zero_win_len_res_delta  as zeroWinLenResDelta,
            
              -- Window Full (카운트)
              window_full_cnt_delta      as windowFullCntDelta,
              window_full_cnt_req_delta  as windowFullCntReqDelta,
              window_full_cnt_res_delta  as windowFullCntResDelta,
            
              -- Window Update (카운트)
              win_update_cnt_delta      as winUpdateCntDelta,
              win_update_cnt_req_delta  as winUpdateCntReqDelta,
              win_update_cnt_res_delta  as winUpdateCntResDelta,
            
              -- Window Update (바이트)
              win_update_len_delta      as winUpdateLenDelta,
              win_update_len_req_delta  as winUpdateLenReqDelta,
              win_update_len_res_delta  as winUpdateLenResDelta,
            
              -- RTT/RTO (가장 중요!)
              ack_rtt_cnt_req as ackRttCntReq,
              ack_rtt_cnt_res as ackRttCntRes,
              ack_rto_cnt_req as ackRtoCntReq,
              ack_rto_cnt_res as ackRtoCntRes,
            
              -- TCP 플래그 (모든 플래그)
              tcp_flag_stat_syn_delta        as tcpFlagStatSynDelta,
              tcp_flag_stat_synack_delta     as tcpFlagStatSynackDelta,
              tcp_flag_stat_fin_req_delta    as tcpFlagStatFinReqDelta,
              tcp_flag_stat_fin_res_delta    as tcpFlagStatFinResDelta,
              tcp_flag_stat_finack_req_delta as tcpFlagStatFinackReqDelta,
              tcp_flag_stat_finack_res_delta as tcpFlagStatFinackResDelta,
              tcp_flag_stat_rst_req_delta    as tcpFlagStatRstReqDelta,
              tcp_flag_stat_rst_res_delta    as tcpFlagStatRstResDelta,
              tcp_flag_stat_psh_req_delta    as tcpFlagStatPshReqDelta,
              tcp_flag_stat_psh_res_delta    as tcpFlagStatPshResDelta,
              tcp_flag_stat_ack_req_delta    as tcpFlagStatAckReqDelta,
              tcp_flag_stat_ack_res_delta    as tcpFlagStatAckResDelta,
              tcp_flag_stat_urg_req_delta    as tcpFlagStatUrgReqDelta,
              tcp_flag_stat_urg_res_delta    as tcpFlagStatUrgResDelta,
              tcp_flag_stat_cwr_req_delta    as tcpFlagStatCwrReqDelta,
              tcp_flag_stat_cwr_res_delta    as tcpFlagStatCwrResDelta,
              tcp_flag_stat_ece_req_delta    as tcpFlagStatEceReqDelta,
              tcp_flag_stat_ece_res_delta    as tcpFlagStatEceResDelta,
              tcp_flag_stat_ns_req_delta     as tcpFlagStatNsReqDelta,
              tcp_flag_stat_ns_res_delta     as tcpFlagStatNsResDelta,
              tcp_flag_stat_syn_req_delta    as tcpFlagStatSynReqDelta,
              tcp_flag_stat_syn_res_delta    as tcpFlagStatSynResDelta,
            
              -- 세션 상태
              expired                 as expired,
              expired_by_timeout      as expiredByTimeout,
              session_timeout         as sessionTimeout,
              stopped_transaction     as stoppedTransaction,
              stopped_transaction_req as stoppedTransactionReq,
              stopped_transaction_res as stoppedTransactionRes,
            
              -- 엔드포인트 (inet → text 캐스팅)
              src_ip::text as srcIp,
              dst_ip::text as dstIp,
              src_port     as srcPort,
              dst_port     as dstPort,
              src_mac::text as srcMac,
              dst_mac::text as dstMac,
            
              -- 애플리케이션
              ndpi_protocol_app    as ndpiProtocolApp,
              ndpi_protocol_master as ndpiProtocolMaster,
              sni_hostname         as sniHostname,
            
              -- 지리 정보
              country_name_req         as countryNameReq,
              country_name_res         as countryNameRes,
              continent_name_req       as continentNameReq,
              continent_name_res       as continentNameRes,
              domestic_primary_name_req as domesticPrimaryNameReq,
              domestic_primary_name_res as domesticPrimaryNameRes,
              domestic_sub1_name_req    as domesticSub1NameReq,
              domestic_sub1_name_res    as domesticSub1NameRes,
              domestic_sub2_name_req    as domesticSub2NameReq,
              domestic_sub2_name_res    as domesticSub2NameRes
            
            from tcp_sample
            where row_key = :rowKey
            """, nativeQuery = true)
    Optional<TcpRowSlice> findSlice(@Param("rowKey") String rowKey);
}