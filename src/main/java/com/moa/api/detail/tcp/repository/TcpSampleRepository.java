package com.moa.api.detail.tcp.repository;

import com.moa.api.detail.tcp.entity.TcpSample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TcpSampleRepository extends JpaRepository<TcpSample, String> {

    @Query(value = """
            select
              row_key  as rowKey,
              ts_sample_begin as tsSampleBegin,
              ts_sample_end   as tsSampleEnd,
            
              len_delta  as lenDelta,
              len_req_delta as lenReqDelta,
              len_res_delta as lenResDelta,
            
              pkts_delta    as pktsDelta,
              pkts_req_delta as pktsReqDelta,
              pkts_res_delta as pktsResDelta,
            
              retransmission_len_delta as retransmissionLenDelta,
              retransmission_cnt_delta as retransmissionCntDelta,
              out_of_order_cnt_delta   as outOfOrderCntDelta,
              lost_seg_cnt_delta       as lostSegCntDelta,
              checksum_error_cnt_delta as checksumErrorCntDelta,
              zero_win_cnt_delta       as zeroWinCntDelta,
              window_full_cnt_delta    as windowFullCntDelta,
              win_update_cnt_delta     as winUpdateCntDelta,
            
              tcp_flag_stat_syn_delta      as tcpFlagStatSynDelta,
              tcp_flag_stat_synack_delta   as tcpFlagStatSynackDelta,
              tcp_flag_stat_fin_req_delta  as tcpFlagStatFinReqDelta,
              tcp_flag_stat_fin_res_delta  as tcpFlagStatFinResDelta,
              tcp_flag_stat_finack_req_delta as tcpFlagStatFinackReqDelta,
              tcp_flag_stat_finack_res_delta as tcpFlagStatFinackResDelta,
              tcp_flag_stat_rst_req_delta  as tcpFlagStatRstReqDelta,
              tcp_flag_stat_rst_res_delta  as tcpFlagStatRstResDelta,
              tcp_flag_stat_psh_req_delta  as tcpFlagStatPshReqDelta,
              tcp_flag_stat_psh_res_delta  as tcpFlagStatPshResDelta,
            
              -- inet → String 매핑을 위해 캐스팅
              src_ip::text as srcIp,
              dst_ip::text as dstIp,
              src_port     as srcPort,
              dst_port     as dstPort,
            
              ndpi_protocol_app    as ndpiProtocolApp,
              ndpi_protocol_master as ndpiProtocolMaster,
              sni_hostname         as sniHostname
            from tcp_sample
            where row_key = :rowKey
            """, nativeQuery = true)
    Optional<TcpRowSlice> findSlice(@Param("rowKey") String rowKey);

}