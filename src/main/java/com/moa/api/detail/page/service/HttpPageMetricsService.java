package com.moa.api.detail.page.service;

import com.moa.api.detail.page.dto.HttpPageMetricsDTO;
import com.moa.api.detail.page.exception.HttpPageException;
import com.moa.api.detail.page.repository.HttpPageRowSlice;
import com.moa.api.detail.page.repository.HttpPageSampleRepository;
import com.moa.api.detail.page.validation.HttpPageValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HTTP Page Metrics Service
 * 235개 컬럼을 계층적 DTO로 변환
 *
 * 개선사항:
 * - HttpPageValidator 추가
 * - HttpPageException 사용
 * - 로깅 추가
 * - @Transactional(readOnly = true)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HttpPageMetricsService {

    private final HttpPageSampleRepository repository;
    private final HttpPageValidator validator;

    /**
     * rowKey로 HTTP Page 메트릭 조회
     */
    public HttpPageMetricsDTO getByRowKey(String rowKey) {
        // Validation
        validator.validateRowKey(rowKey);

        log.debug("Fetching HTTP Page metrics for rowKey: {}", rowKey);

        try {
            HttpPageRowSlice r = repository.findSlice(rowKey)
                    .orElseThrow(() -> new HttpPageException(
                            HttpPageException.ErrorCode.ROW_KEY_NOT_FOUND,
                            "rowKey: " + rowKey));

            HttpPageMetricsDTO dto = buildDTO(r);

            log.debug("Successfully fetched HTTP Page metrics for rowKey: {}", rowKey);
            return dto;

        } catch (HttpPageException e) {
            throw e;
        } catch (Exception e) {
            log.error("Database error while fetching HTTP Page metrics for rowKey: {}", rowKey, e);
            throw new HttpPageException(
                    HttpPageException.ErrorCode.DATABASE_ERROR,
                    e
            );
        }
    }

    /**
     * DTO 빌드
     */
    private HttpPageMetricsDTO buildDTO(HttpPageRowSlice r) {
        return new HttpPageMetricsDTO(
                r.getRowKey(),
                r.getSrcIp(),
                r.getDstIp(),
                r.getSrcPort(),
                r.getDstPort(),
                r.getSrcMac(),
                r.getDstMac(),
                r.getPageIdx(),
                r.getTsServer(),

                // HTTP 기본 정보
                r.getHttpMethod(),
                r.getHttpVersion(),
                r.getHttpHost(),
                r.getHttpUri(),
                r.getHttpResCode(),
                r.getHttpResPhrase(),
                r.getHttpContentType(),
                r.getHttpUserAgent(),
                r.getHttpCookie(),
                r.getHttpLocation(),
                r.getHttpReferer(),
                toBoolean(r.getIsHttps()),

                // 페이지 통계
                nzl(r.getPageSessionCnt()),
                nzl(r.getPageTcpConnectCnt()),
                nzl(r.getUriCnt()),
                nzl(r.getHttpUriCnt()),
                nzl(r.getHttpsUriCnt()),
                nzl(r.getPageErrorCnt()),

                // 프로토콜/센서 정보
                r.getNdpiProtocolApp(),
                r.getNdpiProtocolMaster(),
                r.getSensorDeviceName(),

                // 중첩 구조
                buildTiming(r),
                buildMethods(r),
                buildStatusCodes(r),
                buildTcpQuality(r),
                buildPerformance(r),
                buildTraffic(r),
                buildEnvironment(r),
                buildUserAgentInfo(r)
        );
    }

    /**
     * Timing 빌드
     */
    private HttpPageMetricsDTO.Timing buildTiming(HttpPageRowSlice r) {
        return new HttpPageMetricsDTO.Timing(
                r.getTsFirst(),
                r.getTsPageBegin(),
                r.getTsPageEnd(),
                r.getTsPageReqSyn(),
                r.getTsPage(),
                r.getTsPageGap(),
                r.getTsPageResInit(),
                r.getTsPageResInitGap(),
                r.getTsPageResApp(),
                r.getTsPageResAppGap(),
                r.getTsPageRes(),
                r.getTsPageResGap(),
                r.getTsPageTransferReq(),
                r.getTsPageTransferReqGap(),
                r.getTsPageTransferRes(),
                r.getTsPageTransferResGap(),
                r.getTsPageReqMakingSum(),
                r.getTsPageReqMakingAvg(),
                r.getTsPageTcpConnectSum(),
                r.getTsPageTcpConnectMin(),
                r.getTsPageTcpConnectMax(),
                r.getTsPageTcpConnectAvg()
        );
    }

    /**
     * Methods 빌드
     */
    private HttpPageMetricsDTO.Methods buildMethods(HttpPageRowSlice r) {
        long getCnt = nzl(r.getReqMethodGetCnt());
        long postCnt = nzl(r.getReqMethodPostCnt());
        long putCnt = nzl(r.getReqMethodPutCnt());
        long deleteCnt = nzl(r.getReqMethodDeleteCnt());
        long headCnt = nzl(r.getReqMethodHeadCnt());
        long optionsCnt = nzl(r.getReqMethodOptionsCnt());
        long patchCnt = nzl(r.getReqMethodPatchCnt());
        long traceCnt = nzl(r.getReqMethodTraceCnt());
        long connectCnt = nzl(r.getReqMethodConnectCnt());
        long othCnt = nzl(r.getReqMethodOthCnt());

        long getCntError = nzl(r.getReqMethodGetCntError());
        long postCntError = nzl(r.getReqMethodPostCntError());
        long putCntError = nzl(r.getReqMethodPutCntError());
        long deleteCntError = nzl(r.getReqMethodDeleteCntError());
        long headCntError = nzl(r.getReqMethodHeadCntError());
        long optionsCntError = nzl(r.getReqMethodOptionsCntError());
        long patchCntError = nzl(r.getReqMethodPatchCntError());
        long traceCntError = nzl(r.getReqMethodTraceCntError());
        long connectCntError = nzl(r.getReqMethodConnectCntError());
        long othCntError = nzl(r.getReqMethodOthCntError());

        boolean hasErrors = (getCntError + postCntError + putCntError + deleteCntError +
                headCntError + optionsCntError + patchCntError + traceCntError +
                connectCntError + othCntError) > 0;

        return new HttpPageMetricsDTO.Methods(
                getCnt, postCnt, putCnt, deleteCnt, headCnt,
                optionsCnt, patchCnt, traceCnt, connectCnt, othCnt,
                getCntError, postCntError, putCntError, deleteCntError, headCntError,
                optionsCntError, patchCntError, traceCntError, connectCntError, othCntError,
                hasErrors
        );
    }

    /**
     * StatusCodes 빌드
     */
    private HttpPageMetricsDTO.StatusCodes buildStatusCodes(HttpPageRowSlice r) {
        return new HttpPageMetricsDTO.StatusCodes(
                nzl(r.getResCode1xxCnt()),
                nzl(r.getResCode2xxCnt()),
                nzl(r.getResCode304Cnt()),
                nzl(r.getResCode3xxCnt()),
                nzl(r.getResCode401Cnt()),
                nzl(r.getResCode403Cnt()),
                nzl(r.getResCode404Cnt()),
                nzl(r.getResCode4xxCnt()),
                nzl(r.getResCode5xxCnt()),
                nzl(r.getResCodeOthCnt())
        );
    }

    /**
     * TcpQuality 빌드
     */
    private HttpPageMetricsDTO.TcpQuality buildTcpQuality(HttpPageRowSlice r) {
        long tcpSessionCnt = nzl(r.getPageTcpConnectCnt());
        long tcpErrorSessionCnt = nzl(r.getConnErrSessionCnt());

        long totalTcpCnt = nzl(r.getPageTcpCnt());
        long totalTcpLen = nzl(r.getPageTcpLen());

        long tcpErrorCnt = nzl(r.getTcpErrorCnt());
        long tcpErrorLen = nzl(r.getTcpErrorLen());

        Double tcpErrorSessionRatio = safeRatio(tcpErrorSessionCnt, tcpSessionCnt);
        Double tcpErrorCntRatio = safeRatio(tcpErrorCnt, totalTcpCnt);
        Double tcpErrorLenRatio = safeRatio(tcpErrorLen, totalTcpLen);

        Double tcpErrorPercentage = normalizePct(r.getTcpErrorPercentage());
        Double tcpErrorPercentageReq = normalizePct(r.getTcpErrorPercentageReq());
        Double tcpErrorPercentageRes = normalizePct(r.getTcpErrorPercentageRes());
        Double pageErrorPercentage = normalizePct(r.getPageErrorPercentage());

        return new HttpPageMetricsDTO.TcpQuality(
                tcpSessionCnt,
                tcpErrorSessionCnt,
                tcpErrorSessionRatio,
                tcpErrorCntRatio,
                tcpErrorLenRatio,

                tcpErrorCnt,
                nzl(r.getTcpErrorCntReq()),
                nzl(r.getTcpErrorCntRes()),
                tcpErrorLen,
                nzl(r.getTcpErrorLenReq()),
                nzl(r.getTcpErrorLenRes()),

                // 재전송
                nzl(r.getRetransmissionCnt()),
                nzl(r.getRetransmissionCntReq()),
                nzl(r.getRetransmissionCntRes()),
                nzl(r.getRetransmissionLen()),
                nzl(r.getRetransmissionLenReq()),
                nzl(r.getRetransmissionLenRes()),

                // 순서 오류
                nzl(r.getOutOfOrderCnt()),
                nzl(r.getOutOfOrderCntReq()),
                nzl(r.getOutOfOrderCntRes()),
                nzl(r.getOutOfOrderLen()),
                nzl(r.getOutOfOrderLenReq()),
                nzl(r.getOutOfOrderLenRes()),

                // 패킷 손실
                nzl(r.getLostSegCnt()),
                nzl(r.getLostSegCntReq()),
                nzl(r.getLostSegCntRes()),
                nzl(r.getLostSegLen()),
                nzl(r.getLostSegLenReq()),
                nzl(r.getLostSegLenRes()),

                // ACK 손실
                nzl(r.getAckLostCnt()),
                nzl(r.getAckLostCntReq()),
                nzl(r.getAckLostCntRes()),
                nzl(r.getAckLostLen()),
                nzl(r.getAckLostLenReq()),
                nzl(r.getAckLostLenRes()),

                // Window 업데이트
                nzl(r.getWinUpdateCnt()),
                nzl(r.getWinUpdateCntReq()),
                nzl(r.getWinUpdateCntRes()),
                nzl(r.getWinUpdateLen()),
                nzl(r.getWinUpdateLenReq()),
                nzl(r.getWinUpdateLenRes()),

                // 중복 ACK
                nzl(r.getDupAckCnt()),
                nzl(r.getDupAckCntReq()),
                nzl(r.getDupAckCntRes()),
                nzl(r.getDupAckLen()),
                nzl(r.getDupAckLenReq()),
                nzl(r.getDupAckLenRes()),

                // Zero Window
                nzl(r.getZeroWinCnt()),
                nzl(r.getZeroWinCntReq()),
                nzl(r.getZeroWinCntRes()),
                nzl(r.getZeroWinLen()),
                nzl(r.getZeroWinLenReq()),
                nzl(r.getZeroWinLenRes()),

                // Window Full
                nzl(r.getWindowFullCnt()),
                nzl(r.getWindowFullCntReq()),
                nzl(r.getWindowFullCntRes()),

                // Checksum 에러
                nzl(r.getChecksumErrorLen()),
                nzl(r.getChecksumErrorLenReq()),
                nzl(r.getChecksumErrorLenRes()),

                // 연결 에러
                nzl(r.getConnErrSessionCnt()),
                nzl(r.getConnErrPktCnt()),
                nzl(r.getConnErrSessionLen()),
                nzl(r.getReqConnErrSessionLen()),
                nzl(r.getResConnErrSessionLen()),

                // 트랜잭션 상태
                nzl(r.getStoppedTransactionCnt()),
                nzl(r.getStoppedTransactionCntReq()),
                nzl(r.getStoppedTransactionCntRes()),
                nzl(r.getIncompleteCnt()),
                nzl(r.getIncompleteCntReq()),
                nzl(r.getIncompleteCntRes()),
                nzl(r.getTimeoutCnt()),
                nzl(r.getTimeoutCntReq()),
                nzl(r.getTimeoutCntRes()),

                // RTO
                nzl(r.getTsPageRtoCntReq()),
                nzl(r.getTsPageRtoCntRes()),

                // RTT/ACK
                nzl(r.getPageRttConnCntReq()),
                nzl(r.getPageRttConnCntRes()),
                nzl(r.getPageRttAckCntReq()),
                nzl(r.getPageRttAckCntRes()),

                tcpErrorPercentage,
                tcpErrorPercentageReq,
                tcpErrorPercentageRes,
                pageErrorPercentage
        );
    }

    /**
     * Performance 빌드
     */
    private HttpPageMetricsDTO.Performance buildPerformance(HttpPageRowSlice r) {
        return new HttpPageMetricsDTO.Performance(
                r.getMbps(),
                r.getMbpsReq(),
                r.getMbpsRes(),
                r.getMbpsMin(),
                r.getMbpsMinReq(),
                r.getMbpsMinRes(),
                r.getMbpsMax(),
                r.getMbpsMaxReq(),
                r.getMbpsMaxRes(),
                r.getPps(),
                r.getPpsReq(),
                r.getPpsRes(),
                r.getPpsMin(),
                r.getPpsMinReq(),
                r.getPpsMinRes(),
                r.getPpsMax(),
                r.getPpsMaxReq(),
                r.getPpsMaxRes()
        );
    }

    /**
     * Traffic 빌드
     */
    private HttpPageMetricsDTO.Traffic buildTraffic(HttpPageRowSlice r) {
        return new HttpPageMetricsDTO.Traffic(
                // HTTP 길이
                nzl(r.getPageHttpLen()),
                nzl(r.getPageHttpLenReq()),
                nzl(r.getPageHttpLenRes()),
                nzl(r.getPageHttpHeaderLenReq()),
                nzl(r.getPageHttpHeaderLenRes()),
                nzl(r.getPageHttpContentLenReq()),
                nzl(r.getPageHttpContentLenRes()),
                nzl(r.getHttpContentLength()),
                nzl(r.getHttpContentLengthReq()),

                // 패킷 길이
                nzl(r.getPagePktLen()),
                nzl(r.getPagePktLenReq()),
                nzl(r.getPagePktLenRes()),

                // TCP 길이
                nzl(r.getPageTcpLen()),
                nzl(r.getPageTcpLenReq()),
                nzl(r.getPageTcpLenRes()),

                // 카운트
                nzl(r.getPageHttpCnt()),
                nzl(r.getPageHttpCntReq()),
                nzl(r.getPageHttpCntRes()),
                nzl(r.getPagePktCnt()),
                nzl(r.getPagePktCntReq()),
                nzl(r.getPagePktCntRes()),
                nzl(r.getPageTcpCnt()),
                nzl(r.getPageTcpCntReq()),
                nzl(r.getPageTcpCntRes()),
                nzl(r.getPageReqMakingCnt()),

                // Content Type
                nzl(r.getContentTypeHtmlCntReq()),
                nzl(r.getContentTypeHtmlCntRes()),
                nzl(r.getContentTypeCssCntReq()),
                nzl(r.getContentTypeCssCntRes()),
                nzl(r.getContentTypeJsCntReq()),
                nzl(r.getContentTypeJsCntRes()),
                nzl(r.getContentTypeImgCntReq()),
                nzl(r.getContentTypeImgCntRes()),
                nzl(r.getContentTypeOthCntReq()),
                nzl(r.getContentTypeOthCntRes())
        );
    }

    /**
     * Environment 빌드
     */
    private HttpPageMetricsDTO.Environment buildEnvironment(HttpPageRowSlice r) {
        return new HttpPageMetricsDTO.Environment(
                r.getCountryNameReq(),
                r.getCountryNameRes(),
                r.getContinentNameReq(),
                r.getContinentNameRes(),
                r.getDomesticPrimaryNameReq(),
                r.getDomesticPrimaryNameRes(),
                r.getDomesticSub1NameReq(),
                r.getDomesticSub1NameRes(),
                r.getDomesticSub2NameReq(),
                r.getDomesticSub2NameRes()
        );
    }

    /**
     * UserAgentInfo 빌드
     */
    private HttpPageMetricsDTO.UserAgentInfo buildUserAgentInfo(HttpPageRowSlice r) {
        return new HttpPageMetricsDTO.UserAgentInfo(
                r.getUserAgentSoftwareName(),
                r.getUserAgentOperatingSystemName(),
                r.getUserAgentOperatingPlatform(),
                r.getUserAgentSoftwareType(),
                r.getUserAgentHardwareType(),
                r.getUserAgentLayoutEngineName()
        );
    }

    /* ========== Helper Methods ========== */

    private static boolean toBoolean(Integer v) {
        return v != null && v > 0;
    }

    private long nzl(Long val) {
        return val != null ? val : 0L;
    }

    private Double safeRatio(long num, long den) {
        if (den <= 0) return null;
        return (double) num / den;
    }

    private Double normalizePct(Double raw) {
        if (raw == null) return null;
        double v = raw;
        if (v > 1.0) v = v / 100.0;
        if (v < 0.0) v = 0.0;
        if (v > 1.0) v = 1.0;
        return v;
    }
}