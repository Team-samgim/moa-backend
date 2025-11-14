package com.moa.api.detail.uri.service;

import com.moa.api.detail.uri.dto.HttpUriMetricsDTO;
import com.moa.api.detail.uri.dto.HttpUriMetricsDTO.*;
import com.moa.api.detail.uri.repository.HttpUriSampleRepository;
import com.moa.api.detail.uri.repository.HttpUriRowSlice;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class HttpUriMetricsService {

    private final HttpUriSampleRepository repo;

    public HttpUriMetricsService(HttpUriSampleRepository repo) {
        this.repo = repo;
    }

    public HttpUriMetricsDTO getByRowKey(String rowKey) {
        HttpUriRowSlice r = repo.findSlice(rowKey)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "HTTP URI not found"));

        // === 기본 시간 계산 ===
        double tsFirst = nz(r.getTsFirst());
        double tsFrameArrival = nz(r.getTsFrameArrival());
        double tsFrameLandoff = nz(r.getTsFrameLandoff());

        // 요청 시간
        double reqPktFirst = nz(r.getTsReqPktFirst());
        double reqPktPush = nz(r.getTsReqPktPush());
        double reqPktLast = nz(r.getTsReqPktLast());
        double reqDelayTransfer = nz(r.getTsReqDelayTransfer());

        // 응답 시간
        double resPktFirst = nz(r.getTsResPktFirst());
        double resPktPush = nz(r.getTsResPktPush());
        double resPktLast = nz(r.getTsResPktLast());
        double resDelayTransfer = nz(r.getTsResDelayTransfer());

        // 처리 시간
        double resProcessFirst = nz(r.getTsResProcessFirst());
        double resProcessPush = nz(r.getTsResProcessPush());
        double resProcessLast = nz(r.getTsResProcessLast());

        // === 지연 시간 계산 (ms 단위) ===
        double responseTime = (resPktFirst - reqPktLast) * 1000;  // 응답 시간
        double transferTime = (resPktLast - resPktFirst) * 1000;  // 전송 시간
        double totalTime = (resPktLast - reqPktFirst) * 1000;     // 전체 시간

        // === 트래픽 통계 ===
        long pktLen = nzl(r.getPktLen());
        long pktLenReq = nzl(r.getPktLenReq());
        long pktLenRes = nzl(r.getPktLenRes());

        long httpLen = nzl(r.getHttpLen());
        long httpLenReq = nzl(r.getHttpLenReq());
        long httpLenRes = nzl(r.getHttpLenRes());

        long tcpLen = nzl(r.getTcpLen());
        long tcpLenReq = nzl(r.getTcpLenReq());
        long tcpLenRes = nzl(r.getTcpLenRes());

        long pktCnt = nzl(r.getPktCnt());
        long pktCntReq = nzl(r.getPktCntReq());
        long pktCntRes = nzl(r.getPktCntRes());

        // === 비율 계산 ===
        double reqResRatio = pktLenRes > 0 ? (double) pktLenReq / pktLenRes : 0;
        double httpOverhead = pktLen > 0 ? ((pktLen - httpLen) * 100.0 / pktLen) : 0;

        // === TCP 에러 집계 ===
        var retrans = buildTcpError(r.getRetransmissionCnt(), r.getRetransmissionCntReq(),
                r.getRetransmissionCntRes(), r.getRetransmissionLen(),
                r.getRetransmissionLenReq(), r.getRetransmissionLenRes(), pktCnt);

        var fastRetrans = buildTcpError(r.getFastRetransmissionLen() != null ? 1L : 0L, 0L, 0L,
                r.getFastRetransmissionLen(), r.getFastRetransmissionLenReq(),
                r.getFastRetransmissionLenRes(), pktCnt);

        var outOfOrder = buildTcpError(r.getOutOfOrderCnt(), r.getOutOfOrderCntReq(),
                r.getOutOfOrderCntRes(), r.getOutOfOrderLen(),
                r.getOutOfOrderLenReq(), r.getOutOfOrderLenRes(), pktCnt);

        var lostSeg = buildTcpError(r.getLostSegCnt(), r.getLostSegCntReq(),
                r.getLostSegCntRes(), r.getLostSegLen(),
                r.getLostSegLenReq(), r.getLostSegLenRes(), pktCnt);

        var ackLost = buildTcpError(r.getAckLostCnt(), r.getAckLostCntReq(),
                r.getAckLostCntRes(), r.getAckLostLen(),
                r.getAckLostLenReq(), r.getAckLostLenRes(), pktCnt);

        var checksumError = buildTcpError(r.getChecksumErrorCnt(), r.getChecksumErrorCntReq(),
                r.getChecksumErrorCntRes(), r.getChecksumErrorLen(),
                r.getChecksumErrorLenReq(), r.getChecksumErrorLenRes(), pktCnt);

        var zeroWin = buildTcpError(r.getZeroWinCnt(), r.getZeroWinCntReq(),
                r.getZeroWinCntRes(), r.getZeroWinLen(),
                r.getZeroWinLenReq(), r.getZeroWinLenRes(), pktCnt);

        var windowFull = buildTcpError(r.getWindowFullCnt(), r.getWindowFullCntReq(),
                r.getWindowFullCntRes(), 0L, 0L, 0L, pktCnt);

        var winUpdate = buildTcpError(r.getWinUpdateCnt(), r.getWinUpdateCntReq(),
                r.getWinUpdateCntRes(), r.getWinUpdateLen(),
                r.getWinUpdateLenReq(), r.getWinUpdateLenRes(), pktCnt);

        var dupAck = buildTcpError(r.getDupAckCnt(), r.getDupAckCntReq(),
                r.getDupAckCntRes(), r.getDupAckLen(),
                r.getDupAckLenReq(), r.getDupAckLenRes(), pktCnt);

        // === RTT/RTO ===
        long ackRttCntReq = nzl(r.getAckRttCntReq());
        long ackRttCntRes = nzl(r.getAckRttCntRes());
        long ackRtoCntReq = nzl(r.getAckRtoCntReq());
        long ackRtoCntRes = nzl(r.getAckRtoCntRes());
        long ackRtoTotal = ackRtoCntReq + ackRtoCntRes;

        // === TCP 전체 에러 ===
        long tcpErrorCnt = nzl(r.getTcpErrorCnt());
        long tcpErrorLen = nzl(r.getTcpErrorLen());
        double tcpErrorPercentage = nz(r.getTcpErrorPercentage());
        double tcpErrorLenPercentage = nz(r.getTcpErrorLenPercentage());

        // === 품질 점수 계산 ===
        var qualityScore = calculateQualityScore(
                retrans.rate(), lostSeg.rate(), ackRtoTotal,
                zeroWin.cnt(), responseTime, totalTime,
                toBoolean(r.getIsTimeout()), toBoolean(r.getIsIncomplete())
        );

        // === HTTP 요청 정보 ===
        var userAgentInfo = new UserAgentInfo(
                r.getUserAgentSoftwareName(),
                r.getUserAgentOperatingSystemName(),
                r.getUserAgentOperatingPlatform(),
                r.getUserAgentSoftwareType(),
                r.getUserAgentHardwareType(),
                r.getUserAgentLayoutEngineName()
        );

        var request = new HttpRequest(
                r.getHttpMethod(),
                r.getHttpVersionReq(),
                r.getHttpUri(),
                r.getHttpUriSplit(),
                r.getHttpHost(),
                r.getHttpReferer(),
                r.getHttpUserAgent(),
                r.getHttpCookie(),
                userAgentInfo,
                nzl(r.getContentTypeHtmlCntReq()),
                nzl(r.getContentTypeCssCntReq()),
                nzl(r.getContentTypeJsCntReq()),
                nzl(r.getContentTypeImgCntReq()),
                nzl(r.getContentTypeOthCntReq()),
                nzl(r.getHttpHeaderLenReq()),
                nzl(r.getHttpContentLenReq()),
                httpLenReq,
                pktCntReq,
                nzl(r.getReqCnt())
        );

        // === HTTP 응답 정보 ===
        var response = new HttpResponse(
                r.getHttpVersionRes(),
                r.getHttpResPhrase(),
                r.getHttpContentType(),
                r.getHttpLocation(),
                nzl(r.getContentTypeHtmlCntRes()),
                nzl(r.getContentTypeCssCntRes()),
                nzl(r.getContentTypeJsCntRes()),
                nzl(r.getContentTypeImgCntRes()),
                nzl(r.getContentTypeOthCntRes()),
                nzl(r.getHttpHeaderLenRes()),
                nzl(r.getHttpContentLenRes()),
                httpLenRes,
                pktCntRes,
                nzl(r.getResCnt())
        );

        // === 시간 분석 ===
        var timing = new TimingAnalysis(
                tsFirst, tsFrameArrival, tsFrameLandoff,
                reqPktFirst, reqPktPush, reqPktLast, reqDelayTransfer,
                resPktFirst, resPktPush, resPktLast, resDelayTransfer,
                resProcessFirst, resProcessPush, resProcessLast,
                responseTime, transferTime, totalTime
        );

        // === 트래픽 통계 ===
        var traffic = new TrafficStats(
                pktLen, pktCnt, httpLen, tcpLen,
                pktLenReq, pktCntReq, httpLenReq, tcpLenReq,
                pktLenRes, pktCntRes, httpLenRes, tcpLenRes,
                reqResRatio, httpOverhead
        );

        // === TCP 품질 ===
        var tcpQuality = new TcpQuality(
                retrans, fastRetrans, outOfOrder, lostSeg, ackLost, checksumError,
                zeroWin, windowFull, winUpdate, dupAck,
                ackRttCntReq, ackRttCntRes, ackRtoCntReq, ackRtoCntRes, ackRtoTotal,
                tcpErrorCnt, tcpErrorLen, tcpErrorPercentage, tcpErrorLenPercentage,
                qualityScore
        );

        // === 성능 메트릭 ===
        var performance = new Performance(
                nz(r.getMbps()), nz(r.getMbpsReq()), nz(r.getMbpsRes()),
                nz(r.getMbpsMin()), nz(r.getMbpsMax()),
                nz(r.getPps()), nz(r.getPpsReq()), nz(r.getPpsRes()),
                nz(r.getPpsMin()), nz(r.getPpsMax()),
                nzl(r.getPageIdx()), nzl(r.getUriIdx()), nzl(r.getRefererCnt())
        );

        // === 환경 정보 ===
        var env = new Environment(
                r.getCountryNameReq(), r.getCountryNameRes(),
                r.getContinentNameReq(), r.getContinentNameRes(),
                r.getDomesticPrimaryNameReq(), r.getDomesticPrimaryNameRes(),
                r.getDomesticSub1NameReq(), r.getDomesticSub1NameRes(),
                r.getDomesticSub2NameReq(), r.getDomesticSub2NameRes(),
                r.getSensorDeviceName(),
                r.getNdpiProtocolApp(), r.getNdpiProtocolMaster(),
                toBoolean(r.getIsHttps())
        );

        // === 상태 정보 ===
        boolean isStoppedTransaction = toBoolean(r.getIsStoppedTransaction());
        boolean isIncomplete = toBoolean(r.getIsIncomplete());
        boolean isTimeout = toBoolean(r.getIsTimeout());

        String connectionStatus = determineConnectionStatus(
                isStoppedTransaction, isIncomplete, isTimeout, qualityScore.grade()
        );

        var status = new Status(
                isStoppedTransaction, toBoolean(r.getIsStoppedTransactionReq()),
                toBoolean(r.getIsStoppedTransactionRes()),
                isIncomplete, toBoolean(r.getIsIncompleteReq()),
                toBoolean(r.getIsIncompleteRes()),
                isTimeout, toBoolean(r.getIsTimeoutReq()),
                toBoolean(r.getIsTimeoutRes()),
                connectionStatus
        );

        // === 진단 메시지 ===
        var diagnostics = buildDiagnostics(
                retrans.rate(), lostSeg.rate(), outOfOrder.rate(),
                ackRtoTotal, responseTime, transferTime, totalTime,
                zeroWin.cnt(), windowFull.cnt(),
                isTimeout, isIncomplete, isStoppedTransaction,
                r.getHttpResPhrase(), qualityScore.grade()
        );

        return new HttpUriMetricsDTO(
                r.getRowKey(),
                r.getSrcIp(), r.getSrcPort(), r.getSrcMac(),
                r.getDstIp(), r.getDstPort(), r.getDstMac(),
                request, response,
                timing, traffic, tcpQuality, performance,
                env, status, diagnostics
        );
    }

    // === Helper Methods ===

    private TcpError buildTcpError(Long cnt, Long cntReq, Long cntRes,
                                   Long len, Long lenReq, Long lenRes,
                                   long totalPkts) {
        long c = nzl(cnt);
        double rate = totalPkts > 0 ? (c * 100.0 / totalPkts) : 0;
        return new TcpError(
                c, nzl(cntReq), nzl(cntRes),
                nzl(len), nzl(lenReq), nzl(lenRes),
                rate
        );
    }

    private QualityScore calculateQualityScore(
            double retransRate, double lossRate, long rtoTotal,
            long zeroWinCnt, double responseTime, double totalTime,
            boolean isTimeout, boolean isIncomplete) {

        int score = 100;
        StringBuilder issues = new StringBuilder();

        // 재전송
        if (retransRate >= 5) {
            score -= 30;
            issues.append("높은 재전송율, ");
        } else if (retransRate >= 2) {
            score -= 15;
            issues.append("재전송 발생, ");
        }

        // 패킷 손실
        if (lossRate >= 1) {
            score -= 25;
            issues.append("패킷 손실, ");
        } else if (lossRate > 0) {
            score -= 10;
            issues.append("일부 패킷 손실, ");
        }

        // RTO 타임아웃
        if (rtoTotal >= 5) {
            score -= 20;
            issues.append("빈번한 타임아웃, ");
        } else if (rtoTotal > 0) {
            score -= 10;
            issues.append("타임아웃 발생, ");
        }

        // Zero Window
        if (zeroWinCnt > 0) {
            score -= 10;
            issues.append("수신 버퍼 부족, ");
        }

        // 응답 시간
        if (responseTime > 3000) {
            score -= 15;
            issues.append("응답 지연, ");
        } else if (responseTime > 1000) {
            score -= 5;
            issues.append("응답 시간 증가, ");
        }

        // 타임아웃/불완전
        if (isTimeout) {
            score -= 20;
            issues.append("타임아웃, ");
        }
        if (isIncomplete) {
            score -= 15;
            issues.append("불완전한 전송, ");
        }

        score = Math.max(0, score);

        String grade =
                score >= 90 ? "Excellent" :
                        score >= 70 ? "Good" :
                                score >= 50 ? "Fair" :
                                        score >= 30 ? "Poor" : "Critical";

        String summary = issues.length() > 0
                ? issues.substring(0, issues.length() - 2)
                : "양호한 HTTP 통신 품질";

        return new QualityScore(score, grade, summary);
    }

    private String determineConnectionStatus(
            boolean isStoppedTransaction, boolean isIncomplete,
            boolean isTimeout, String grade) {

        if (isTimeout) return "🔴 타임아웃";
        if (isStoppedTransaction) return "⚠️ 중단됨";
        if (isIncomplete) return "⚠️ 불완전";

        return switch (grade) {
            case "Excellent", "Good" -> "✅ 정상";
            case "Fair" -> "⚠️ 주의";
            case "Poor" -> "🟠 불량";
            case "Critical" -> "🔴 심각";
            default -> "ℹ️ 알수없음";
        };
    }

    private Map<String, String> buildDiagnostics(
            double retransRate, double lossRate, double oooRate,
            long rtoTotal, double responseTime, double transferTime, double totalTime,
            long zeroWinCnt, long windowFullCnt,
            boolean isTimeout, boolean isIncomplete, boolean isStoppedTransaction,
            String httpResPhrase, String grade) {

        Map<String, String> m = new LinkedHashMap<>();

        // 품질 등급
        String emoji = switch (grade) {
            case "Excellent" -> "✅";
            case "Good" -> "👍";
            case "Fair" -> "⚠️";
            case "Poor" -> "🔴";
            case "Critical" -> "🚨";
            default -> "ℹ️";
        };
        m.put("quality", emoji + " HTTP 통신 품질: " + grade);

        // HTTP 상태
        if (httpResPhrase != null && !httpResPhrase.isEmpty()) {
            if (httpResPhrase.equals("OK")) {
                m.put("httpStatus", "✅ HTTP 응답: " + httpResPhrase);
            } else if (httpResPhrase.startsWith("2")) {
                m.put("httpStatus", "✅ HTTP 응답: " + httpResPhrase);
            } else if (httpResPhrase.startsWith("3")) {
                m.put("httpStatus", "ℹ️ 리다이렉트: " + httpResPhrase);
            } else if (httpResPhrase.startsWith("4")) {
                m.put("httpStatus", "⚠️ 클라이언트 에러: " + httpResPhrase);
            } else if (httpResPhrase.startsWith("5")) {
                m.put("httpStatus", "🔴 서버 에러: " + httpResPhrase);
            }
        }

        // 응답 시간
        if (responseTime > 3000) {
            m.put("responseTime", String.format(
                    "🚨 응답 시간 %.0fms (매우 느림)", responseTime));
        } else if (responseTime > 1000) {
            m.put("responseTime", String.format(
                    "⚠️ 응답 시간 %.0fms (느림)", responseTime));
        } else if (responseTime > 0) {
            m.put("responseTime", String.format(
                    "✅ 응답 시간 %.0fms", responseTime));
        }

        // 타임아웃/불완전
        if (isTimeout) {
            m.put("timeout", "🔴 타임아웃 발생 - 연결 실패");
        }
        if (isIncomplete) {
            m.put("incomplete", "⚠️ 불완전한 전송 - 데이터 누락 가능");
        }
        if (isStoppedTransaction) {
            m.put("stopped", "⚠️ 트랜잭션 중단");
        }

        // TCP 에러
        if (retransRate >= 5) {
            m.put("retrans", String.format(
                    "🚨 재전송율 %.2f%% (심각)", retransRate));
        } else if (retransRate > 0) {
            m.put("retrans", String.format(
                    "⚠️ 재전송율 %.2f%%", retransRate));
        }

        if (lossRate >= 1) {
            m.put("loss", String.format(
                    "🚨 패킷 손실 %.2f%% (심각)", lossRate));
        } else if (lossRate > 0) {
            m.put("loss", String.format(
                    "⚠️ 패킷 손실 %.2f%%", lossRate));
        }

        if (rtoTotal > 0) {
            m.put("rto", String.format(
                    "⚠️ RTO 타임아웃 %d회", rtoTotal));
        }

        if (zeroWinCnt > 0) {
            m.put("zeroWin", String.format(
                    "⚠️ Zero Window %d회 (수신측 버퍼 부족)", zeroWinCnt));
        }

        if (windowFullCnt > 0) {
            m.put("winFull", String.format(
                    "ℹ️ Window Full %d회 (송신측 혼잡)", windowFullCnt));
        }

        return m;
    }

    private static double nz(Double v) {
        return v == null ? 0 : v;
    }

    private static long nzl(Long v) {
        return v == null ? 0L : v;
    }

    private static boolean toBoolean(Integer v) {
        return v != null && v > 0;
    }
}