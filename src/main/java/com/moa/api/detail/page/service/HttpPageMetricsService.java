package com.moa.api.detail.page.service;

import com.moa.api.detail.page.dto.HttpPageMetricsDTO;
import com.moa.api.detail.page.repository.HttpPageAgg;
import com.moa.api.detail.page.repository.HttpPageSampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HttpPageMetricsService {

    private final HttpPageSampleRepository repo;

    // 임계치
    private static final double SLOW_PAGE_SEC_WARN = 3.0;
    private static final double CONNECT_MS_WARN    = 0.30;  // 300ms
    private static final double APP_MS_WARN        = 0.80;  // 800ms
    private static final double RETRANS_RATE_WARN  = 0.005; // 0.5%
    private static final double RETRANS_RATE_CRIT  = 0.02;  // 2%
    private static final double LOSS_RATE_WARN     = 0.001; // 0.1%
    private static final double CSUM_RATE_WARN     = 0.0001;// 0.01%

    public Optional<HttpPageMetricsDTO> getMetrics(String rowKey) {
        // Agg만 사용 (RowSlice는 필요하면 나중에 별도 DTO에 추가)
        return repo.findAgg(rowKey).map(this::toDTO);
    }

    private HttpPageMetricsDTO toDTO(HttpPageAgg a) {
        boolean https = a.getIsHttps() != null && a.getIsHttps() != 0;

        double durSec  = nz(a.getDurSec());
        long bytes     = nz(a.getBytes());
        long bytesReq  = nz(a.getBytesReq());
        long bytesRes  = nz(a.getBytesRes());

        long pkts      = nz(a.getPkts());
        long pktsReq   = nz(a.getPktsReq());
        long pktsRes   = nz(a.getPktsRes());

        long retrans   = nz(a.getRetransmissionCnt());
        long ooo       = nz(a.getOutOfOrderCnt());
        long loss      = nz(a.getLostSegCnt());
        long csum      = nz(a.getChecksumErrorCnt());

        double bps     = (durSec > 0.0) ? (bytes * 8.0) / durSec : 0.0;

        double retransRate = rate(retrans, pkts);
        double oooRate     = rate(ooo, pkts);
        double lossRate    = rate(loss, pkts);
        double csumRate    = rate(csum, pkts);

        // 품질 카운트 맵
        Map<String, Integer> qualityCounts = new LinkedHashMap<>();
        qualityCounts.put("retrans", capInt(retrans));
        qualityCounts.put("ooo",     capInt(ooo));
        qualityCounts.put("loss",    capInt(loss));
        qualityCounts.put("csum",    capInt(csum));
        qualityCounts.put("dupAck",  capInt(nz(a.getDupAckCnt())));
        qualityCounts.put("winUpd",  capInt(nz(a.getWinUpdateCnt())));
        qualityCounts.put("zeroWin", capInt(nz(a.getZeroWinCnt())));
        qualityCounts.put("winFull", capInt(nz(a.getWindowFullCnt())));
        qualityCounts.put("ackLost", capInt(nz(a.getAckLostCnt())));

        // 배지
        Map<String, String> badges = new LinkedHashMap<>();
        if (https) badges.put("scheme", "HTTPS");
        Integer rc = a.getHttpResCode();
        if (rc != null) {
            if (rc >= 500)      badges.put("res", "5xx");
            else if (rc >= 400) badges.put("res", "4xx");
            else if (rc == 304) badges.put("res", "304");
            else if (rc >= 200) badges.put("res", "2xx");
        }
        if (nz(a.getTimeoutCnt()) > 0)           badges.put("timeout", ">0");
        if (nz(a.getIncompleteCnt()) > 0)        badges.put("incomplete", ">0");
        if (nz(a.getStoppedTransactionCnt())> 0) badges.put("stopped", ">0");
        if (retrans > 0) badges.put("retrans", ">0");
        if (loss > 0)    badges.put("loss",    ">0");
        if (csum > 0)    badges.put("csum",    ">0");

        // 타이밍 섹션
        var timings = new HttpPageMetricsDTO.Timings(
                nzD(a.getConnectAvg()), nzD(a.getConnectMin()), nzD(a.getConnectMax()),
                nzD(a.getReqMakingAvg()), nzD(a.getReqMakingSum()),
                nzD(a.getResInit()), nzD(a.getResInitGap()),
                nzD(a.getResApp()), nzD(a.getResAppGap()),
                nzD(a.getTransferReq()), nzD(a.getTransferReqGap()),
                nzD(a.getTransferRes()), nzD(a.getTransferResGap())
        );

        // 전송/품질 섹션
        double dupAckRate    = rate(nz(a.getDupAckCnt()), pkts);
        double winUpdRate    = rate(nz(a.getWinUpdateCnt()), pkts);
        double zeroWinRate   = rate(nz(a.getZeroWinCnt()), pkts);
        double winFullRate   = rate(nz(a.getWindowFullCnt()), pkts);
        double ackLostRate   = rate(nz(a.getAckLostCnt()), pkts);

        var transport = new HttpPageMetricsDTO.Transport(
                retrans, ooo, loss, csum,
                nz(a.getDupAckCnt()), nz(a.getWinUpdateCnt()), nz(a.getZeroWinCnt()), nz(a.getWindowFullCnt()), nz(a.getAckLostCnt()),
                round(dupAckRate, 5), round(winUpdRate, 5), round(zeroWinRate, 5), round(winFullRate, 5), round(ackLostRate, 5)
        );

        // HTTP 섹션
        var http = new HttpPageMetricsDTO.Http(
                a.getResCode1xxCnt(), a.getResCode2xxCnt(), a.getResCode3xxCnt(), a.getResCode304Cnt(), a.getResCode4xxCnt(), a.getResCode5xxCnt(),
                a.getReqMethodGetCnt(), a.getReqMethodPostCnt(), a.getReqMethodPutCnt(), a.getReqMethodDeleteCnt(),
                a.getReqMethodHeadCnt(), a.getReqMethodOptionsCnt(), a.getReqMethodPatchCnt(), a.getReqMethodTraceCnt(),
                a.getReqMethodConnectCnt(), a.getReqMethodOthCnt(),
                a.getHttpVersion(), a.getHttpVersionReq(), a.getHttpVersionRes(),
                a.getContentTypeHtmlCntReq(), a.getContentTypeHtmlCntRes(),
                a.getContentTypeCssCntReq(),  a.getContentTypeCssCntRes(),
                a.getContentTypeJsCntReq(),   a.getContentTypeJsCntRes(),
                a.getContentTypeImgCntReq(),  a.getContentTypeImgCntRes(),
                a.getContentTypeOthCntReq(),  a.getContentTypeOthCntRes(),
                // 크기/길이/메타
                nz(a.getPageHttpHeaderLenReq()), nz(a.getPageHttpHeaderLenRes()),
                nz(a.getPageHttpContentLenReq()), nz(a.getPageHttpContentLenRes()),
                nz(a.getPagePktLenReq()), nz(a.getPagePktLenRes()),
                nz(a.getHttpContentLengthReq()), nz(a.getHttpContentLengthRes()),
                a.getHttpReferer(), a.getHttpCookie(), a.getHttpUserAgent(),
                a.getHttpContentType()
        );

        // 환경 섹션
        var env = new HttpPageMetricsDTO.Environment(
                a.getCountryNameReq(), a.getCountryNameRes(),
                a.getContinentNameReq(), a.getContinentNameRes(),
                a.getUserAgentOperatingSystemName(),
                a.getUserAgentSoftwareName(),
                a.getUserAgentHardwareType(),
                a.getUserAgentLayoutEngineName()
        );

        // 진단 메시지
        Map<String, String> diag = buildDiagnostics(
                durSec, timings, retransRate, lossRate, csumRate,
                dupAckRate, zeroWinRate, winFullRate, a.getResCode5xxCnt()
        );

        return new HttpPageMetricsDTO(
                a.getRowKey(),
                a.getSrcIp(), a.getSrcPort(),
                a.getDstIp(), a.getDstPort(),
                a.getNdpiProtocolApp(), a.getNdpiProtocolMaster(),
                https,
                a.getHttpHost(), a.getHttpUri(),
                a.getHttpMethod(), a.getHttpResCode(), a.getHttpResPhrase(),
                round(durSec, 2), round(bps, 2),
                bytes, bytesReq, bytesRes,
                pkts, pktsReq, pktsRes,
                round(retransRate, 5), round(oooRate, 5), round(lossRate, 5), round(csumRate, 5),
                qualityCounts,
                badges,
                timings, transport, http, env, diag
        );
    }

    private Map<String, String> buildDiagnostics(double durSec, HttpPageMetricsDTO.Timings t,
                                                 double retransRate, double lossRate, double csumRate,
                                                 double dupAckRate, double zeroWinRate, double winFullRate,
                                                 Integer code5xxCnt) {
        Map<String,String> m = new LinkedHashMap<>();
        if (durSec > SLOW_PAGE_SEC_WARN) m.put("slow", "지연: 페이지 총 소요 " + durSec + "s");
        if (nzD(t.connectAvg()) > CONNECT_MS_WARN) m.put("connect", "연결 지연 가능: avg=" + round(nzD(t.connectAvg()),3) + "s");
        if (nzD(t.resApp()) > APP_MS_WARN) m.put("server", "서버 앱 처리 지연 가능: " + round(nzD(t.resApp()),3) + "s");
        if (lossRate > LOSS_RATE_WARN) m.put("loss", "패킷 손실 의심: " + round(lossRate*100,3) + "%");
        if (retransRate > RETRANS_RATE_CRIT) m.put("retrans", "재전송 높음(crit): " + round(retransRate*100,3) + "%");
        else if (retransRate > RETRANS_RATE_WARN) m.put("retrans", "재전송 주의: " + round(retransRate*100,3) + "%");
        if (csumRate > CSUM_RATE_WARN) m.put("csum", "TCP 체크섬 오류 의심: " + round(csumRate*100,3) + "%");
        if (zeroWinRate > 0) m.put("zeroWin", "Zero Window 발생: " + round(zeroWinRate*100,3) + "%");
        if (winFullRate > 0) m.put("winFull", "Window Full 발생: " + round(winFullRate*100,3) + "%");
        if (dupAckRate > 0) m.put("dupAck", "Dup ACK 다수: " + round(dupAckRate*100,3) + "%");
        if (nz(code5xxCnt) > 0) m.put("5xx", "서버 5xx 응답 존재");
        return m;
    }

    private long   nz(Long v)    { return v == null ? 0L   : v; }
    private int    nz(Integer v) { return v == null ? 0    : v; }
    private double nz(Double v)  { return v == null ? 0.0  : v; }
    private Double nzD(Double v) { return v == null ? 0.0  : v; }

    private double rate(long num, long den) { return den > 0 ? (double) num / den : 0.0; }
    private int    capInt(long v) { return (v > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) v; }
    private double round(double v, int scale) {
        double p = Math.pow(10, scale);
        return Math.round(v * p) / p;
    }
}
