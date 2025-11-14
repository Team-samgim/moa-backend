package com.moa.api.detail.ethernet.dto;

import java.util.Map;

public record EthernetMetricsDTO(
        // === 식별 / 엔드포인트 / 프로토콜 ===
        String rowKey,
        String flowIdentifier,
        String srcMac,
        String dstMac,
        String srcIp,
        Integer srcPort,
        String dstIp,
        Integer dstPort,
        Long l2Proto,
        Long l3Proto,
        Long l4Proto,
        String l4ProtoName,      // 🆕 "TCP", "UDP" 등 읽기 쉬운 이름
        Long l7proto,
        Integer ipVersion,
        String app,              // ndpi_protocol_app
        String master,           // ndpi_protocol_master
        String sniHostname,

        // === 시간 정보 ===
        Double tsFirst,          // 🆕 플로우 시작 시간 (epoch seconds)
        Double tsLast,           // 🆕 플로우 종료 시간 (epoch seconds)
        Double tsSampleBegin,    // 🆕 샘플링 시작 시간
        Double tsSampleEnd,      // 🆕 샘플링 종료 시간
        double durSec,           // 세션 총 지속시간 (sec)

        // === 세션 상태 ===
        Integer expired,         // 🆕 만료 여부 (0/1)
        Integer expiredByTimeout,// 🆕 타임아웃으로 만료 (0/1)

        // === 기간 / 규모 / 속도 ===
        double bps,              // bits per second
        long bytes,              // 전체 바이트(len_delta 합계)
        long bytesReq,           // 요청 바이트
        long bytesRes,           // 응답 바이트
        long frames,             // 전체 프레임(pkts_delta)
        long framesReq,          // 요청 프레임
        long framesRes,          // 응답 프레임

        // === CRC 오류 ===
        long crcErrorCnt,        // 전체 CRC 오류 카운트
        long crcErrorCntReq,     // 요청 방향 CRC 오류 카운트
        long crcErrorCntRes,     // 응답 방향 CRC 오류 카운트
        double crcErrorRateFrames, // CRC 오류율 (crcErrorCnt / frames)

        long crcErrorLen,        // CRC 오류 발생 바이트 합
        long crcErrorLenReq,
        long crcErrorLenRes,

        // === 패킷 크기 통계 ===
        PacketStats packetStats,

        // === 환경 정보(Geo/센서) ===
        Environment env,

        // === 카운터 모음 (원시 카운트용) ===
        Map<String, Long> counters,

        // === 상태 뱃지 (CRC 경고 등) ===
        Map<String, String> badges,

        // === 진단 메시지 ===
        Map<String, String> diagnostics
) {

    /** 패킷 길이 관련 통계 (요청/응답) */
    public record PacketStats(
            Integer pktLenMinReq,
            Integer pktLenMinRes,
            Integer pktLenMaxReq,
            Integer pktLenMaxRes,
            Double pktLenAvgReq,
            Double pktLenAvgRes
    ) {}

    /** 환경(Geo/국내 지역/센서) */
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
            String sensorDeviceName
    ) {}
}