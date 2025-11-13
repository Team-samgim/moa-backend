package com.moa.api.detail.tcp.service;

import com.moa.api.detail.tcp.dto.TcpMetricsDTO;
import com.moa.api.detail.tcp.repository.TcpRowSlice;
import com.moa.api.detail.tcp.repository.TcpSampleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
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

    public TcpMetricsService(TcpSampleRepository repo) { this.repo = repo; }

    public TcpMetricsDTO getByRowKey(String rowKey) {
        TcpRowSlice r = repo.findSlice(rowKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "row not found"));

        double beg = nz(r.getTsSampleBegin());
        double end = nz(r.getTsSampleEnd());
        double durSec = Math.max(0.001, end - beg);

        long len     = nzl(r.getLenDelta());
        long lenReq  = nzl(r.getLenReqDelta());
        long lenRes  = nzl(r.getLenResDelta());
        long pkts    = nzl(r.getPktsDelta());
        long pktsReq = nzl(r.getPktsReqDelta());
        long pktsRes = nzl(r.getPktsResDelta());

        long rtLen       = nzl(r.getRetransmissionLenDelta());
        long rtCnt       = nzl(r.getRetransmissionCntDelta());
        long oooCnt      = nzl(r.getOutOfOrderCntDelta());
        long lostCnt     = nzl(r.getLostSegCntDelta());
        long csumCnt     = nzl(r.getChecksumErrorCntDelta());
        long zeroWinCnt  = nzl(r.getZeroWinCntDelta());
        long winFullCnt  = nzl(r.getWindowFullCntDelta());
        long winUpdateCnt= nzl(r.getWinUpdateCntDelta());

        double bps = (len * 8.0) / durSec;

        double retransRateBytes = sdiv(rtLen,  Math.max(1.0, (double) len));
        double oooRatePkts      = sdiv(oooCnt, Math.max(1.0, (double) pkts));
        double lossRatePkts     = sdiv(lostCnt,Math.max(1.0, (double) pkts));
        double csumRatePkts     = sdiv(csumCnt,Math.max(1.0, (double) pkts));

        double reqResRatio = lenRes > 0 ? (double) lenReq / lenRes : 0;
        boolean ackOnly = (len == 0 && pkts > 0);

        // 플래그들 Long으로
        long syn      = nzl(r.getTcpFlagStatSynDelta());
        long synack   = nzl(r.getTcpFlagStatSynackDelta());
        long finReq   = nzl(r.getTcpFlagStatFinReqDelta());
        long finRes   = nzl(r.getTcpFlagStatFinResDelta());
        long finackReq= nzl(r.getTcpFlagStatFinackReqDelta());
        long finackRes= nzl(r.getTcpFlagStatFinackResDelta());
        long rstReq   = nzl(r.getTcpFlagStatRstReqDelta());
        long rstRes   = nzl(r.getTcpFlagStatRstResDelta());
        long pshReq   = nzl(r.getTcpFlagStatPshReqDelta());
        long pshRes   = nzl(r.getTcpFlagStatPshResDelta());

        String handshake =
                (syn > 0 && synack > 0) ? "성공" :
                        (syn > 0 && synack == 0) ? "응답없음" : "불명";

        String termination =
                (finReq > 0 || finRes > 0 || finackReq > 0 || finackRes > 0) ? "정상종료(예상)" :
                        (rstReq > 0 || rstRes > 0) ? "RST 종료" : "불명";

        Map<String, Long> flags = new java.util.LinkedHashMap<>();
        flags.put("syn", syn); flags.put("synack", synack);
        flags.put("finReq", finReq); flags.put("finRes", finRes);
        flags.put("finackReq", finackReq); flags.put("finackRes", finackRes);
        flags.put("rstReq", rstReq); flags.put("rstRes", rstRes);
        flags.put("pshReq", pshReq); flags.put("pshRes", pshRes);

        Map<String, Long> quality = new java.util.LinkedHashMap<>();
        quality.put("retransCnt", rtCnt);
        quality.put("oooCnt", oooCnt);
        quality.put("lostCnt", lostCnt);
        quality.put("csumCnt", csumCnt);
        quality.put("zeroWinCnt", zeroWinCnt);
        quality.put("winFullCnt", winFullCnt);
        quality.put("winUpdateCnt", winUpdateCnt);

        Map<String, String> badges = new HashMap<>();
        badges.put("retrans", level(retransRateBytes, retransWarn, retransCrit));
        badges.put("ooo",     level(oooRatePkts,      oooWarn,     oooCrit));
        badges.put("loss",    level(lossRatePkts,     lossWarn,    lossCrit));
        badges.put("csum",    csumCnt > 0 ? "warn" : "ok");
        badges.put("win",     (zeroWinCnt > 0 || winFullCnt > 0) ? "warn" : "ok");

        return new TcpMetricsDTO(
                r.getRowKey(),
                r.getSrcIp(), r.getSrcPort(),
                r.getDstIp(), r.getDstPort(),
                r.getNdpiProtocolApp(), r.getNdpiProtocolMaster(), r.getSniHostname(),
                durSec, bps,
                len, lenReq, lenRes,
                pkts, pktsReq, pktsRes,
                retransRateBytes, oooRatePkts, lossRatePkts, csumRatePkts,
                reqResRatio, ackOnly, handshake, termination,
                flags, quality, badges
        );
    }

    // util
    private static double nz(Double v){ return v == null ? 0 : v; }
    private static long   nzl(Long v){ return v == null ? 0L : v; }
    private static double sdiv(double a, double b){ return b > 0 ? a / b : 0; }
    private static String level(double v, double warn, double crit) {
        return v >= crit ? "crit" : v >= warn ? "warn" : "ok";
    }
}