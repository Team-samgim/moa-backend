package com.moa.api.detail.page.repository;

/*****************************************************************************
 INTERFACE NAME : HttpPageRowSlice
 DESCRIPTION    : HTTP Page Row Projection 인터페이스
 - 네이티브 쿼리 결과를 235개 컬럼으로 매핑
 - 상세 메트릭 계산용 원시 데이터를 제공
 AUTHOR         : 방대혁
 ******************************************************************************/
/**
 * HTTP Page Row Slice Interface
 * 235개 컬럼 Projection
 */
public interface HttpPageRowSlice {

    // 기본 정보
    String getRowKey();
    String getSrcIp();
    String getDstIp();
    Integer getSrcPort();
    Integer getDstPort();
    Double getTsFrameArrival();
    Double getTsFrameLandoff();
    Long getPageIdx();
    String getTsServer();
    Double getTsServerNsec();
    String getSrcMac();
    String getDstMac();

    // HTTP 길이
    Long getPageHttpLen();
    Long getPageHttpLenReq();
    Long getPageHttpLenRes();
    Long getPageHttpHeaderLenReq();
    Long getPageHttpHeaderLenRes();
    Long getPageHttpContentLenReq();
    Long getPageHttpContentLenRes();
    Long getHttpContentLength();
    Long getHttpContentLengthReq();

    // 패킷/TCP 길이
    Long getPagePktLen();
    Long getPagePktLenReq();
    Long getPagePktLenRes();
    Long getPageTcpLen();
    Long getPageTcpLenReq();
    Long getPageTcpLenRes();
    Long getConnErrSessionLen();
    Long getReqConnErrSessionLen();
    Long getResConnErrSessionLen();

    // TCP 에러 바이트
    Long getRetransmissionLen();
    Long getRetransmissionLenReq();
    Long getRetransmissionLenRes();
    Long getOutOfOrderLen();
    Long getOutOfOrderLenReq();
    Long getOutOfOrderLenRes();
    Long getLostSegLen();
    Long getLostSegLenReq();
    Long getLostSegLenRes();
    Long getAckLostLen();
    Long getAckLostLenReq();
    Long getAckLostLenRes();
    Long getWinUpdateLen();
    Long getWinUpdateLenReq();
    Long getWinUpdateLenRes();
    Long getDupAckLen();
    Long getDupAckLenReq();
    Long getDupAckLenRes();
    Long getZeroWinLen();
    Long getZeroWinLenReq();
    Long getZeroWinLenRes();
    Long getChecksumErrorLen();
    Long getChecksumErrorLenReq();
    Long getChecksumErrorLenRes();

    // RTT/Ack 카운트
    Long getPageRttConnCntReq();
    Long getPageRttConnCntRes();
    Long getPageRttAckCntReq();
    Long getPageRttAckCntRes();

    // 페이지 카운트
    Long getPageReqMakingCnt();
    Long getPageHttpCnt();
    Long getPageHttpCntReq();
    Long getPageHttpCntRes();
    Long getPagePktCnt();
    Long getPagePktCntReq();
    Long getPagePktCntRes();
    Long getPageSessionCnt();
    Long getPageTcpConnectCnt();
    Long getConnErrPktCnt();
    Long getConnErrSessionCnt();
    Long getPageTcpCnt();
    Long getPageTcpCntReq();
    Long getPageTcpCntRes();
    Long getPageErrorCnt();

    // TCP 에러 카운트
    Long getRetransmissionCnt();
    Long getRetransmissionCntReq();
    Long getRetransmissionCntRes();
    Long getOutOfOrderCnt();
    Long getOutOfOrderCntReq();
    Long getOutOfOrderCntRes();
    Long getLostSegCnt();
    Long getLostSegCntReq();
    Long getLostSegCntRes();
    Long getAckLostCnt();
    Long getAckLostCntReq();
    Long getAckLostCntRes();
    Long getWinUpdateCnt();
    Long getWinUpdateCntReq();
    Long getWinUpdateCntRes();
    Long getDupAckCnt();
    Long getDupAckCntReq();
    Long getDupAckCntRes();
    Long getZeroWinCnt();
    Long getZeroWinCntReq();
    Long getZeroWinCntRes();
    Long getWindowFullCnt();
    Long getWindowFullCntReq();
    Long getWindowFullCntRes();
    Long getTcpErrorCnt();
    Long getTcpErrorCntReq();
    Long getTcpErrorCntRes();
    Long getTcpErrorLen();
    Long getTcpErrorLenReq();
    Long getTcpErrorLenRes();

