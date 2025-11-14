package com.moa.api.detail.page.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * HTTP Page Metrics DTO
 * 235개 컬럼을 계층적 구조로 구성
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HttpPageMetricsDTO(
        // 기본 정보
        String rowKey,
        String srcIp,
        String dstIp,
        Integer srcPort,
        Integer dstPort,
        String srcMac,
        String dstMac,
        Long pageIdx,
        String tsServer,

        // HTTP 기본 정보
        String httpMethod,
        String httpVersion,
        String httpHost,
        String httpUri,
        String httpResCode,
        String httpResPhrase,
        String httpContentType,
        String httpUserAgent,
        String httpCookie,
        String httpLocation,
        String httpReferer,
        Boolean isHttps,

        // 페이지 통계
        Long pageSessionCnt,
        Long pageTcpConnectCnt,
        Long uriCnt,
        Long httpUriCnt,
        Long httpsUriCnt,
        Long pageErrorCnt,

        // 프로토콜/센서 정보 (최상위로 이동) ⭐
        String ndpiProtocolApp,
        String ndpiProtocolMaster,
        String sensorDeviceName,

        // 중첩 구조
        Timing timing,
        Methods methods,
        StatusCodes statusCodes,
        TcpQuality tcpQuality,
        Performance performance,
        Traffic traffic,
        Environment env,
        UserAgentInfo userAgentInfo
) {

    /**
     * 시간 메트릭
     */
    public record Timing(
            Double tsFirst,
            Double tsPageBegin,
            Double tsPageEnd,
            Double tsPageReqSyn,
            Double tsPage,
            Double tsPageGap,
            Double tsPageResInit,
            Double tsPageResInitGap,
            Double tsPageResApp,
            Double tsPageResAppGap,
            Double tsPageRes,
            Double tsPageResGap,
            Double tsPageTransferReq,
            Double tsPageTransferReqGap,
            Double tsPageTransferRes,
            Double tsPageTransferResGap,
            Double tsPageReqMakingSum,
            Double tsPageReqMakingAvg,
            Double tsPageTcpConnectSum,
            Double tsPageTcpConnectMin,
            Double tsPageTcpConnectMax,
            Double tsPageTcpConnectAvg
    ) {}

    /**
     * HTTP 메소드 통계
     */
    public record Methods(
            long getCnt,
            long postCnt,
            long putCnt,
            long deleteCnt,
            long headCnt,
            long optionsCnt,
            long patchCnt,
            long traceCnt,
            long connectCnt,
            long othCnt,
            long getCntError,
            long postCntError,
            long putCntError,
            long deleteCntError,
            long headCntError,
            long optionsCntError,
            long patchCntError,
            long traceCntError,
            long connectCntError,
            long othCntError,
            boolean hasErrors
    ) {}

    /**
     * HTTP 응답 코드 분포
     */
    public record StatusCodes(
            long code1xxCnt,
            long code2xxCnt,
            long code304Cnt,     // ← 304가 3xx보다 먼저! (CSV 순서와 일치)
            long code3xxCnt,
            long code401Cnt,
            long code403Cnt,
            long code404Cnt,
            long code4xxCnt,
            long code5xxCnt,
            long codeOthCnt
    ) {}

    /**
     * TCP 품질
     */
    public record TcpQuality(
            long tcpErrorCnt,
            long tcpErrorCntReq,
            long tcpErrorCntRes,
            long tcpErrorLen,
            long tcpErrorLenReq,
            long tcpErrorLenRes,

            // 재전송
            long retransmissionCnt,
            long retransmissionCntReq,
            long retransmissionCntRes,
            long retransmissionLen,
            long retransmissionLenReq,
            long retransmissionLenRes,

            // 순서 오류
            long outOfOrderCnt,
            long outOfOrderCntReq,
            long outOfOrderCntRes,
            long outOfOrderLen,
            long outOfOrderLenReq,
            long outOfOrderLenRes,

            // 패킷 손실
            long lostSegCnt,
            long lostSegCntReq,
            long lostSegCntRes,
            long lostSegLen,
            long lostSegLenReq,
            long lostSegLenRes,

            // ACK 손실
            long ackLostCnt,
            long ackLostCntReq,
            long ackLostCntRes,
            long ackLostLen,
            long ackLostLenReq,
            long ackLostLenRes,

            // Window 업데이트
            long winUpdateCnt,
            long winUpdateCntReq,
            long winUpdateCntRes,
            long winUpdateLen,
            long winUpdateLenReq,
            long winUpdateLenRes,

            // 중복 ACK
            long dupAckCnt,
            long dupAckCntReq,
            long dupAckCntRes,
            long dupAckLen,
            long dupAckLenReq,
            long dupAckLenRes,

            // Zero Window
            long zeroWinCnt,
            long zeroWinCntReq,
            long zeroWinCntRes,
            long zeroWinLen,
            long zeroWinLenReq,
            long zeroWinLenRes,

            // Window Full
            long windowFullCnt,
            long windowFullCntReq,
            long windowFullCntRes,

            // Checksum 에러
            long checksumErrorLen,
            long checksumErrorLenReq,
            long checksumErrorLenRes,

            // 연결 에러
            long connErrSessionCnt,
            long connErrPktCnt,
            long connErrSessionLen,
            long reqConnErrSessionLen,
            long resConnErrSessionLen,

            // 트랜잭션 상태
            long stoppedTransactionCnt,
            long stoppedTransactionCntReq,
            long stoppedTransactionCntRes,
            long incompleteCnt,
            long incompleteCntReq,
            long incompleteCntRes,
            long timeoutCnt,
            long timeoutCntReq,
            long timeoutCntRes,

            // RTO
            long tsPageRtoCntReq,
            long tsPageRtoCntRes,

            // RTT/ACK
            long pageRttConnCntReq,
            long pageRttConnCntRes,
            long pageRttAckCntReq,
            long pageRttAckCntRes,

            // 에러 비율
            Double tcpErrorPercentage,
            Double tcpErrorPercentageReq,
            Double tcpErrorPercentageRes,
            Double pageErrorPercentage
    ) {}

    /**
     * 성능 메트릭
     */
    public record Performance(
            Double mbps,
            Double mbpsReq,
            Double mbpsRes,
            Double mbpsMin,
            Double mbpsMinReq,
            Double mbpsMinRes,
            Double mbpsMax,
            Double mbpsMaxReq,
            Double mbpsMaxRes,
            Double pps,
            Double ppsReq,
            Double ppsRes,
            Double ppsMin,
            Double ppsMinReq,
            Double ppsMinRes,
            Double ppsMax,
            Double ppsMaxReq,
            Double ppsMaxRes
    ) {}

    /**
     * 트래픽 통계
     */
    public record Traffic(
            // HTTP 길이
            long pageHttpLen,
            long pageHttpLenReq,
            long pageHttpLenRes,
            long pageHttpHeaderLenReq,
            long pageHttpHeaderLenRes,
            long pageHttpContentLenReq,
            long pageHttpContentLenRes,
            long httpContentLength,
            long httpContentLengthReq,

            // 패킷 길이
            long pagePktLen,
            long pagePktLenReq,
            long pagePktLenRes,

            // TCP 길이
            long pageTcpLen,
            long pageTcpLenReq,
            long pageTcpLenRes,

            // 카운트
            long pageHttpCnt,
            long pageHttpCntReq,
            long pageHttpCntRes,
            long pagePktCnt,
            long pagePktCntReq,
            long pagePktCntRes,
            long pageTcpCnt,
            long pageTcpCntReq,
            long pageTcpCntRes,
            long pageReqMakingCnt,

            // Content Type
            long contentTypeHtmlCntReq,
            long contentTypeHtmlCntRes,
            long contentTypeCssCntReq,
            long contentTypeCssCntRes,
            long contentTypeJsCntReq,
            long contentTypeJsCntRes,
            long contentTypeImgCntReq,
            long contentTypeImgCntRes,
            long contentTypeOthCntReq,
            long contentTypeOthCntRes
    ) {}

    /**
     * 환경 정보 (지리 정보만)
     */
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

    /**
     * User Agent 정보
     */
    public record UserAgentInfo(
            String softwareName,
            String osName,
            String osPlatform,
            String softwareType,
            String hardwareType,
            String layoutEngineName
    ) {}
}