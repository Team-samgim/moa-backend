package com.moa.api.detail.ethernet.repository;

import com.moa.api.detail.ethernet.entity.EthernetSample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/*****************************************************************************
 INTERFACE NAME : EthernetSampleRepository
 AUTHOR         : 방대혁
 ******************************************************************************/
public interface EthernetSampleRepository extends JpaRepository<EthernetSample, String> {

    /**
     * 단일 rowKey에 대한 Ethernet 집계 데이터 조회
     * - 상세 화면용 메트릭 계산을 위해 필요한 값들을 한 번에 가져오는 쿼리
     */
    @Query(value = """
            select
              /* 식별 / 샘플 메타 */
              row_key                                     as rowKey,
              min(flow_identifier)                        as flowIdentifier,
              min(ts_sample_begin)                        as tsSampleBegin,
              min(ts_sample_end)                          as tsSampleEnd,
              min(expired)                                as expired,
              min(ts_server)                              as tsServer,
              min(ts_server_nsec)::bigint                 as tsServerNsec,
              min(expired_by_timeout)                     as expiredByTimeout,
            
              /* 엔드포인트 */
              min(src_mac::text)                          as srcMac,
              min(dst_mac::text)                          as dstMac,
              min(src_ip::text)                           as srcIp,
              min(dst_ip::text)                           as dstIp,
              min(src_port)                               as srcPort,
              min(dst_port)                               as dstPort,
            
              /* 프로토콜 (숫자 코드) */
              min(l2_proto)                               as l2Proto,
              min(l3_proto)                               as l3Proto,
              min(l4_proto)                               as l4Proto,
              min(l7proto)                                as l7proto,
              min(ip_version)                             as ipVersion,
            
              /* 규모 (delta 합계) */
              coalesce(sum(len_delta),0)::bigint               as lenDelta,
              coalesce(sum(len_req_delta),0)::bigint           as lenReqDelta,
              coalesce(sum(len_res_delta),0)::bigint           as lenResDelta,
            
              coalesce(sum(crc_error_len_delta),0)::bigint     as crcErrorLenDelta,
              coalesce(sum(crc_error_len_req_delta),0)::bigint as crcErrorLenReqDelta,
              coalesce(sum(crc_error_len_res_delta),0)::bigint as crcErrorLenResDelta,
            
              coalesce(sum(pkts_delta),0)::bigint              as pktsDelta,
              coalesce(sum(pkts_req_delta),0)::bigint          as pktsReqDelta,
              coalesce(sum(pkts_res_delta),0)::bigint          as pktsResDelta,
            
              coalesce(sum(crc_error_cnt_delta),0)::bigint     as crcErrorCntDelta,
              coalesce(sum(crc_error_cnt_req_delta),0)::bigint as crcErrorCntReqDelta,
              coalesce(sum(crc_error_cnt_res_delta),0)::bigint as crcErrorCntResDelta,
            
              /* 패킷 길이 통계 */
              min(pkt_len_min_req)                          as pktLenMinReq,
              min(pkt_len_min_res)                          as pktLenMinRes,
              max(pkt_len_max_req)                          as pktLenMaxReq,
              max(pkt_len_max_res)                          as pktLenMaxRes,
              avg(nullif(pkt_len_avg_req,0))                as pktLenAvgReq,
              avg(nullif(pkt_len_avg_res,0))                as pktLenAvgRes,
            
              /* 타임스탬프 (duration 계산용) */
              min(ts_first)                                 as tsFirst,
              min(ts_first_req)                             as tsFirstReq,
              min(ts_first_res)                             as tsFirstRes,
              max(ts_last)                                  as tsLast,
              max(ts_last_req)                              as tsLastReq,
              max(ts_last_res)                              as tsLastRes,
            
              /* nDPI / TLS */
              min(ndpi_protocol_app)                        as ndpiProtocolApp,
              min(ndpi_protocol_master)                     as ndpiProtocolMaster,
              min(sni_hostname)                             as sniHostname,
            
              /* Geo */
              min(country_name_req)                         as countryNameReq,
              min(country_name_res)                         as countryNameRes,
              min(continent_name_req)                       as continentNameReq,
              min(continent_name_res)                       as continentNameRes,
            
              /* 국내 지역 */
              min(domestic_primary_name_req)                as domesticPrimaryNameReq,
              min(domestic_primary_name_res)                as domesticPrimaryNameRes,
              min(domestic_sub1_name_req)                   as domesticSub1NameReq,
              min(domestic_sub1_name_res)                   as domesticSub1NameRes,
              min(domestic_sub2_name_req)                   as domesticSub2NameReq,
              min(domestic_sub2_name_res)                   as domesticSub2NameRes,
            
              /* 센서 */
              min(sensor_device_name)                       as sensorDeviceName
            
            from public.ethernet_sample
            where row_key = :rowKey
            group by row_key
            """,
            nativeQuery = true)
    Optional<EthernetRowSlice> findAgg(@Param("rowKey") String rowKey);
}
