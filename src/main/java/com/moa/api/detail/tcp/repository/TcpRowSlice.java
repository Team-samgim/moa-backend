package com.moa.api.detail.tcp.repository;

public interface TcpRowSlice {
    String getRowKey();
    Double getTsSampleBegin();
    Double getTsSampleEnd();

    Long getLenDelta();
    Long getLenReqDelta();
    Long getLenResDelta();

    Long getPktsDelta();
    Long getPktsReqDelta();
    Long getPktsResDelta();

    Long getRetransmissionLenDelta();
    Long getRetransmissionCntDelta();
    Long getOutOfOrderCntDelta();
    Long getLostSegCntDelta();
    Long getChecksumErrorCntDelta();
    Long getZeroWinCntDelta();
    Long getWindowFullCntDelta();
    Long getWinUpdateCntDelta();

    // int8 → Long 로 통일
    Long getTcpFlagStatSynDelta();
    Long getTcpFlagStatSynackDelta();
    Long getTcpFlagStatFinReqDelta();
    Long getTcpFlagStatFinResDelta();
    Long getTcpFlagStatFinackReqDelta();
    Long getTcpFlagStatFinackResDelta();
    Long getTcpFlagStatRstReqDelta();
    Long getTcpFlagStatRstResDelta();
    Long getTcpFlagStatPshReqDelta();
    Long getTcpFlagStatPshResDelta();

    // inet → 쿼리에서 ::text 캐스팅, 인터페이스는 String
    String getSrcIp();
    String getDstIp();
    Integer getSrcPort(); // int4 → Integer
    Integer getDstPort();

    String getNdpiProtocolApp();
    String getNdpiProtocolMaster();
    String getSniHostname();
}
