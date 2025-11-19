package com.moa.api.data.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity(name = "HttpPageSampleFull")
@Table(name = "http_page_sample")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpPageSampleFull {

    @Id
    @Column(name = "row_key", nullable = false)
    private String rowKey;

    // IP & Port
    @Column(name = "src_ip", nullable = false)
    private String srcIp;

    @Column(name = "dst_ip", nullable = false)
    private String dstIp;

    @Column(name = "src_port", nullable = false)
    private Integer srcPort;

    @Column(name = "dst_port", nullable = false)
    private Integer dstPort;

    // Timestamp
    @Column(name = "ts_frame_arrival", nullable = false)
    private Double tsFrameArrival;

    @Column(name = "ts_frame_landoff", nullable = false)
    private Double tsFrameLandoff;

    @Column(name = "page_idx", nullable = false)
    private Long pageIdx;

    @Column(name = "ts_server", nullable = false)
    private LocalDateTime tsServer;

    @Column(name = "ts_server_nsec", nullable = false)
    private Double tsServerNsec;

    // MAC Address
    @Column(name = "src_mac", nullable = false, columnDefinition = "macaddr")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private String srcMac;

    @Column(name = "dst_mac", nullable = false, columnDefinition = "macaddr")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private String dstMac;

    // HTTP Length
    @Column(name = "page_http_len", nullable = false)
    private Long pageHttpLen;

    @Column(name = "page_http_len_req", nullable = false)
    private Long pageHttpLenReq;

    @Column(name = "page_http_len_res", nullable = false)
    private Long pageHttpLenRes;

    @Column(name = "page_http_header_len_req", nullable = false)
    private Long pageHttpHeaderLenReq;

    @Column(name = "page_http_header_len_res", nullable = false)
    private Long pageHttpHeaderLenRes;

    @Column(name = "page_http_content_len_req", nullable = false)
    private Long pageHttpContentLenReq;

    @Column(name = "page_http_content_len_res", nullable = false)
    private Long pageHttpContentLenRes;

    // Packet Length
    @Column(name = "page_pkt_len", nullable = false)
    private Long pagePktLen;

    @Column(name = "page_pkt_len_req", nullable = false)
    private Long pagePktLenReq;

    @Column(name = "page_pkt_len_res", nullable = false)
    private Long pagePktLenRes;

    // TCP Length
    @Column(name = "page_tcp_len", nullable = false)
    private Long pageTcpLen;

    @Column(name = "page_tcp_len_req", nullable = false)
    private Long pageTcpLenReq;

    @Column(name = "page_tcp_len_res", nullable = false)
    private Long pageTcpLenRes;

    @Column(name = "http_content_length", nullable = false)
    private Long httpContentLength;

    @Column(name = "http_content_length_req", nullable = false)
    private Long httpContentLengthReq;

    // Connection Error Session Length
    @Column(name = "conn_err_session_len", nullable = false)
    private Long connErrSessionLen;

    @Column(name = "req_conn_err_session_len", nullable = false)
    private Long reqConnErrSessionLen;

    @Column(name = "res_conn_err_session_len", nullable = false)
    private Long resConnErrSessionLen;

    // Retransmission Length
    @Column(name = "retransmission_len", nullable = false)
    private Long retransmissionLen;

    @Column(name = "retransmission_len_req", nullable = false)
    private Long retransmissionLenReq;

    @Column(name = "retransmission_len_res", nullable = false)
    private Long retransmissionLenRes;

    // Out of Order Length
    @Column(name = "out_of_order_len", nullable = false)
    private Long outOfOrderLen;

    @Column(name = "out_of_order_len_req", nullable = false)
    private Long outOfOrderLenReq;

    @Column(name = "out_of_order_len_res", nullable = false)
    private Long outOfOrderLenRes;

    // Lost Segment Length
    @Column(name = "lost_seg_len", nullable = false)
    private Long lostSegLen;

    @Column(name = "lost_seg_len_req", nullable = false)
    private Long lostSegLenReq;

    @Column(name = "lost_seg_len_res", nullable = false)
    private Long lostSegLenRes;

    // ACK Lost Length
    @Column(name = "ack_lost_len", nullable = false)
    private Long ackLostLen;

    @Column(name = "ack_lost_len_req", nullable = false)
    private Long ackLostLenReq;

    @Column(name = "ack_lost_len_res", nullable = false)
    private Long ackLostLenRes;

    // Window Update Length
    @Column(name = "win_update_len", nullable = false)
    private Long winUpdateLen;

    @Column(name = "win_update_len_req", nullable = false)
    private Long winUpdateLenReq;

    @Column(name = "win_update_len_res", nullable = false)
    private Long winUpdateLenRes;

    // Duplicate ACK Length
    @Column(name = "dup_ack_len", nullable = false)
    private Long dupAckLen;

    @Column(name = "dup_ack_len_req", nullable = false)
    private Long dupAckLenReq;

    @Column(name = "dup_ack_len_res", nullable = false)
    private Long dupAckLenRes;

    // Zero Window Length
    @Column(name = "zero_win_len", nullable = false)
    private Long zeroWinLen;

    @Column(name = "zero_win_len_req", nullable = false)
    private Long zeroWinLenReq;

    @Column(name = "zero_win_len_res", nullable = false)
    private Long zeroWinLenRes;

    // Checksum Error Length
    @Column(name = "checksum_error_len", nullable = false)
    private Long checksumErrorLen;

    @Column(name = "checksum_error_len_req", nullable = false)
    private Long checksumErrorLenReq;

    @Column(name = "checksum_error_len_res", nullable = false)
    private Long checksumErrorLenRes;

    // RTT Count
    @Column(name = "page_rtt_conn_cnt_req", nullable = false)
    private Integer pageRttConnCntReq;

    @Column(name = "page_rtt_conn_cnt_res", nullable = false)
    private Integer pageRttConnCntRes;

    @Column(name = "page_rtt_ack_cnt_req", nullable = false)
    private Integer pageRttAckCntReq;

    @Column(name = "page_rtt_ack_cnt_res", nullable = false)
    private Integer pageRttAckCntRes;

    @Column(name = "page_req_making_cnt", nullable = false)
    private Integer pageReqMakingCnt;

    // HTTP Count
    @Column(name = "page_http_cnt", nullable = false)
    private Integer pageHttpCnt;

    @Column(name = "page_http_cnt_req", nullable = false)
    private Integer pageHttpCntReq;

    @Column(name = "page_http_cnt_res", nullable = false)
    private Integer pageHttpCntRes;

    // Packet Count
    @Column(name = "page_pkt_cnt", nullable = false)
    private Integer pagePktCnt;

    @Column(name = "page_pkt_cnt_req", nullable = false)
    private Integer pagePktCntReq;

    @Column(name = "page_pkt_cnt_res", nullable = false)
    private Integer pagePktCntRes;

    // Session & Connection Count
    @Column(name = "page_session_cnt", nullable = false)
    private Long pageSessionCnt;

    @Column(name = "page_tcp_connect_cnt", nullable = false)
    private Integer pageTcpConnectCnt;

    // Connection Error Count
    @Column(name = "conn_err_pkt_cnt", nullable = false)
    private Integer connErrPktCnt;

    @Column(name = "conn_err_session_cnt", nullable = false)
    private Integer connErrSessionCnt;

    // Retransmission Count
    @Column(name = "retransmission_cnt", nullable = false)
    private Integer retransmissionCnt;

    @Column(name = "retransmission_cnt_req", nullable = false)
    private Integer retransmissionCntReq;

    @Column(name = "retransmission_cnt_res", nullable = false)
    private Integer retransmissionCntRes;

    // Out of Order Count
    @Column(name = "out_of_order_cnt", nullable = false)
    private Integer outOfOrderCnt;

    @Column(name = "out_of_order_cnt_req", nullable = false)
    private Integer outOfOrderCntReq;

    @Column(name = "out_of_order_cnt_res", nullable = false)
    private Integer outOfOrderCntRes;

    // Lost Segment Count
    @Column(name = "lost_seg_cnt", nullable = false)
    private Integer lostSegCnt;

    @Column(name = "lost_seg_cnt_req", nullable = false)
    private Integer lostSegCntReq;

    @Column(name = "lost_seg_cnt_res", nullable = false)
    private Integer lostSegCntRes;

    // ACK Lost Count
    @Column(name = "ack_lost_cnt", nullable = false)
    private Integer ackLostCnt;

    @Column(name = "ack_lost_cnt_req", nullable = false)
    private Integer ackLostCntReq;

    @Column(name = "ack_lost_cnt_res", nullable = false)
    private Integer ackLostCntRes;

    // Window Update Count
    @Column(name = "win_update_cnt", nullable = false)
    private Integer winUpdateCnt;

    @Column(name = "win_update_cnt_req", nullable = false)
    private Integer winUpdateCntReq;

    @Column(name = "win_update_cnt_res", nullable = false)
    private Integer winUpdateCntRes;

    // Duplicate ACK Count
    @Column(name = "dup_ack_cnt", nullable = false)
    private Integer dupAckCnt;

    @Column(name = "dup_ack_cnt_req", nullable = false)
    private Integer dupAckCntReq;

    @Column(name = "dup_ack_cnt_res", nullable = false)
    private Integer dupAckCntRes;

    // Zero Window Count
    @Column(name = "zero_win_cnt", nullable = false)
    private Integer zeroWinCnt;

    @Column(name = "zero_win_cnt_req", nullable = false)
    private Integer zeroWinCntReq;

    @Column(name = "zero_win_cnt_res", nullable = false)
    private Integer zeroWinCntRes;

    // Window Full Count
    @Column(name = "window_full_cnt", nullable = false)
    private Integer windowFullCnt;

    @Column(name = "window_full_cnt_req", nullable = false)
    private Integer windowFullCntReq;

    @Column(name = "window_full_cnt_res", nullable = false)
    private Integer windowFullCntRes;

    // TCP Count
    @Column(name = "page_tcp_cnt", nullable = false)
    private Integer pageTcpCnt;

    @Column(name = "page_tcp_cnt_req", nullable = false)
    private Integer pageTcpCntReq;

    @Column(name = "page_tcp_cnt_res", nullable = false)
    private Integer pageTcpCntRes;

    // Request Method Count
    @Column(name = "req_method_get_cnt", nullable = false)
    private Integer reqMethodGetCnt;

    @Column(name = "req_method_put_cnt", nullable = false)
    private Integer reqMethodPutCnt;

    @Column(name = "req_method_head_cnt", nullable = false)
    private Integer reqMethodHeadCnt;

    @Column(name = "req_method_post_cnt", nullable = false)
    private Integer reqMethodPostCnt;

    @Column(name = "req_method_trace_cnt", nullable = false)
    private Integer reqMethodTraceCnt;

    @Column(name = "req_method_delete_cnt", nullable = false)
    private Integer reqMethodDeleteCnt;

    @Column(name = "req_method_options_cnt", nullable = false)
    private Integer reqMethodOptionsCnt;

    @Column(name = "req_method_patch_cnt", nullable = false)
    private Integer reqMethodPatchCnt;

    @Column(name = "req_method_connect_cnt", nullable = false)
    private Integer reqMethodConnectCnt;

    @Column(name = "req_method_oth_cnt", nullable = false)
    private Integer reqMethodOthCnt;

    // Request Method Error Count
    @Column(name = "req_method_get_cnt_error", nullable = false)
    private Integer reqMethodGetCntError;

    @Column(name = "req_method_put_cnt_error", nullable = false)
    private Integer reqMethodPutCntError;

    @Column(name = "req_method_head_cnt_error", nullable = false)
    private Integer reqMethodHeadCntError;

    @Column(name = "req_method_post_cnt_error", nullable = false)
    private Integer reqMethodPostCntError;

    @Column(name = "req_method_trace_cnt_error", nullable = false)
    private Integer reqMethodTraceCntError;

    @Column(name = "req_method_delete_cnt_error", nullable = false)
    private Integer reqMethodDeleteCntError;

    @Column(name = "req_method_options_cnt_error", nullable = false)
    private Integer reqMethodOptionsCntError;

    @Column(name = "req_method_patch_cnt_error", nullable = false)
    private Integer reqMethodPatchCntError;

    @Column(name = "req_method_connect_cnt_error", nullable = false)
    private Integer reqMethodConnectCntError;

    @Column(name = "req_method_oth_cnt_error", nullable = false)
    private Integer reqMethodOthCntError;

    // Response Code Count
    @Column(name = "res_code_1xx_cnt", nullable = false)
    private Integer resCode1xxCnt;

    @Column(name = "res_code_2xx_cnt", nullable = false)
    private Integer resCode2xxCnt;

    @Column(name = "res_code_304_cnt", nullable = false)
    private Integer resCode304Cnt;

    @Column(name = "res_code_3xx_cnt", nullable = false)
    private Integer resCode3xxCnt;

    @Column(name = "res_code_401_cnt", nullable = false)
    private Integer resCode401Cnt;

    @Column(name = "res_code_403_cnt", nullable = false)
    private Integer resCode403Cnt;

    @Column(name = "res_code_404_cnt", nullable = false)
    private Integer resCode404Cnt;

    @Column(name = "res_code_4xx_cnt", nullable = false)
    private Integer resCode4xxCnt;

    @Column(name = "res_code_5xx_cnt", nullable = false)
    private Integer resCode5xxCnt;

    @Column(name = "res_code_oth_cnt", nullable = false)
    private Integer resCodeOthCnt;

    // Transaction Count
    @Column(name = "stopped_transaction_cnt", nullable = false)
    private Integer stoppedTransactionCnt;

    @Column(name = "stopped_transaction_cnt_req", nullable = false)
    private Integer stoppedTransactionCntReq;

    @Column(name = "stopped_transaction_cnt_res", nullable = false)
    private Integer stoppedTransactionCntRes;

    // Incomplete Count
    @Column(name = "incomplete_cnt", nullable = false)
    private Integer incompleteCnt;

    @Column(name = "incomplete_cnt_req", nullable = false)
    private Integer incompleteCntReq;

    @Column(name = "incomplete_cnt_res", nullable = false)
    private Integer incompleteCntRes;

    // Timeout Count
    @Column(name = "timeout_cnt", nullable = false)
    private Integer timeoutCnt;

    @Column(name = "timeout_cnt_req", nullable = false)
    private Integer timeoutCntReq;

    @Column(name = "timeout_cnt_res", nullable = false)
    private Integer timeoutCntRes;

    // RTO Count
    @Column(name = "ts_page_rto_cnt_req", nullable = false)
    private Integer tsPageRtoCntReq;

    @Column(name = "ts_page_rto_cnt_res", nullable = false)
    private Integer tsPageRtoCntRes;

    // TCP Error
    @Column(name = "tcp_error_cnt", nullable = false)
    private Integer tcpErrorCnt;

    @Column(name = "tcp_error_cnt_req", nullable = false)
    private Integer tcpErrorCntReq;

    @Column(name = "tcp_error_cnt_res", nullable = false)
    private Integer tcpErrorCntRes;

    @Column(name = "tcp_error_len", nullable = false)
    private Long tcpErrorLen;

    @Column(name = "tcp_error_len_req", nullable = false)
    private Long tcpErrorLenReq;

    @Column(name = "tcp_error_len_res", nullable = false)
    private Long tcpErrorLenRes;

    // Page Error
    @Column(name = "page_error_cnt", nullable = false)
    private Integer pageErrorCnt;

    // URI Count
    @Column(name = "uri_cnt", nullable = false)
    private Integer uriCnt;

    @Column(name = "http_uri_cnt", nullable = false)
    private Integer httpUriCnt;

    @Column(name = "https_uri_cnt", nullable = false)
    private Integer httpsUriCnt;

    // Content Type Count
    @Column(name = "content_type_html_cnt_req", nullable = false)
    private Integer contentTypeHtmlCntReq;

    @Column(name = "content_type_html_cnt_res", nullable = false)
    private Integer contentTypeHtmlCntRes;

    @Column(name = "content_type_css_cnt_req", nullable = false)
    private Integer contentTypeCssCntReq;

    @Column(name = "content_type_css_cnt_res", nullable = false)
    private Integer contentTypeCssCntRes;

    @Column(name = "content_type_js_cnt_req", nullable = false)
    private Integer contentTypeJsCntReq;

    @Column(name = "content_type_js_cnt_res", nullable = false)
    private Integer contentTypeJsCntRes;

    @Column(name = "content_type_img_cnt_req", nullable = false)
    private Integer contentTypeImgCntReq;

    @Column(name = "content_type_img_cnt_res", nullable = false)
    private Integer contentTypeImgCntRes;

    @Column(name = "content_type_oth_cnt_req", nullable = false)
    private Integer contentTypeOthCntReq;

    @Column(name = "content_type_oth_cnt_res", nullable = false)
    private Integer contentTypeOthCntRes;

    // HTTP Response Code & HTTPS
    @Column(name = "http_res_code", nullable = false)
    private String httpResCode;

    @Column(name = "is_https", nullable = false)
    private Integer isHttps;

    // Timing Information (Double for milliseconds precision)
    @Column(name = "ts_first", nullable = false)
    private Double tsFirst;

    @Column(name = "ts_page_begin", nullable = false)
    private Double tsPageBegin;

    @Column(name = "ts_page_end", nullable = false)
    private Double tsPageEnd;

    @Column(name = "ts_page_req_syn", nullable = false)
    private Double tsPageReqSyn;

    @Column(name = "ts_page", nullable = false)
    private Double tsPage;

    @Column(name = "ts_page_gap", nullable = false)
    private Double tsPageGap;

    @Column(name = "ts_page_res_init", nullable = false)
    private Double tsPageResInit;

    @Column(name = "ts_page_res_init_gap", nullable = false)
    private Double tsPageResInitGap;

    @Column(name = "ts_page_res_app", nullable = false)
    private Double tsPageResApp;

    @Column(name = "ts_page_res_app_gap", nullable = false)
    private Double tsPageResAppGap;

    @Column(name = "ts_page_res", nullable = false)
    private Double tsPageRes;

    @Column(name = "ts_page_res_gap", nullable = false)
    private Double tsPageResGap;

    @Column(name = "ts_page_transfer_req", nullable = false)
    private Double tsPageTransferReq;

    @Column(name = "ts_page_transfer_req_gap", nullable = false)
    private Double tsPageTransferReqGap;

    @Column(name = "ts_page_transfer_res", nullable = false)
    private Double tsPageTransferRes;

    @Column(name = "ts_page_transfer_res_gap", nullable = false)
    private Double tsPageTransferResGap;

    @Column(name = "ts_page_req_making_sum", nullable = false)
    private Double tsPageReqMakingSum;

    @Column(name = "ts_page_req_making_avg", nullable = false)
    private Double tsPageReqMakingAvg;

    @Column(name = "ts_page_tcp_connect_sum", nullable = false)
    private Double tsPageTcpConnectSum;

    @Column(name = "ts_page_tcp_connect_min", nullable = false)
    private Double tsPageTcpConnectMin;

    @Column(name = "ts_page_tcp_connect_max", nullable = false)
    private Double tsPageTcpConnectMax;

    @Column(name = "ts_page_tcp_connect_avg", nullable = false)
    private Double tsPageTcpConnectAvg;

    // Network Speed (Mbps/pps)
    @Column(name = "mbps", nullable = false)
    private Double mbps;

    @Column(name = "mbps_req", nullable = false)
    private Double mbpsReq;

    @Column(name = "mbps_res", nullable = false)
    private Double mbpsRes;

    @Column(name = "pps", nullable = false)
    private Double pps;

    @Column(name = "pps_req", nullable = false)
    private Double ppsReq;

    @Column(name = "pps_res", nullable = false)
    private Double ppsRes;

    @Column(name = "mbps_min", nullable = false)
    private Double mbpsMin;

    @Column(name = "mbps_min_req", nullable = false)
    private Double mbpsMinReq;

    @Column(name = "mbps_min_res", nullable = false)
    private Double mbpsMinRes;

    @Column(name = "pps_min", nullable = false)
    private Double ppsMin;

    @Column(name = "pps_min_req", nullable = false)
    private Double ppsMinReq;

    @Column(name = "pps_min_res", nullable = false)
    private Double ppsMinRes;

    @Column(name = "mbps_max", nullable = false)
    private Double mbpsMax;

    @Column(name = "mbps_max_req", nullable = false)
    private Double mbpsMaxReq;

    @Column(name = "mbps_max_res", nullable = false)
    private Double mbpsMaxRes;

    @Column(name = "pps_max", nullable = false)
    private Double ppsMax;

    @Column(name = "pps_max_req", nullable = false)
    private Double ppsMaxReq;

    @Column(name = "pps_max_res", nullable = false)
    private Double ppsMaxRes;

    // Error Percentage
    @Column(name = "tcp_error_percentage", nullable = false)
    private Double tcpErrorPercentage;

    @Column(name = "tcp_error_percentage_req", nullable = false)
    private Double tcpErrorPercentageReq;

    @Column(name = "tcp_error_percentage_res", nullable = false)
    private Double tcpErrorPercentageRes;

    @Column(name = "page_error_percentage", nullable = false)
    private Double pageErrorPercentage;

    // Location Information
    @Column(name = "country_name_req", length = 64)
    private String countryNameReq;

    @Column(name = "country_name_res", length = 64)
    private String countryNameRes;

    @Column(name = "continent_name_req", length = 32)
    private String continentNameReq;

    @Column(name = "continent_name_res", length = 32)
    private String continentNameRes;

    @Column(name = "domestic_primary_name_req", length = 64)
    private String domesticPrimaryNameReq;

    @Column(name = "domestic_primary_name_res", length = 64)
    private String domesticPrimaryNameRes;

    @Column(name = "domestic_sub1_name_req", length = 64)
    private String domesticSub1NameReq;

    @Column(name = "domestic_sub1_name_res", length = 64)
    private String domesticSub1NameRes;

    @Column(name = "domestic_sub2_name_req", length = 64)
    private String domesticSub2NameReq;

    @Column(name = "domestic_sub2_name_res", length = 64)
    private String domesticSub2NameRes;

    // Protocol Information
    @Column(name = "ndpi_protocol_app", nullable = false, length = 64)
    private String ndpiProtocolApp;

    @Column(name = "ndpi_protocol_master", nullable = false, length = 64)
    private String ndpiProtocolMaster;

    @Column(name = "sensor_device_name", nullable = false, length = 64)
    private String sensorDeviceName;

    // HTTP Information
    @Column(name = "http_method")
    private String httpMethod;

    @Column(name = "http_version", nullable = false, length = 16)
    private String httpVersion;

    @Column(name = "http_version_req", length = 16)
    private String httpVersionReq;

    @Column(name = "http_version_res", length = 16)
    private String httpVersionRes;

    @Column(name = "http_res_phrase", columnDefinition = "TEXT")
    private String httpResPhrase;

    @Column(name = "http_content_type", columnDefinition = "TEXT")
    private String httpContentType;

    @Column(name = "http_user_agent", columnDefinition = "TEXT")
    private String httpUserAgent;

    @Column(name = "http_cookie", columnDefinition = "TEXT")
    private String httpCookie;

    @Column(name = "http_location", columnDefinition = "TEXT")
    private String httpLocation;

    @Column(name = "http_host", columnDefinition = "TEXT")
    private String httpHost;

    @Column(name = "http_uri", columnDefinition = "TEXT")
    private String httpUri;

    @Column(name = "http_uri_split", columnDefinition = "TEXT")
    private String httpUriSplit;

    @Column(name = "http_referer", columnDefinition = "TEXT")
    private String httpReferer;

    // User Agent Information
    @Column(name = "user_agent_software_name", length = 64)
    private String userAgentSoftwareName;

    @Column(name = "user_agent_operating_system_name", length = 64)
    private String userAgentOperatingSystemName;

    @Column(name = "user_agent_operating_platform", length = 64)
    private String userAgentOperatingPlatform;

    @Column(name = "user_agent_software_type", length = 64)
    private String userAgentSoftwareType;

    @Column(name = "user_agent_hardware_type", length = 64)
    private String userAgentHardwareType;

    @Column(name = "user_agent_layout_engine_name", length = 64)
    private String userAgentLayoutEngineName;

    // Metadata
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}