package com.moa.api.detail.page.repository;

public interface HttpPageAgg {
    // 식별
    String  getRowKey();
    String  getSrcIp();
    String  getDstIp();
    Integer getSrcPort();
    Integer getDstPort();
    String  getNdpiProtocolApp();
    String  getNdpiProtocolMaster();
    Integer getIsHttps();
    String  getHttpHost();
    String  getHttpUri();
    String  getHttpMethod();
    Integer getHttpResCode();
    String  getHttpResPhrase();

    // 규모 요약
    Long getBytes();
    Long getBytesReq();
    Long getBytesRes();
    Long getPkts();
    Long getPktsReq();
    Long getPktsRes();

    // 타이밍(집계)
    Double getDurSec();
    Double getConnectAvg();
    Double getConnectMin();
    Double getConnectMax();
    Double getReqMakingAvg();
    Double getReqMakingSum();
    Double getResInit();
    Double getResInitGap();
    Double getResApp();
    Double getResAppGap();
    Double getTransferReq();
    Double getTransferReqGap();
    Double getTransferRes();
    Double getTransferResGap();

    // 품질 카운트(집계)
    Long getRetransmissionCnt();
    Long getOutOfOrderCnt();
    Long getLostSegCnt();
    Long getChecksumErrorCnt();
    Long getDupAckCnt();
    Long getWinUpdateCnt();
    Long getZeroWinCnt();
    Long getWindowFullCnt();
    Long getAckLostCnt();

    // HTTP 분포
    Integer getResCode1xxCnt();
    Integer getResCode2xxCnt();
    Integer getResCode3xxCnt();
    Integer getResCode304Cnt();
    Integer getResCode4xxCnt();
    Integer getResCode5xxCnt();

    Integer getReqMethodGetCnt();
    Integer getReqMethodPostCnt();
    Integer getReqMethodPutCnt();
    Integer getReqMethodDeleteCnt();
    Integer getReqMethodHeadCnt();
    Integer getReqMethodOptionsCnt();
    Integer getReqMethodPatchCnt();
    Integer getReqMethodTraceCnt();
    Integer getReqMethodConnectCnt();
    Integer getReqMethodOthCnt();

    String  getHttpVersion();
    String  getHttpVersionReq();
    String  getHttpVersionRes();

    Integer getContentTypeHtmlCntReq();
    Integer getContentTypeHtmlCntRes();
    Integer getContentTypeCssCntReq();
    Integer getContentTypeCssCntRes();
    Integer getContentTypeJsCntReq();
    Integer getContentTypeJsCntRes();
    Integer getContentTypeImgCntReq();
    Integer getContentTypeImgCntRes();
    Integer getContentTypeOthCntReq();
    Integer getContentTypeOthCntRes();

    // 상태성 배지
    Integer getTimeoutCnt();
    Integer getIncompleteCnt();
    Integer getStoppedTransactionCnt();

    // 크기/헤더/메타/Geo/UA
    Long   getPageHttpHeaderLenReq();
    Long   getPageHttpHeaderLenRes();
    Long   getPageHttpContentLenReq();
    Long   getPageHttpContentLenRes();
    Long   getPagePktLenReq();
    Long   getPagePktLenRes();
    Long   getHttpContentLengthReq();
    Long   getHttpContentLengthRes();

    String getHttpReferer();
    String getHttpCookie();
    String getHttpUserAgent();
    String getHttpContentType();

    String getCountryNameReq();
    String getCountryNameRes();
    String getContinentNameReq();
    String getContinentNameRes();

    String getUserAgentOperatingSystemName();
    String getUserAgentSoftwareName();
    String getUserAgentHardwareType();
    String getUserAgentLayoutEngineName();
}
