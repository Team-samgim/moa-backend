package com.moa.api.detail.tcp.service;

/*****************************************************************************
 CLASS NAME    : TcpMetricsService
 DESCRIPTION   : tcp_sample 테이블에서 상세 메트릭을 조회하고
 품질/배지/진단 메시지를 계산하여 TcpMetricsDTO로 변환하는 서비스
 AUTHOR        : 방대혁
 ******************************************************************************/
/**
 * TCP Metrics Service
 */

import com.moa.api.detail.tcp.dto.TcpMetricsDTO;
import com.moa.api.detail.tcp.repository.TcpRowSlice;
import com.moa.api.detail.tcp.repository.TcpSampleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class TcpMetricsService {

    private final TcpSampleRepository repo;

    @Value("${metrics.thresholds.tcp.retrans-warn:0.03}") private double retransWarn;
    @Value("${metrics.thresholds.tcp.retrans-crit:0.07}") private double retransCrit;
    @Value("${metrics.thresholds.tcp.ooo-warn:0.01}")     private double oooWarn;
    @Value("${metrics.thresholds.tcp.ooo-crit:0.03}")     private double oooCrit;
    @Value("${metrics.thresholds.tcp.loss-warn:0.005}")   private double lossWarn;
    @Value("${metrics.thresholds.tcp.loss-crit:0.02}")    private double lossCrit;
    @Value("${metrics.thresholds.tcp.rto-warn:5}")        private int rtoWarn;
    @Value("${metrics.thresholds.tcp.rto-crit:10}")       private int rtoCrit;

    public TcpMetricsService(TcpSampleRepository repo) {
        this.repo = repo;
    }

    public TcpMetricsDTO getByRowKey(String rowKey) {
        TcpRowSlice r = repo.findSlice(rowKey)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "row not found"));

        // === 기본 시간 계산 ===
        double beg = nz(r.getTsSampleBegin());
        double end = nz(r.getTsSampleEnd());
        double durSec = Math.max(0.001, end - beg);

        // 플로우 실제 시작/종료 시간
        Double tsFirst = r.getTsFirst();
        Double tsLast = r.getTsLast();
        Double tsExpired = r.getTsExpired();

        // 트래픽 통계 (전체)
        long len     = nzl(r.getLenDelta());
        long lenReq  = nzl(r.getLenReqDelta());
        long lenRes  = nzl(r.getLenResDelta());
        long pkts    = nzl(r.getPktsDelta());
        long pktsReq = nzl(r.getPktsReqDelta());
        long pktsRes = nzl(r.getPktsResDelta());

        // PDU (페이로드만)
        long lenPdu     = nzl(r.getLenPduDelta());
        long lenPduReq  = nzl(r.getLenPduReqDelta());
        long lenPduRes  = nzl(r.getLenPduResDelta());
        long pktsPdu    = nzl(r.getPktsPduDelta());
        long pktsPduReq = nzl(r.getPktsPduReqDelta());
        long pktsPduRes = nzl(r.getPktsPduResDelta());

        // 오버헤드 계산
        long overhead = len - lenPdu;
        double overheadRate = len > 0 ? (overhead * 100.0 / len) : 0.0;

        // 재전송
        long rtLen       = nzl(r.getRetransmissionLenDelta());
        long rtLenReq    = nzl(r.getRetransmissionLenReqDelta());
        long rtLenRes    = nzl(r.getRetransmissionLenResDelta());
        long rtCnt       = nzl(r.getRetransmissionCntDelta());
        long rtCntReq    = nzl(r.getRetransmissionCntReqDelta());
        long rtCntRes    = nzl(r.getRetransmissionCntResDelta());

        // 순서 오류
        long oooCnt      = nzl(r.getOutOfOrderCntDelta());
        long oooCntReq   = nzl(r.getOutOfOrderCntReqDelta());
        long oooCntRes   = nzl(r.getOutOfOrderCntResDelta());
        long oooLen      = nzl(r.getOutOfOrderLenDelta());

        // 패킷 손실
        long lostCnt     = nzl(r.getLostSegCntDelta());
        long lostCntReq  = nzl(r.getLostSegCntReqDelta());
        long lostCntRes  = nzl(r.getLostSegCntResDelta());
        long lostLen     = nzl(r.getLostSegLenDelta());

        // 중복 ACK
        long dupAckCnt    = nzl(r.getDupAckCntDelta());
        long dupAckCntReq = nzl(r.getDupAckCntReqDelta());
        long dupAckCntRes = nzl(r.getDupAckCntResDelta());

        // ACK 손실
        long ackLostCnt    = nzl(r.getAckLostCntDelta());
        long ackLostCntReq = nzl(r.getAckLostCntReqDelta());
        long ackLostCntRes = nzl(r.getAckLostCntResDelta());

        // 체크섬 에러
        long csumCnt     = nzl(r.getChecksumErrorCntDelta());
        long csumCntReq  = nzl(r.getChecksumErrorCntReqDelta());
        long csumCntRes  = nzl(r.getChecksumErrorCntResDelta());
        long csumLen     = nzl(r.getChecksumErrorLenDelta());

        // 윈도우
        long zeroWinCnt    = nzl(r.getZeroWinCntDelta());
        long zeroWinCntReq = nzl(r.getZeroWinCntReqDelta());
        long zeroWinCntRes = nzl(r.getZeroWinCntResDelta());
        long winFullCnt    = nzl(r.getWindowFullCntDelta());
        long winFullCntReq = nzl(r.getWindowFullCntReqDelta());
        long winFullCntRes = nzl(r.getWindowFullCntResDelta());
        long winUpdateCnt  = nzl(r.getWinUpdateCntDelta());

        // RTT/RTO
        long ackRttCntReq = nzl(r.getAckRttCntReq());
        long ackRttCntRes = nzl(r.getAckRttCntRes());
        long ackRtoCntReq = nzl(r.getAckRtoCntReq());
        long ackRtoCntRes = nzl(r.getAckRtoCntRes());
        long ackRtoTotal  = ackRtoCntReq + ackRtoCntRes;

        // 세션 상태
        Integer expired          = r.getExpired();
        Integer expiredByTimeout = r.getExpiredByTimeout();
        Integer sessionTimeout   = r.getSessionTimeout();

        // 계산 메트릭
        double bps             = (len * 8.0) / durSec;
        double retransRateBytes = sdiv(rtLen, Math.max(1.0, (double) len));
        double retransRatePkts  = sdiv(rtCnt, Math.max(1.0, (double) pkts));
        double oooRatePkts      = sdiv(oooCnt, Math.max(1.0, (double) pkts));
        double lossRatePkts     = sdiv(lostCnt, Math.max(1.0, (double) pkts));
        double csumRatePkts     = sdiv(csumCnt, Math.max(1.0, (double) pkts));
        double dupAckRate       = sdiv(dupAckCnt, Math.max(1.0, (double) pkts));
        double reqResRatio      = lenRes > 0 ? (double) lenReq / lenRes : 0;
        boolean ackOnly         = (len == 0 && pkts > 0);
        double avgPktSize       = pkts > 0 ? (double) len / pkts : 0;

        // RTO 비율
        long rttTotal = ackRttCntReq + ackRttCntRes;
        double rtoRate = rttTotal > 0 ? (ackRtoTotal * 100.0 / rttTotal) : 0.0;

        // TCP 플래그
        long syn       = nzl(r.getTcpFlagStatSynDelta());
        long synack    = nzl(r.getTcpFlagStatSynackDelta());
        long finReq    = nzl(r.getTcpFlagStatFinReqDelta());
        long finRes    = nzl(r.getTcpFlagStatFinResDelta());
        long finackReq = nzl(r.getTcpFlagStatFinackReqDelta());
        long finackRes = nzl(r.getTcpFlagStatFinackResDelta());
        long rstReq    = nzl(r.getTcpFlagStatRstReqDelta());
        long rstRes    = nzl(r.getTcpFlagStatRstResDelta());
        long pshReq    = nzl(r.getTcpFlagStatPshReqDelta());
        long pshRes    = nzl(r.getTcpFlagStatPshResDelta());

        String handshake =
                (syn > 0 && synack > 0) ? "성공" :
                        (syn > 0 && synack == 0) ? "응답없음" : "불명";

        String termination =
                (finReq > 0 || finRes > 0 || finackReq > 0 || finackRes > 0)
                        ? "정상종료(예상)" :
                        (rstReq > 0 || rstRes > 0) ? "RST 종료" : "불명";

        // 플래그 맵
        Map<String, Long> flags = new LinkedHashMap<>();
        flags.put("syn", syn);
        flags.put("synack", synack);
        flags.put("finReq", finReq);
        flags.put("finRes", finRes);
        flags.put("finackReq", finackReq);
        flags.put("finackRes", finackRes);
        flags.put("rstReq", rstReq);
        flags.put("rstRes", rstRes);
        flags.put("pshReq", pshReq);
        flags.put("pshRes", pshRes);

        // 품질 카운트
        Map<String, Long> quality = new LinkedHashMap<>();
        quality.put("retransCnt", rtCnt);
        quality.put("oooCnt", oooCnt);
        quality.put("lostCnt", lostCnt);
        quality.put("dupAckCnt", dupAckCnt);
        quality.put("ackLostCnt", ackLostCnt);
        quality.put("csumCnt", csumCnt);
        quality.put("zeroWinCnt", zeroWinCnt);
        quality.put("winFullCnt", winFullCnt);
        quality.put("winUpdateCnt", winUpdateCnt);
        quality.put("ackRtoTotal", ackRtoTotal);

        // 배지
        Map<String, String> badges = new HashMap<>();
        badges.put("retrans", level(retransRateBytes, retransWarn, retransCrit));
        badges.put("ooo", level(oooRatePkts, oooWarn, oooCrit));
        badges.put("loss", level(lossRatePkts, lossWarn, lossCrit));
        badges.put("csum", csumCnt > 0 ? "warn" : "ok");
        badges.put("win", (zeroWinCnt > 0 || winFullCnt > 0) ? "warn" : "ok");
        // RTO 배지
        badges.put("rto",
                ackRtoTotal >= rtoCrit ? "crit" :
                        ackRtoTotal >= rtoWarn ? "warn" : "ok");
        // 오버헤드 배지
        badges.put("overhead", overheadRate > 30 ? "warn" : "ok");

        // 품질 점수 계산
        var qualityScore = calculateQualityScore(
                retransRatePkts, lossRatePkts, ackRtoTotal,
                zeroWinCnt, rstReq, rstRes
        );

        // 진단 메시지 생성
        Map<String, String> diagnostics = buildDiagnostics(
                retransRatePkts, lossRatePkts, oooRatePkts, dupAckRate,
                ackRtoTotal, rtoRate, zeroWinCnt, winFullCnt,
                overheadRate, termination, qualityScore.grade()
        );

        // 환경 정보
        var env = new TcpMetricsDTO.Environment(
                r.getCountryNameReq(), r.getCountryNameRes(),
                r.getContinentNameReq(), r.getContinentNameRes(),
                r.getDomesticPrimaryNameReq(), r.getDomesticPrimaryNameRes(),
                r.getDomesticSub1NameReq(), r.getDomesticSub1NameRes(),
                r.getDomesticSub2NameReq(), r.getDomesticSub2NameRes()
        );

        // DTO 생성
        return new TcpMetricsDTO(
                r.getRowKey(),
                r.getSrcMac(), r.getDstMac(),
                r.getSrcIp(), r.getSrcPort(),
                r.getDstIp(), r.getDstPort(),
                r.getNdpiProtocolApp(),
                r.getNdpiProtocolMaster(),
                r.getSniHostname(),

                // 시간 정보
                beg, end,
                tsFirst, tsLast, tsExpired,
                durSec,

                // 세션 상태
                expired, expiredByTimeout, sessionTimeout,
                null, null, null,  // stoppedTransaction 필드들

                // 속도
                bps,

                // 트래픽 통계
                len, lenReq, lenRes,
                pkts, pktsReq, pktsRes,

                // PDU
                lenPdu, lenPduReq, lenPduRes,
                pktsPdu, pktsPduReq, pktsPduRes,
                overhead, overheadRate,

                // 재전송
                rtCnt, rtCntReq, rtCntRes,
                rtLen, rtLenReq, rtLenRes,
                retransRateBytes, retransRatePkts,

                // 순서 오류
                oooCnt, oooCntReq, oooCntRes,
                oooLen, 0L, 0L,
                oooRatePkts,

                // 패킷 손실
                lostCnt, lostCntReq, lostCntRes,
                lostLen, 0L, 0L,
                lossRatePkts,

                // 중복 ACK
                dupAckCnt, dupAckCntReq, dupAckCntRes,
                0L, dupAckRate,

                // ACK 손실
                ackLostCnt, ackLostCntReq, ackLostCntRes,
                0L,

                // 체크섬 에러
                csumCnt, csumCntReq, csumCntRes,
                csumLen, 0L, 0L,
                csumRatePkts,

                // 윈도우
                zeroWinCnt, zeroWinCntReq, zeroWinCntRes,
                winFullCnt, winFullCntReq, winFullCntRes,
                winUpdateCnt, 0L, 0L,

                // RTT/RTO
                ackRttCntReq, ackRttCntRes,
                ackRtoCntReq, ackRtoCntRes,
                ackRtoTotal, rtoRate,

                // 패턴
                reqResRatio, ackOnly, handshake, termination,
                avgPktSize,

                // 맵들
                flags, quality,
                qualityScore,
                badges,
                diagnostics,
                env
        );
    }

    // 품질 점수 계산
    private TcpMetricsDTO.QualityScore calculateQualityScore(
            double retransRate, double lossRate, long rtoTotal,
            long zeroWinCnt, long rstReq, long rstRes) {

        int score = 100;
        StringBuilder issues = new StringBuilder();

        // 재전송
        if (retransRate >= retransCrit * 100) {
            score -= 30;
            issues.append("심각한 재전송, ");
        } else if (retransRate >= retransWarn * 100) {
            score -= 15;
            issues.append("재전송 증가, ");
        }

        // 패킷 손실
        if (lossRate >= lossCrit * 100) {
            score -= 20;
            issues.append("높은 패킷 손실, ");
        } else if (lossRate >= lossWarn * 100) {
            score -= 10;
            issues.append("패킷 손실 발생, ");
        }

        // RTO
        if (rtoTotal >= rtoCrit) {
            score -= 25;
            issues.append("빈번한 타임아웃, ");
        } else if (rtoTotal >= rtoWarn) {
            score -= 12;
            issues.append("타임아웃 발생, ");
        }

        // Zero Window
        if (zeroWinCnt > 0) {
            score -= 10;
            issues.append("버퍼 부족, ");
        }

        // RST
        if (rstReq > 0 || rstRes > 0) {
            score -= 3;
            issues.append("비정상 종료, ");
        }

        score = Math.max(0, score);

        String grade =
                score >= 90 ? "Excellent" :
                        score >= 70 ? "Good" :
                                score >= 50 ? "Fair" :
                                        score >= 30 ? "Poor" : "Critical";

        String summary = issues.length() > 0
                ? issues.substring(0, issues.length() - 2)
                : "양호한 연결 품질";

        return new TcpMetricsDTO.QualityScore(score, grade, summary);
    }

    // 진단 메시지 생성
    private Map<String, String> buildDiagnostics(
            double retransRate, double lossRate, double oooRate, double dupAckRate,
            long rtoTotal, double rtoRate, long zeroWinCnt, long winFullCnt,
            double overheadRate, String termination, String grade) {

        Map<String, String> m = new LinkedHashMap<>();

        // 품질 등급
        m.put("quality", "TCP 연결 품질: " + grade);

        // RTO 타임아웃
        if (rtoTotal > 0) {
            String severity = rtoTotal >= rtoCrit ? "CRIT" :
                    (rtoTotal >= rtoWarn ? "WARN" : "INFO");
            m.put("rto", String.format("[%s] RTO 타임아웃 %d회 발생",
                    severity, rtoTotal));

            if (rtoRate > 10) {
                m.put("rtoRate", String.format(
                        "RTT 대비 RTO 비율 %.1f%% (네트워크 매우 불안정)", rtoRate));
            }
        }

        // 재전송
        if (retransRate >= retransCrit * 100) {
            m.put("retransCrit", String.format(
                    "재전송율 %.2f%% (심각 - 기준 %.1f%%)",
                    retransRate, retransCrit * 100));
        } else if (retransRate >= retransWarn * 100) {
            m.put("retransWarn", String.format(
                    "재전송율 %.2f%% (경고 - 기준 %.1f%%)",
                    retransRate, retransWarn * 100));
        }

        // 패킷 손실
        if (lossRate >= lossCrit * 100) {
            m.put("lossCrit", String.format(
                    "패킷 손실률 %.3f%% (심각)", lossRate));
        } else if (lossRate > 0) {
            m.put("loss", String.format(
                    "패킷 손실 발생 (%.3f%%)", lossRate));
        }

        // 순서 오류
        if (oooRate > 1.0) {
            m.put("outOfOrder", String.format(
                    "순서 오류율 %.2f%% (네트워크 경로 문제 가능)", oooRate));
        }

        // 중복 ACK
        if (dupAckRate > 1.0) {
            m.put("dupAck", String.format(
                    "중복 ACK 비율 %.2f%% (빠른 재전송 트리거)", dupAckRate));
        }

        // Zero Window
        if (zeroWinCnt > 0) {
            m.put("zeroWin", String.format(
                    "Zero Window %d회 (수신측 버퍼 부족)", zeroWinCnt));
        }

        // Window Full
        if (winFullCnt > 0) {
            m.put("winFull", String.format(
                    "Window Full %d회 (송신측 혼잡)", winFullCnt));
        }

        // 오버헤드
        if (overheadRate > 30) {
            m.put("overhead", String.format(
                    "TCP 오버헤드 %.1f%% (작은 패킷 많음)", overheadRate));
        }

        // 연결 종료
        if ("RST 종료".equals(termination)) {
            m.put("rst", "비정상 종료 (RST 플래그)");
        } else if ("정상종료(예상)".equals(termination)) {
            m.put("fin", "정상 종료 (FIN/FIN-ACK)");
        }

        return m;
    }

    // util
    private static double nz(Double v) {
        return v == null ? 0 : v;
    }

    private static long nzl(Long v) {
        return v == null ? 0L : v;
    }

    private static double sdiv(double a, double b) {
        return b > 0 ? a / b : 0;
    }

    private static String level(double v, double warn, double crit) {
        return v >= crit ? "crit" : v >= warn ? "warn" : "ok";
    }
}
