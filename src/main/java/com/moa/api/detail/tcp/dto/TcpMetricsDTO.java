package com.moa.api.detail.tcp.dto;

import java.util.Map;

/*****************************************************************************
 CLASS NAME    : TcpMetricsDTO
 DESCRIPTION   : TCP 세션 상세 메트릭을 표현하는 DTO로,
 원시 통계 컬럼과 파생 지표, 품질 평가, 진단 정보를 포함
 AUTHOR        : 방대혁
 ******************************************************************************/
/**
 * TCP Metrics DTO
 * 실제 DB 컬럼에 완전 매핑
 * ts_first, ts_last는 null 가능
 */
public record TcpMetricsDTO(
        // 식별 / 엔드포인트
        String rowKey,
        String srcMac,
        String dstMac,
        String srcIp,
        Integer srcPort,
        String dstIp,
        Integer dstPort,
        String app,
        String master,
        String sni,

        // 시간 정보
        Double tsSampleBegin,
        Double tsSampleEnd,
        Double tsFirst,
        Double tsLast,
        Double tsExpired,
        double durSec,

        // 세션 상태
        Integer expired,
        Integer expiredByTimeout,
        Integer sessionTimeout,
        Integer stoppedTransaction,
        Integer stoppedTransactionReq,
        Integer stoppedTransactionRes,

        // 속도
        double bps,

        // 트래픽 통계 (전체)
        long len,
        long lenReq,
        long lenRes,
        long pkts,
        long pktsReq,
        long pktsRes,

        // PDU (페이로드만)
        long lenPdu,
        long lenPduReq,
        long lenPduRes,
        long pktsPdu,
        long pktsPduReq,
        long pktsPduRes,
        long overhead,              // 계산: len - lenPdu
        double overheadRate,        // 계산: overhead / len * 100

        // 재전송
        long retransCnt,
        long retransCntReq,
        long retransCntRes,
        long retransLen,
        long retransLenReq,
        long retransLenRes,
        double retransRateBytes,    // 계산: retransLen / len
        double retransRatePkts,     // 계산: retransCnt / pkts

        // 순서 오류
        long oooCnt,
        long oooCntReq,
        long oooCntRes,
        long oooLen,
        long oooLenReq,
        long oooLenRes,
        double oooRatePkts,         // 계산: oooCnt / pkts

        // 패킷 손실
        long lostCnt,
        long lostCntReq,
        long lostCntRes,
        long lostLen,
        long lostLenReq,
        long lostLenRes,
        double lossRatePkts,        // 계산: lostCnt / pkts

        // 중복 ACK
        long dupAckCnt,
        long dupAckCntReq,
        long dupAckCntRes,
        long dupAckLen,
        double dupAckRate,          // 계산: dupAckCnt / pkts

        // ACK 손실
        long ackLostCnt,
        long ackLostCntReq,
        long ackLostCntRes,
        long ackLostLen,

        // 체크섬 에러
        long csumCnt,
        long csumCntReq,
        long csumCntRes,
        long csumLen,
        long csumLenReq,
        long csumLenRes,
        double csumRatePkts,        // 계산: csumCnt / pkts

        // 윈도우 관련
        long zeroWinCnt,
        long zeroWinCntReq,
        long zeroWinCntRes,
        long winFullCnt,
        long winFullCntReq,
        long winFullCntRes,
        long winUpdateCnt,
        long winUpdateCntReq,
        long winUpdateCntRes,

        // RTT/RTO
        long ackRttCntReq,          // RTT 측정 횟수
        long ackRttCntRes,
        long ackRtoCntReq,          // RTO 타임아웃
        long ackRtoCntRes,
        long ackRtoTotal,           // 계산: rtoReq + rtoRes
        double rtoRate,             // 계산: rtoTotal / (rttReq + rttRes) * 100

        // 패턴 분석
        double reqResRatio,         // 계산: lenReq / lenRes
        boolean ackOnly,            // 계산: len == 0 && pkts > 0
        String handshake,           // "성공" / "응답없음" / "불명"
        String termination,         // "정상종료(예상)" / "RST 종료" / "불명"
        double avgPktSize,          // 계산: len / pkts

        // TCP 플래그
        Map<String, Long> flags,

        // 품질 카운트
        Map<String, Long> qualityCounts,

        // 품질 평가
        QualityScore quality,

        // 배지
        Map<String, String> badges,

        // 진단 메시지
        Map<String, String> diagnostics,

        // 환경 정보
        Environment env
) {

    /** 품질 점수 평가 */
    public record QualityScore(
            int score,              // 0~100 점수
            String grade,           // Excellent/Good/Fair/Poor/Critical
            String summary          // 한줄 요약
    ) {}

    /** 환경(지리) 정보 */
    public record Environment(
            String countryReq,
            String countryRes,
            String continentReq,
            String continentRes,
            String domesticPrimaryReq,
            String domesticPrimaryRes,
            String domesticSub1Req,
            String domesticSub1Res,
            String domesticSub2Req,
            String domesticSub2Res
    ) {}
}
