package com.moa.api.detail.ethernet.repository;

/**
 * Repository의 네이티브 쿼리 결과를 매핑하는 Projection 인터페이스
 */
public interface EthernetAgg {

    // === 식별 / 메타 ===
    String getRowKey();
    String getFlowIdentifier();

    // 🆕 샘플링 구간
    Double getTsSampleBegin();
    Double getTsSampleEnd();

    // 🆕 세션 상태
    Integer getExpired();
    Integer getExpiredByTimeout();

    // === 엔드포인트 ===
    String getSrcMac();
    String getDstMac();
    String getSrcIp();
    String getDstIp();
    Integer getSrcPort();
    Integer getDstPort();

    // === 프로토콜 ===
    Long getL2Proto();
    Long getL3Proto();
    Long getL4Proto();
    Long getL7proto();
    Integer getIpVersion();

    // === 바이트 통계 ===
    Long getLenDelta();
    Long getLenReqDelta();
    Long getLenResDelta();

    // === CRC 에러 바이트 ===
    Long getCrcErrorLenDelta();
    Long getCrcErrorLenReqDelta();
    Long getCrcErrorLenResDelta();

    // === 프레임/패킷 통계 ===
    Long getPktsDelta();
    Long getPktsReqDelta();
    Long getPktsResDelta();

    // === CRC 에러 카운트 ===
    Long getCrcErrorCntDelta();
    Long getCrcErrorCntReqDelta();
    Long getCrcErrorCntResDelta();

    // === 패킷 길이 통계 ===
    Integer getPktLenMinReq();
    Integer getPktLenMinRes();
    Integer getPktLenMaxReq();
    Integer getPktLenMaxRes();
    Double getPktLenAvgReq();
    Double getPktLenAvgRes();

    // === 타임스탬프 (전체) ===
    Double getTsFirst();
    Double getTsLast();

    // 🆕 방향별 타임스탬프 (선택사항 - 필요시 쿼리에 추가)
    Double getTsFirstReq();
    Double getTsFirstRes();
    Double getTsLastReq();
    Double getTsLastRes();

    // === nDPI / 애플리케이션 ===
    String getNdpiProtocolApp();
    String getNdpiProtocolMaster();
    String getSniHostname();

    // === 지리 정보 ===
    String getCountryNameReq();
    String getCountryNameRes();
    String getContinentNameReq();
    String getContinentNameRes();

    // === 국내 지역 ===
    String getDomesticPrimaryNameReq();
    String getDomesticPrimaryNameRes();
    String getDomesticSub1NameReq();
    String getDomesticSub1NameRes();
    String getDomesticSub2NameReq();
    String getDomesticSub2NameRes();

    // === 센서 ===
    String getSensorDeviceName();
}