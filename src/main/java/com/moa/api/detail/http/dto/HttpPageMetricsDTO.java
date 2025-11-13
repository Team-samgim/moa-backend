package com.moa.api.detail.http.dto;

import java.util.Map;

public record HttpPageMetricsDTO(
        // 식별/엔드포인트
        String rowKey,
        String srcIp, Integer srcPort,
        String dstIp, Integer dstPort,
        String app, String master,
        boolean https,
        String host, String uri,
        String method, Integer resCode, String resPhrase,

        // 규모/시간/속도
        double durSec, double bps,
        long bytes, long bytesReq, long bytesRes,
        long pkts, long pktsReq, long pktsRes,

        // 품질 비율 (패킷 기준)
        double retransRatePkts, double oooRatePkts, double lossRatePkts, double csumRatePkts,

        // 카운트/배지
        Map<String,Integer> qualityCounts,
        Map<String,String> badges,

        // 탭용 세부 섹션
        Timings timings,
        Transport transport,
        Http http,
        Environment env,
        Map<String,String> diagnostics
) {

    /** 타이밍 섹션 */
    public record Timings(
            Double connectAvg, Double connectMin, Double connectMax,
            Double reqMakingAvg, Double reqMakingSum,
            Double resInit, Double resInitGap,
            Double resApp, Double resAppGap,
            Double transferReq, Double transferReqGap,
            Double transferRes, Double transferResGap
    ) {}

    /** 전송/품질 섹션 */
    public record Transport(
            Long retransCnt, Long oooCnt, Long lossCnt, Long csumCnt,
            Long dupAckCnt, Long winUpdateCnt, Long zeroWinCnt, Long windowFullCnt, Long ackLostCnt,
            Double dupAckRate, Double winUpdateRate, Double zeroWinRate, Double windowFullRate, Double ackLostRate
    ) {}

    /** HTTP/컨텐츠 섹션 */
    public record Http(
            // 상태 코드 분포
            Integer code1xx, Integer code2xx, Integer code3xx, Integer code304, Integer code4xx, Integer code5xx,
            // 메서드 분포
            Integer methodGetCnt, Integer methodPostCnt, Integer methodPutCnt, Integer methodDeleteCnt,
            Integer methodHeadCnt, Integer methodOptionsCnt, Integer methodPatchCnt, Integer methodTraceCnt,
            Integer methodConnectCnt, Integer methodOthCnt,
            // 버전
            String httpVersion, String httpVersionReq, String httpVersionRes,
            // 컨텐츠 타입 카운트
            Integer contentHtmlReq, Integer contentHtmlRes,
            Integer contentCssReq,  Integer contentCssRes,
            Integer contentJsReq,   Integer contentJsRes,
            Integer contentImgReq,  Integer contentImgRes,
            Integer contentOthReq,  Integer contentOthRes,
            // 크기/길이
            Long headerBytesReq, Long headerBytesRes,
            Long bodyBytesReq,   Long bodyBytesRes,
            Long pktLenReq,      Long pktLenRes,
            Long contentLengthReq, Long contentLengthRes,
            // 헤더 메타
            String referer, String cookie, String userAgent,
            String contentType
    ) {}

    /** 환경(Geo/UA) 섹션 */
    public record Environment(
            String countryReq, String countryRes,
            String continentReq, String continentRes,
            String os, String browser, String deviceType, String layoutEngine
    ) {}
}
