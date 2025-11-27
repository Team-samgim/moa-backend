package com.moa.api.detail.uri.repository;

/*****************************************************************************
 INTERFACE NAME : HttpUriRowSlice
 DESCRIPTION    : HTTP URI/페이지 단위 상세 지표 조회용 Projection 인터페이스
 PARAMETERS     : 없음 (Spring Data JPA Projection 인터페이스)
 OUTPUT PARAMETER  : HTTP 세션/URI별 지연 시간, 패킷/바이트, 에러, 지리 정보,
 HTTP/UA 메타데이터 등을 컬럼 단위로 조회
 특이사항       :
 - http_page_sample 등 상세 성능 테이블을 대상으로 한 Row 단위 조회에 사용
 - 동적 쿼리/페이징 시 필요한 최소 컬럼만 선별하여 조회하는 용도
 AUTHOR        : 방대혁
 ******************************************************************************/
/**
 * HTTP URI 데이터 조회용 Projection 인터페이스
 */
public interface HttpUriRowSlice {

    // 기본 식별
    String getRowKey();

    // 엔드포인트
    String getSrcIp();
    String getDstIp();
    Integer getSrcPort();
    Integer getDstPort();
    String getSrcMac();
    String getDstMac();

    // 타임스탬프
    Double getTsFrameArrival();
    Double getTsFrameLandoff();
    Double getTsServer();
    Long getTsServerNsec();
    Double getTsFirst();

    // 요청/응답 시간 분석
    Double getTsReqPktFirst();
    Double getTsReqPktPush();
    Double getTsReqPktLast();
    Double getTsResPktFirst();
    Double getTsResPktPush();
    Double getTsResPktLast();

    // 지연 시간
    Double getTsResDelayTransfer();
    Double getTsRsqDelayResponse();
    Double getTsResProcessFirst();
    Double getTsResProcessPush();
    Double getTsResProcessLast();
    Double getTsReqDelayTransfer();

    // 페이지/URI 인덱스
    Long getPageIdx();
    Long getUriIdx();

    // 패킷 길이 (전체)
    Long getPktLen();
    Long getPktLenReq();
    Long getPktLenRes();

    // HTTP 길이
    Long getHttpLen();
    Long getHttpLenReq();
    Long getHttpLenRes();
    Long getHttpHeaderLenReq();
    Long getHttpHeaderLenRes();
    Long getHttpContentLenReq();
    Long getHttpContentLenRes();

    // TCP 길이
    Long getTcpLen();
    Long getTcpLenReq();
    Long getTcpLenRes();

    // 재전송 (바이트)
    Long getRetransmissionLen();
    Long getRetransmissionLenReq();
    Long getRetransmissionLenRes();

    // 빠른 재전송 (바이트)
    Long getFastRetransmissionLen();
    Long getFastRetransmissionLenReq();
    Long getFastRetransmissionLenRes();

    // 순서 오류 (바이트)
    Long getOutOfOrderLen();
    Long getOutOfOrderLenReq();
    Long getOutOfOrderLenRes();

    // 패킷 손실 (바이트)
    Long getLostSegLen();
    Long getLostSegLenReq();
    Long getLostSegLenRes();

    // ACK 손실 (바이트)
    Long getAckLostLen();
    Long getAckLostLenReq();
    Long getAckLostLenRes();

    // Window Update (바이트)
    Long getWinUpdateLen();
    Long getWinUpdateLenReq();
    Long getWinUpdateLenRes();

    // 중복 ACK (바이트)
    Long getDupAckLen();
    Long getDupAckLenReq();
    Long getDupAckLenRes();

    // Zero Window (바이트)
    Long getZeroWinLen();
    Long getZeroWinLenReq();
    Long getZeroWinLenRes();

    // 체크섬 에러 (바이트)
    Long getChecksumErrorLen();
    Long getChecksumErrorLenReq();
    Long getChecksumErrorLenRes();

    // 패킷 카운트
    Long getPktCnt();
    Long getPktCntReq();
    Long getPktCntRes();

    // HTTP 카운트
    Long getHttpCnt();
    Long getHttpCntReq();
    Long getHttpCntRes();

