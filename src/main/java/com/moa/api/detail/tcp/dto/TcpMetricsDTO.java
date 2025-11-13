package com.moa.api.detail.tcp.dto;

import java.util.Map;

public record TcpMetricsDTO(
        // 식별/엔드포인트
        String rowKey,
        String srcIp, Integer srcPort,
        String dstIp, Integer dstPort,
        String app, String master, String sni,

        // 규모/시간/속도
        double durSec, double bps,
        long len, long lenReq, long lenRes,
        long pkts, long pktsReq, long pktsRes,

        // 품질 비율
        double retransRateBytes, double oooRatePkts, double lossRatePkts, double csumRatePkts,

        // 패턴
        double reqResRatio, boolean ackOnly, String handshake, String termination,

        // 플래그/품질 카운트/배지
        Map<String,Long> flags,
        Map<String,Long> qualityCounts,
        Map<String,String> badges
) {}