    // HTTP 메소드
    Long getReqMethodGetCnt();
    Long getReqMethodPutCnt();
    Long getReqMethodHeadCnt();
    Long getReqMethodPostCnt();
    Long getReqMethodTraceCnt();
    Long getReqMethodDeleteCnt();
    Long getReqMethodOptionsCnt();
    Long getReqMethodPatchCnt();
    Long getReqMethodConnectCnt();
    Long getReqMethodOthCnt();
    Long getReqMethodGetCntError();
    Long getReqMethodPutCntError();
    Long getReqMethodHeadCntError();
    Long getReqMethodPostCntError();
    Long getReqMethodTraceCntError();
    Long getReqMethodDeleteCntError();
    Long getReqMethodOptionsCntError();
    Long getReqMethodPatchCntError();
    Long getReqMethodConnectCntError();
    Long getReqMethodOthCntError();

    // 응답 코드
    Long getResCode1xxCnt();
    Long getResCode2xxCnt();
    Long getResCode304Cnt();
    Long getResCode3xxCnt();
    Long getResCode401Cnt();
    Long getResCode403Cnt();
    Long getResCode404Cnt();
    Long getResCode4xxCnt();
    Long getResCode5xxCnt();
    Long getResCodeOthCnt();

    // 트랜잭션 상태
    Long getStoppedTransactionCnt();
    Long getStoppedTransactionCntReq();
    Long getStoppedTransactionCntRes();
    Long getIncompleteCnt();
    Long getIncompleteCntReq();
    Long getIncompleteCntRes();
    Long getTimeoutCnt();
    Long getTimeoutCntReq();
    Long getTimeoutCntRes();
    Long getTsPageRtoCntReq();
    Long getTsPageRtoCntRes();

    // Content Type
    Long getContentTypeHtmlCntReq();
    Long getContentTypeHtmlCntRes();
    Long getContentTypeCssCntReq();
    Long getContentTypeCssCntRes();
    Long getContentTypeJsCntReq();
    Long getContentTypeJsCntRes();
    Long getContentTypeImgCntReq();
    Long getContentTypeImgCntRes();
    Long getContentTypeOthCntReq();
    Long getContentTypeOthCntRes();

    // URI 카운트
    Long getUriCnt();
    Long getHttpUriCnt();
    Long getHttpsUriCnt();

    // 시간 메트릭
    Double getTsFirst();
    Double getTsPageBegin();
    Double getTsPageEnd();
    Double getTsPageReqSyn();
    Double getTsPage();
    Double getTsPageGap();
    Double getTsPageResInit();
    Double getTsPageResInitGap();
    Double getTsPageResApp();
    Double getTsPageResAppGap();
    Double getTsPageRes();
    Double getTsPageResGap();
    Double getTsPageTransferReq();
    Double getTsPageTransferReqGap();
    Double getTsPageTransferRes();
    Double getTsPageTransferResGap();
    Double getTsPageReqMakingSum();
    Double getTsPageReqMakingAvg();
    Double getTsPageTcpConnectSum();
    Double getTsPageTcpConnectMin();
    Double getTsPageTcpConnectMax();
    Double getTsPageTcpConnectAvg();

    // 성능 메트릭
    Double getMbps();
    Double getMbpsReq();
    Double getMbpsRes();
    Double getPps();
    Double getPpsReq();
    Double getPpsRes();
    Double getMbpsMin();
    Double getMbpsMinReq();
    Double getMbpsMinRes();
    Double getPpsMin();
    Double getPpsMinReq();
    Double getPpsMinRes();
    Double getMbpsMax();
    Double getMbpsMaxReq();
    Double getMbpsMaxRes();
    Double getPpsMax();
    Double getPpsMaxReq();
    Double getPpsMaxRes();

    // 에러 비율
    Double getTcpErrorPercentage();
    Double getTcpErrorPercentageReq();
    Double getTcpErrorPercentageRes();
    Double getPageErrorPercentage();

    // 지리 정보
    String getCountryNameReq();
    String getCountryNameRes();
    String getContinentNameReq();
    String getContinentNameRes();
    String getDomesticPrimaryNameReq();
    String getDomesticPrimaryNameRes();
    String getDomesticSub1NameReq();
    String getDomesticSub1NameRes();
    String getDomesticSub2NameReq();
    String getDomesticSub2NameRes();

    // HTTP 정보
    String getNdpiProtocolApp();
    String getNdpiProtocolMaster();
    String getSensorDeviceName();
    String getHttpMethod();
    String getHttpVersion();
    String getHttpVersionReq();
    String getHttpVersionRes();
    String getHttpResCode();
    String getHttpResPhrase();
    String getHttpContentType();
    String getHttpUserAgent();
    String getHttpCookie();
    String getHttpLocation();
    String getHttpHost();
    String getHttpUri();
    String getHttpUriSplit();
    String getHttpReferer();
    Integer getIsHttps();

    // User Agent
    String getUserAgentSoftwareName();
    String getUserAgentOperatingSystemName();
    String getUserAgentOperatingPlatform();
    String getUserAgentSoftwareType();
    String getUserAgentHardwareType();
    String getUserAgentLayoutEngineName();
}