    // RTT/RTO
    Long getAckRttCntReq();
    Long getAckRttCntRes();
    Long getAckRtoCntReq();
    Long getAckRtoCntRes();

    // 재전송 카운트
    Long getRetransmissionCnt();
    Long getRetransmissionCntReq();
    Long getRetransmissionCntRes();

    // 순서 오류 카운트
    Long getOutOfOrderCnt();
    Long getOutOfOrderCntReq();
    Long getOutOfOrderCntRes();

    // 패킷 손실 카운트
    Long getLostSegCnt();
    Long getLostSegCntReq();
    Long getLostSegCntRes();

    // ACK 손실 카운트
    Long getAckLostCnt();
    Long getAckLostCntReq();
    Long getAckLostCntRes();

    // Window Update 카운트
    Long getWinUpdateCnt();
    Long getWinUpdateCntReq();
    Long getWinUpdateCntRes();

    // 중복 ACK 카운트
    Long getDupAckCnt();
    Long getDupAckCntReq();
    Long getDupAckCntRes();

    // Zero Window 카운트
    Long getZeroWinCnt();
    Long getZeroWinCntReq();
    Long getZeroWinCntRes();

    // Window Full 카운트
    Long getWindowFullCnt();
    Long getWindowFullCntReq();
    Long getWindowFullCntRes();

    // 체크섬 에러 카운트
    Long getChecksumErrorCnt();
    Long getChecksumErrorCntReq();
    Long getChecksumErrorCntRes();

    // TCP 카운트
    Long getTcpCnt();
    Long getTcpCntReq();
    Long getTcpCntRes();

    // TCP 에러
    Long getTcpErrorCnt();
    Long getTcpErrorCntReq();
    Long getTcpErrorCntRes();
    Long getTcpErrorLen();
    Long getTcpErrorLenReq();
    Long getTcpErrorLenRes();

    // 요청/응답 카운트
    Long getReqCnt();
    Long getResCnt();

    // Content Type 카운트
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

    // Referer 카운트
    Long getRefererCnt();

    // 상태 플래그
    Integer getIsStoppedTransaction();
    Integer getIsStoppedTransactionReq();
    Integer getIsStoppedTransactionRes();
    Integer getIsIncomplete();
    Integer getIsIncompleteReq();
    Integer getIsIncompleteRes();
    Integer getIsTimeout();
    Integer getIsTimeoutReq();
    Integer getIsTimeoutRes();
    Integer getIsHttps();

    // 성능 메트릭 (Mbps)
    Double getMbps();
    Double getMbpsReq();
    Double getMbpsRes();
    Double getMbpsMin();
    Double getMbpsMinReq();
    Double getMbpsMinRes();
    Double getMbpsMax();
    Double getMbpsMaxReq();
    Double getMbpsMaxRes();

    // 성능 메트릭 (PPS)
    Double getPps();
    Double getPpsReq();
    Double getPpsRes();
    Double getPpsMin();
    Double getPpsMinReq();
    Double getPpsMinRes();
    Double getPpsMax();
    Double getPpsMaxReq();
    Double getPpsMaxRes();

    // TCP 에러 비율
    Double getTcpErrorPercentage();
    Double getTcpErrorPercentageReq();
    Double getTcpErrorPercentageRes();
    Double getTcpErrorLenPercentage();
    Double getTcpErrorLenPercentageReq();
    Double getTcpErrorLenPercentageRes();

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

    // 센서 정보
    String getSensorDeviceName();

    // 프로토콜
    String getNdpiProtocolApp();
    String getNdpiProtocolMaster();

    // HTTP 정보
    String getHttpMethod();
    String getHttpVersion();
    String getHttpVersionReq();
    String getHttpVersionRes();
    String getHttpResPhrase();
    String getHttpContentType();
    String getHttpUserAgent();
    String getHttpCookie();
    String getHttpLocation();
    String getHttpHost();
    String getHttpUri();
    String getHttpUriSplit();
    String getHttpReferer();

    // User Agent 파싱
    String getUserAgentSoftwareName();
    String getUserAgentOperatingSystemName();
    String getUserAgentOperatingPlatform();
    String getUserAgentSoftwareType();
    String getUserAgentHardwareType();
    String getUserAgentLayoutEngineName();
}
