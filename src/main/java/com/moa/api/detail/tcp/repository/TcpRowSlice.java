package com.moa.api.detail.tcp.repository;

/*****************************************************************************
 CLASS NAME    : TcpRowSlice
 DESCRIPTION   : tcp_sample 테이블의 TCP 세션/품질 지표를 조회하기 위한 Projection 인터페이스
 AUTHOR        : 방대혁
 ******************************************************************************/
/**
 * TCP 샘플 데이터 조회용 Projection 인터페이스
 */
public interface TcpRowSlice {

    // 기본 식별
    String getRowKey();
    String getFlowIdentifier();

    // 타임스탬프
    Double getTsSampleBegin();
    Double getTsSampleEnd();
    Double getTsExpired();
    Double getTsServer();
    Long getTsServerNsec();

    /**
     * ts_first/ts_last는 샘플 집계 구간 외의
     * 전체 세션 구간을 표현할 때 사용되며
     * 기본 쿼리에서는 사용하지 않으므로 기본 구현은 null 처리
     */
    default Double getTsFirst() { return null; }
    default Double getTsLast() { return null; }

    // 세션 상태
    Integer getExpired();
    Integer getExpiredByTimeout();
    Integer getSessionTimeout();
    Integer getStoppedTransaction();
    Integer getStoppedTransactionReq();
    Integer getStoppedTransactionRes();

    // 엔드포인트
    String getSrcMac();
    String getDstMac();
    String getSrcIp();
    String getDstIp();
    Integer getSrcPort();
    Integer getDstPort();

    // 트래픽 통계
    Long getLenDelta();
    Long getLenReqDelta();
    Long getLenResDelta();
    Long getPktsDelta();
    Long getPktsReqDelta();
    Long getPktsResDelta();

    // PDU (페이로드만)
    Long getLenPduDelta();
    Long getLenPduReqDelta();
    Long getLenPduResDelta();
    Long getPktsPduDelta();
    Long getPktsPduReqDelta();
    Long getPktsPduResDelta();

    // 재전송 (바이트)
    Long getRetransmissionLenDelta();
    Long getRetransmissionLenReqDelta();
    Long getRetransmissionLenResDelta();

    // 재전송 (카운트)
    Long getRetransmissionCntDelta();
    Long getRetransmissionCntReqDelta();
    Long getRetransmissionCntResDelta();

    // 순서 오류 (바이트)
    Long getOutOfOrderLenDelta();
    Long getOutOfOrderLenReqDelta();
    Long getOutOfOrderLenResDelta();

    // 순서 오류 (카운트)
    Long getOutOfOrderCntDelta();
    Long getOutOfOrderCntReqDelta();
    Long getOutOfOrderCntResDelta();

    // 패킷 손실 (바이트)
    Long getLostSegLenDelta();
    Long getLostSegLenReqDelta();
    Long getLostSegLenResDelta();

    // 패킷 손실 (카운트)
    Long getLostSegCntDelta();
    Long getLostSegCntReqDelta();
    Long getLostSegCntResDelta();

    // ACK 손실 (바이트)
    Long getAckLostLenDelta();
    Long getAckLostLenReqDelta();
    Long getAckLostLenResDelta();

    // ACK 손실 (카운트)
    Long getAckLostCntDelta();
    Long getAckLostCntReqDelta();
    Long getAckLostCntResDelta();

    // Window Update (바이트)
    Long getWinUpdateLenDelta();
    Long getWinUpdateLenReqDelta();
    Long getWinUpdateLenResDelta();

    // Window Update (카운트)
    Long getWinUpdateCntDelta();
    Long getWinUpdateCntReqDelta();
    Long getWinUpdateCntResDelta();

    // 중복 ACK (바이트)
    Long getDupAckLenDelta();
    Long getDupAckLenReqDelta();
    Long getDupAckLenResDelta();

    // 중복 ACK (카운트)
    Long getDupAckCntDelta();
    Long getDupAckCntReqDelta();
    Long getDupAckCntResDelta();

    // Zero Window (바이트)
    Long getZeroWinLenDelta();
    Long getZeroWinLenReqDelta();
    Long getZeroWinLenResDelta();

    // Zero Window (카운트)
    Long getZeroWinCntDelta();
    Long getZeroWinCntReqDelta();
    Long getZeroWinCntResDelta();

    // 체크섬 에러 (바이트)
    Long getChecksumErrorLenDelta();
    Long getChecksumErrorLenReqDelta();
    Long getChecksumErrorLenResDelta();

    // 체크섬 에러 (카운트)
    Long getChecksumErrorCntDelta();
    Long getChecksumErrorCntReqDelta();
    Long getChecksumErrorCntResDelta();

    // Window Full (카운트)
    Long getWindowFullCntDelta();
    Long getWindowFullCntReqDelta();
    Long getWindowFullCntResDelta();

    // RTT/RTO
    Long getAckRttCntReq();
    Long getAckRttCntRes();
    Long getAckRtoCntReq();
    Long getAckRtoCntRes();

    // TCP 플래그 (모든 플래그)
    Long getTcpFlagStatFinReqDelta();
    Long getTcpFlagStatFinResDelta();
    Long getTcpFlagStatFinackReqDelta();
    Long getTcpFlagStatFinackResDelta();
    Long getTcpFlagStatSynReqDelta();
    Long getTcpFlagStatSynResDelta();
    Long getTcpFlagStatRstReqDelta();
    Long getTcpFlagStatRstResDelta();
    Long getTcpFlagStatPshReqDelta();
    Long getTcpFlagStatPshResDelta();
    Long getTcpFlagStatAckReqDelta();
    Long getTcpFlagStatAckResDelta();
    Long getTcpFlagStatUrgReqDelta();
    Long getTcpFlagStatUrgResDelta();
    Long getTcpFlagStatCwrReqDelta();
    Long getTcpFlagStatCwrResDelta();
    Long getTcpFlagStatEceReqDelta();
    Long getTcpFlagStatEceResDelta();
    Long getTcpFlagStatNsReqDelta();
    Long getTcpFlagStatNsResDelta();
    Long getTcpFlagStatSynDelta();
    Long getTcpFlagStatSynackDelta();

    // 애플리케이션
    String getNdpiProtocolApp();
    String getNdpiProtocolMaster();
    String getSniHostname();

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
}
