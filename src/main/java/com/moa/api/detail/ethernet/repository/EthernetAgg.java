package com.moa.api.detail.ethernet.repository;

public interface EthernetAgg {
    // 식별 / 샘플 메타
    String  getRowKey();
    String  getFlowIdentifier();
    Double  getTsSampleBegin();
    Double  getTsSampleEnd();
    Integer getExpired();
    String  getTsServer();
    Long    getTsServerNsec();
    Integer getExpiredByTimeout();

    // 엔드포인트
    String  getSrcMac();
    String  getDstMac();
    String  getSrcIp();
    String  getDstIp();
    Integer getSrcPort();
    Integer getDstPort();

    // 프로토콜
    Long getL2Proto();
    Long getL3Proto();
    Long getL4Proto();
    Long getL7proto();
    Integer getIpVersion();
    String  getNdpiProtocolApp();
    String  getNdpiProtocolMaster();
    String  getSniHostname();

    // 규모 (바이트/패킷) - delta 합계
    Long getLenDelta();
    Long getLenReqDelta();
    Long getLenResDelta();

    Long getCrcErrorLenDelta();
    Long getCrcErrorLenReqDelta();
    Long getCrcErrorLenResDelta();

    Long getPktsDelta();
    Long getPktsReqDelta();
    Long getPktsResDelta();

    Long getCrcErrorCntDelta();
    Long getCrcErrorCntReqDelta();
    Long getCrcErrorCntResDelta();

    // 패킷 크기 통계
    Integer getPktLenMinReq();
    Integer getPktLenMinRes();
    Integer getPktLenMaxReq();
    Integer getPktLenMaxRes();
    Double  getPktLenAvgReq();
    Double  getPktLenAvgRes();

    // 타임스탬프 (duration 계산용)
    Double getTsFirst();
    Double getTsFirstReq();
    Double getTsFirstRes();
    Double getTsLast();
    Double getTsLastReq();
    Double getTsLastRes();

    // Geo / 국내 위치
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
}
