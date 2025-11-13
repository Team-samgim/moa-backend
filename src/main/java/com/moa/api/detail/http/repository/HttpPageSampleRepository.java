package com.moa.api.detail.http.repository;

import com.moa.api.detail.http.entity.HttpPageSample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface HttpPageSampleRepository extends JpaRepository<HttpPageSample, String> {

    @Query(value = """
            select
              row_key                                   as rowKey,
              min(src_ip)::text                         as srcIp,
              min(dst_ip)::text                         as dstIp,
              min(src_port)                             as srcPort,
              min(dst_port)                             as dstPort,
            
              /* 규모 합계 */
              coalesce(sum(page_http_len),0)             as bytes,
              coalesce(sum(page_http_len_req),0)         as bytesReq,
              coalesce(sum(page_http_len_res),0)         as bytesRes,
              coalesce(sum(page_pkt_cnt),0)::bigint      as pkts,
              coalesce(sum(page_pkt_cnt_req),0)::bigint  as pktsReq,
              coalesce(sum(page_pkt_cnt_res),0)::bigint  as pktsRes,
            
              /* === 추가: 헤더/바디/패킷 길이 === */
              coalesce(sum(page_http_header_len_req),0)::bigint  as pageHttpHeaderLenReq,
              coalesce(sum(page_http_header_len_res),0)::bigint  as pageHttpHeaderLenRes,
              coalesce(sum(page_http_content_len_req),0)::bigint as pageHttpContentLenReq,
              coalesce(sum(page_http_content_len_res),0)::bigint as pageHttpContentLenRes,
              coalesce(sum(page_pkt_len_req),0)::bigint          as pagePktLenReq,
              coalesce(sum(page_pkt_len_res),0)::bigint          as pagePktLenRes,
            
              /* === 추가: HTTP Content-Length (대표값) === */
              max(http_content_length_req)::bigint               as httpContentLengthReq,
              max(http_content_length)::bigint                   as httpContentLengthRes,
            
              /* 타이밍(집계) */
              max(ts_page)                               as durSec,
              avg(nullif(ts_page_tcp_connect_avg,0))     as connectAvg,
              min(nullif(ts_page_tcp_connect_min,0))     as connectMin,
              max(ts_page_tcp_connect_max)               as connectMax,
              avg(nullif(ts_page_req_making_avg,0))      as reqMakingAvg,
              sum(coalesce(ts_page_req_making_sum,0))    as reqMakingSum,
              avg(nullif(ts_page_res_init,0))            as resInit,
              avg(nullif(ts_page_res_init_gap,0))        as resInitGap,
              avg(nullif(ts_page_res_app,0))             as resApp,
              avg(nullif(ts_page_res_app_gap,0))         as resAppGap,
              avg(nullif(ts_page_transfer_req,0))        as transferReq,
              avg(nullif(ts_page_transfer_req_gap,0))    as transferReqGap,
              avg(nullif(ts_page_transfer_res,0))        as transferRes,
              avg(nullif(ts_page_transfer_res_gap,0))    as transferResGap,
            
              /* 품질 카운트 합계 */
              coalesce(sum(retransmission_cnt),0)::bigint as retransmissionCnt,
              coalesce(sum(out_of_order_cnt),0)::bigint   as outOfOrderCnt,
              coalesce(sum(lost_seg_cnt),0)::bigint       as lostSegCnt,
              coalesce(sum(tcp_error_cnt),0)::bigint      as checksumErrorCnt,
              coalesce(sum(dup_ack_cnt),0)::bigint        as dupAckCnt,
              coalesce(sum(win_update_cnt),0)::bigint     as winUpdateCnt,
              coalesce(sum(zero_win_cnt),0)::bigint       as zeroWinCnt,
              coalesce(sum(window_full_cnt),0)::bigint    as windowFullCnt,
              coalesce(sum(ack_lost_cnt),0)::bigint       as ackLostCnt,
            
              /* HTTP 분포 */
              coalesce(sum(res_code_1xx_cnt),0)::int      as resCode1xxCnt,
              coalesce(sum(res_code_2xx_cnt),0)::int      as resCode2xxCnt,
              coalesce(sum(res_code_3xx_cnt),0)::int      as resCode3xxCnt,
              coalesce(sum(res_code_304_cnt),0)::int      as resCode304Cnt,
              coalesce(sum(res_code_4xx_cnt),0)::int      as resCode4xxCnt,
              coalesce(sum(res_code_5xx_cnt),0)::int      as resCode5xxCnt,
            
              coalesce(sum(req_method_get_cnt),0)::int     as reqMethodGetCnt,
              coalesce(sum(req_method_post_cnt),0)::int    as reqMethodPostCnt,
              coalesce(sum(req_method_put_cnt),0)::int     as reqMethodPutCnt,
              coalesce(sum(req_method_delete_cnt),0)::int  as reqMethodDeleteCnt,
              coalesce(sum(req_method_head_cnt),0)::int    as reqMethodHeadCnt,
              coalesce(sum(req_method_options_cnt),0)::int as reqMethodOptionsCnt,
              coalesce(sum(req_method_patch_cnt),0)::int   as reqMethodPatchCnt,
              coalesce(sum(req_method_trace_cnt),0)::int   as reqMethodTraceCnt,
              coalesce(sum(req_method_connect_cnt),0)::int as reqMethodConnectCnt,
              coalesce(sum(req_method_oth_cnt),0)::int     as reqMethodOthCnt,
            
              min(http_version)                           as httpVersion,
              min(http_version_req)                       as httpVersionReq,
              min(http_version_res)                       as httpVersionRes,
            
              coalesce(sum(content_type_html_cnt_req),0)::int as contentTypeHtmlCntReq,
              coalesce(sum(content_type_html_cnt_res),0)::int as contentTypeHtmlCntRes,
              coalesce(sum(content_type_css_cnt_req),0)::int  as contentTypeCssCntReq,
              coalesce(sum(content_type_css_cnt_res),0)::int  as contentTypeCssCntRes,
              coalesce(sum(content_type_js_cnt_req),0)::int   as contentTypeJsCntReq,
              coalesce(sum(content_type_js_cnt_res),0)::int   as contentTypeJsCntRes,
              coalesce(sum(content_type_img_cnt_req),0)::int  as contentTypeImgCntReq,
              coalesce(sum(content_type_img_cnt_res),0)::int  as contentTypeImgCntRes,
              coalesce(sum(content_type_oth_cnt_req),0)::int  as contentTypeOthCntReq,
              coalesce(sum(content_type_oth_cnt_res),0)::int  as contentTypeOthCntRes,
            
              /* 상태성 배지 */
              coalesce(sum(timeout_cnt),0)::int            as timeoutCnt,
              coalesce(sum(incomplete_cnt),0)::int         as incompleteCnt,
              coalesce(sum(stopped_transaction_cnt),0)::int as stoppedTransactionCnt,
            
              /* 대표 HTTP/식별 */
              min(http_method)                             as httpMethod,
              min(http_res_code)                           as httpResCode,
              min(http_res_phrase)                         as httpResPhrase,
              min(http_host)                               as httpHost,
              min(http_uri)                                as httpUri,
              max(coalesce(is_https,0))                    as isHttps,
            
              min(ndpi_protocol_app)                       as ndpiProtocolApp,
              min(ndpi_protocol_master)                    as ndpiProtocolMaster,
            
              /* Request 헤더/메타 */
              min(http_referer)                            as httpReferer,
              min(http_cookie)                             as httpCookie,
              min(http_user_agent)                         as httpUserAgent,
              min(http_content_type)                       as httpContentType,
            
              /* 추가: Geo */
              min(country_name_req)                        as countryNameReq,
              min(country_name_res)                        as countryNameRes,
              min(continent_name_req)                      as continentNameReq,
              min(continent_name_res)                      as continentNameRes,
            
              /* UA 파싱 */
              min(user_agent_software_name)                as userAgentSoftwareName,
              min(user_agent_operating_system_name)        as userAgentOperatingSystemName,
              min(user_agent_hardware_type)                as userAgentHardwareType,
              min(user_agent_layout_engine_name)           as userAgentLayoutEngineName
            
            from public.http_page_sample
            where row_key = :rowKey
            group by row_key
            """, nativeQuery = true)
    Optional<HttpPageAgg> findAgg(@Param("rowKey") String rowKey);
}