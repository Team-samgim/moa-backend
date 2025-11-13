package com.moa.api.detail.ethernet.service;

import com.moa.api.detail.ethernet.dto.EthernetMetricsDTO;
import com.moa.api.detail.ethernet.repository.EthernetAgg;
import com.moa.api.detail.ethernet.repository.EthernetSampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EthernetMetricsService {

    private final EthernetSampleRepository repo;

    // 임계치 (프로젝트 기준으로 조정 가능)
    private static final double CRC_RATE_WARN = 0.001; // 0.1%
    private static final double CRC_RATE_CRIT = 0.01;  // 1%

    public Optional<EthernetMetricsDTO> getMetrics(String rowKey) {
        return repo.findAgg(rowKey)
                .map(this::toDTO);
    }

    private EthernetMetricsDTO toDTO(EthernetAgg a) {

        // === 기간 / 규모 계산 ===
        double tsFirst = nzD(a.getTsFirst());
        double tsLast  = nzD(a.getTsLast());
        double durSec  = tsLast > tsFirst ? tsLast - tsFirst : 0.0;

        long bytes     = nzL(a.getLenDelta());
        long bytesReq  = nzL(a.getLenReqDelta());
        long bytesRes  = nzL(a.getLenResDelta());

        long frames    = nzL(a.getPktsDelta());
        long framesReq = nzL(a.getPktsReqDelta());
        long framesRes = nzL(a.getPktsResDelta());

        long crcCnt    = nzL(a.getCrcErrorCntDelta());
        long crcCntReq = nzL(a.getCrcErrorCntReqDelta());
        long crcCntRes = nzL(a.getCrcErrorCntResDelta());

        long crcLen    = nzL(a.getCrcErrorLenDelta());
        long crcLenReq = nzL(a.getCrcErrorLenReqDelta());
        long crcLenRes = nzL(a.getCrcErrorLenResDelta());

        double bps     = durSec > 0.0 ? (bytes * 8.0) / durSec : 0.0;
        double crcRate = frames > 0 ? (double) crcCnt / (double) frames : 0.0;

        // === 패킷 통계 ===
        var pktStats = new EthernetMetricsDTO.PacketStats(
                a.getPktLenMinReq(),
                a.getPktLenMinRes(),
                a.getPktLenMaxReq(),
                a.getPktLenMaxRes(),
                a.getPktLenAvgReq(),
                a.getPktLenAvgRes()
        );

        // === 환경 정보 ===
        var env = new EthernetMetricsDTO.Environment(
                a.getCountryNameReq(),
                a.getCountryNameRes(),
                a.getContinentNameReq(),
                a.getContinentNameRes(),
                a.getDomesticPrimaryNameReq(),
                a.getDomesticPrimaryNameRes(),
                a.getDomesticSub1NameReq(),
                a.getDomesticSub1NameRes(),
                a.getDomesticSub2NameReq(),
                a.getDomesticSub2NameRes(),
                a.getSensorDeviceName()
        );

        // === 카운터 모음 (프론트 카운트 섹션용) ===
        Map<String, Long> counters = new LinkedHashMap<>();
        counters.put("frames", frames);
        counters.put("framesReq", framesReq);
        counters.put("framesRes", framesRes);
        counters.put("bytes", bytes);
        counters.put("bytesReq", bytesReq);
        counters.put("bytesRes", bytesRes);
        counters.put("crcErrorCnt", crcCnt);
        counters.put("crcErrorCntReq", crcCntReq);
        counters.put("crcErrorCntRes", crcCntRes);
        counters.put("crcErrorLen", crcLen);
        counters.put("crcErrorLenReq", crcLenReq);
        counters.put("crcErrorLenRes", crcLenRes);

        // === 뱃지 ===
        Map<String, String> badges = new LinkedHashMap<>();
        if (bytes > 0) {
            badges.put("traffic", "hasTraffic");
        }
        if (crcCnt > 0) {
            if (crcRate >= CRC_RATE_CRIT) {
                badges.put("crc", "crit");
            } else if (crcRate >= CRC_RATE_WARN) {
                badges.put("crc", "warn");
            } else {
                badges.put("crc", "info");
            }
        }

        // === 진단 메시지 ===
        Map<String, String> diag = buildDiagnostics(durSec, frames, crcRate, bytes, bps);

        return new EthernetMetricsDTO(
                a.getRowKey(),
                a.getFlowIdentifier(),
                a.getSrcMac(),
                a.getDstMac(),
                a.getSrcIp(),
                a.getSrcPort(),
                a.getDstIp(),
                a.getDstPort(),
                a.getL2Proto(),
                a.getL3Proto(),
                a.getL4Proto(),
                a.getL7proto(),
                a.getIpVersion(),
                a.getNdpiProtocolApp(),
                a.getNdpiProtocolMaster(),
                a.getSniHostname(),
                round(durSec, 3),
                round(bps, 2),
                bytes,
                bytesReq,
                bytesRes,
                frames,
                framesReq,
                framesRes,
                crcCnt,
                crcCntReq,
                crcCntRes,
                round(crcRate, 5),
                crcLen,
                crcLenReq,
                crcLenRes,
                pktStats,
                env,
                counters,
                badges,
                diag
        );
    }

    private Map<String, String> buildDiagnostics(double durSec,
                                                 long frames,
                                                 double crcRate,
                                                 long bytes,
                                                 double bps) {

        Map<String, String> m = new LinkedHashMap<>();

        if (durSec > 0 && bps > 0) {
            m.put("throughput", "평균 처리량 약 " + round(bps / 1_000_000.0, 2) + " Mbps");
        }

        if (crcRate > CRC_RATE_CRIT) {
            m.put("crcCrit", "CRC 오류율이 매우 높습니다: " + round(crcRate * 100, 3) + "%");
        } else if (crcRate > CRC_RATE_WARN) {
            m.put("crcWarn", "CRC 오류율이 다소 높습니다: " + round(crcRate * 100, 3) + "%");
        }

        if (frames > 0 && bytes > 0) {
            double avgFrameSize = (double) bytes / (double) frames;
            m.put("avgFrame",
                    "평균 프레임 크기: " + round(avgFrameSize, 1) + " bytes/frame");
        }

        if (durSec <= 0 && bytes > 0) {
            m.put("durationAnomaly", "트래픽은 있으나 기간(durSec)이 0으로 계산되었습니다.");
        }

        if (m.isEmpty()) {
            m.put("normal", "특이사항 없음");
        }

        return m;
    }

    // === 유틸 ===
    private long nzL(Long v) {
        return v == null ? 0L : v;
    }

    private double nzD(Double v) {
        return v == null ? 0.0 : v;
    }

    private double round(double v, int scale) {
        double p = Math.pow(10, scale);
        return Math.round(v * p) / p;
    }
}