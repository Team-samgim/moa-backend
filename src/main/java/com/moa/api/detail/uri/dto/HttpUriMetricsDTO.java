package com.moa.api.detail.uri.dto;

import java.util.Map;

/*****************************************************************************
 * CLASS NAME    : HttpUriMetricsDTO
 * DESCRIPTION   : HTTP URI 상세 메트릭을 표현하는 계층형 DTO
 * AUTHOR        : 방대혁
 *****************************************************************************/
/**
 * HTTP URI 메트릭 DTO
 */
public record HttpUriMetricsDTO(
        // 기본 정보
        String rowKey,
        String srcIp,
        Integer srcPort,
        String srcMac,
        String dstIp,
        Integer dstPort,
        String dstMac,

        // HTTP 요청 정보
        HttpRequest request,

        // HTTP 응답 정보
        HttpResponse response,

        // 시간 분석
        TimingAnalysis timing,

        // 트래픽 통계
        TrafficStats traffic,

        // TCP 품질 분석
        TcpQuality tcpQuality,

        // 성능 메트릭
        Performance performance,

        // 환경 정보
        Environment env,

        // 상태 및 진단
        Status status,
        Map<String, String> diagnostics
) {

    /** HTTP 요청 정보 */
    public record HttpRequest(
            String method,           // GET, POST, etc.
            String version,          // HTTP/1.1
            String uri,              // /path/to/resource
            String uriSplit,         // 분할된 URI
            String host,             // www.example.com
            String referer,          // 이전 페이지
            String userAgent,        // 브라우저 정보
            String cookie,           // 쿠키

            // User Agent 파싱
            UserAgentInfo userAgentInfo,

            // Content Type 카운트
            Long htmlCnt,
            Long cssCnt,
            Long jsCnt,
            Long imgCnt,
            Long othCnt,

            // 통계
            long headerLen,          // 헤더 길이
            long contentLen,         // 컨텐츠 길이
            long totalLen,           // 전체 길이
            long pktCnt,             // 패킷 수
            long reqCnt              // 요청 횟수
    ) {}

    /** HTTP 응답 정보 */
    public record HttpResponse(
            String version,          // HTTP/1.1
            String phrase,           // OK, Not Found, etc.
            String contentType,      // text/html, application/json
            String location,         // 리다이렉트 위치

            // Content Type 카운트
            Long htmlCnt,
            Long cssCnt,
            Long jsCnt,
            Long imgCnt,
            Long othCnt,

            // 통계
            long headerLen,          // 헤더 길이
            long contentLen,         // 컨텐츠 길이
            long totalLen,           // 전체 길이
            long pktCnt,             // 패킷 수
            long resCnt              // 응답 횟수
    ) {}

    /** User Agent 파싱 정보 */
    public record UserAgentInfo(
            String softwareName,      // Chrome, Firefox, etc.
            String osName,            // Windows, macOS, etc.
            String osPlatform,        // Desktop, Mobile
            String softwareType,      // Browser, Bot
            String hardwareType,      // Computer, Mobile
            String layoutEngine       // Blink, Gecko, etc.
    ) {}

    /** 시간 분석 */
    public record TimingAnalysis(
            // 기본 타임스탬프
            double tsFirst,
            double tsFrameArrival,
            double tsFrameLandoff,

            // 요청 단계
            double reqPktFirst,      // 요청 첫 패킷
            double reqPktPush,       // 요청 PUSH
            double reqPktLast,       // 요청 마지막 패킷
            double reqDelayTransfer, // 요청 전송 지연

            // 응답 단계
            double resPktFirst,      // 응답 첫 패킷
            double resPktPush,       // 응답 PUSH
            double resPktLast,       // 응답 마지막 패킷
            double resDelayTransfer, // 응답 전송 지연

            // 처리 시간
            double resProcessFirst,  // 응답 처리 시작
            double resProcessPush,   // 응답 처리 PUSH
            double resProcessLast,   // 응답 처리 완료

            // 지연 시간 (ms)
            double responseTime,     // 응답 시간
            double transferTime,     // 전송 시간
            double totalTime         // 전체 시간
    ) {}

    /** 트래픽 통계 */
    public record TrafficStats(
            // 전체
            long pktLen,
            long pktCnt,
            long httpLen,
            long tcpLen,

            // 요청
            long pktLenReq,
            long pktCntReq,
            long httpLenReq,
            long tcpLenReq,

            // 응답
            long pktLenRes,
            long pktCntRes,
            long httpLenRes,
            long tcpLenRes,

            // 비율
            double reqResRatio,      // 요청/응답 비율
            double httpOverhead      // HTTP 오버헤드 비율
    ) {}

    /** TCP 품질 분석 */
    public record TcpQuality(
            // 재전송
            TcpError retransmission,
            TcpError fastRetransmission,

            // 에러
            TcpError outOfOrder,
            TcpError lostSeg,
            TcpError ackLost,
            TcpError checksumError,

            // 윈도우
            TcpError zeroWin,
            TcpError windowFull,
            TcpError winUpdate,
            TcpError dupAck,

            // RTT/RTO
            long ackRttCntReq,
            long ackRttCntRes,
            long ackRtoCntReq,
            long ackRtoCntRes,
            long ackRtoTotal,

            // 전체 에러
            long tcpErrorCnt,
            long tcpErrorLen,
            double tcpErrorPercentage,
            double tcpErrorLenPercentage,

            // 품질 점수
            QualityScore quality
    ) {}

    /** TCP 에러 상세 */
    public record TcpError(
            long cnt,
            long cntReq,
            long cntRes,
            long len,
            long lenReq,
            long lenRes,
            double rate          // 에러 비율
    ) {}

    /** 품질 점수 */
    public record QualityScore(
            int score,           // 0~100
            String grade,        // Excellent/Good/Fair/Poor/Critical
            String summary       // 한줄 요약
    ) {}

    /** 성능 메트릭 */
    public record Performance(
            // Mbps
            double mbps,
            double mbpsReq,
            double mbpsRes,
            double mbpsMin,
            double mbpsMax,

            // PPS
            double pps,
            double ppsReq,
            double ppsRes,
            double ppsMin,
            double ppsMax,

            // 페이지 로딩
            long pageIdx,
            long uriIdx,
            long refererCnt
    ) {}

    /** 환경 정보 */
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
            String domesticSub2Res,
            String sensorDeviceName,
            String ndpiProtocolApp,
            String ndpiProtocolMaster,
            boolean isHttps
    ) {}

    /** 상태 정보 */
    public record Status(
            boolean isStoppedTransaction,
            boolean isStoppedTransactionReq,
            boolean isStoppedTransactionRes,
            boolean isIncomplete,
            boolean isIncompleteReq,
            boolean isIncompleteRes,
            boolean isTimeout,
            boolean isTimeoutReq,
            boolean isTimeoutRes,
            String connectionStatus    // 연결 상태 요약
    ) {}
}
