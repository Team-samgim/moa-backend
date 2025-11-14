package com.moa.api.detail.ethernet.service;

import com.moa.api.detail.ethernet.dto.EthernetMetricsDTO;
import com.moa.api.detail.ethernet.repository.EthernetRowSlice;
import com.moa.api.detail.ethernet.repository.EthernetSampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.of("Asia/Seoul"));

    public Optional<EthernetMetricsDTO> getMetrics(String rowKey) {
        return repo.findAgg(rowKey)
                .map(this::toDTO);
    }

    private EthernetMetricsDTO toDTO(EthernetRowSlice a) {

        // === 기간 / 규모 계산 ===
        double tsFirst = nzD(a.getTsFirst());
        double tsLast  = nzD(a.getTsLast());
        double durSec  = tsLast > tsFirst ? tsLast - tsFirst : 0.0;

        Double tsSampleBegin = a.getTsSampleBegin();
        Double tsSampleEnd = a.getTsSampleEnd();

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

        // === 프로토콜 이름 변환 ===
        String l4ProtoName = getL4ProtoName(a.getL4Proto());

        // === 세션 상태 ===
        Integer expired = a.getExpired();
        Integer expiredByTimeout = a.getExpiredByTimeout();

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
        Map<String, String> badges = buildBadges(bytes, crcCnt, crcRate, expiredByTimeout);

        // === 진단 메시지 ===
        Map<String, String> diag = buildDiagnostics(
                durSec, frames, crcRate, bytes, bps,
                bytesReq, bytesRes, expiredByTimeout, tsFirst, tsLast
        );

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
                l4ProtoName,        // 🆕
                a.getL7proto(),
                a.getIpVersion(),
                a.getNdpiProtocolApp(),
                a.getNdpiProtocolMaster(),
                a.getSniHostname(),
                tsFirst,            // 🆕
                tsLast,             // 🆕
                tsSampleBegin,      // 🆕
                tsSampleEnd,        // 🆕
                round(durSec, 3),
                expired,            // 🆕
                expiredByTimeout,   // 🆕
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

    private Map<String, String> buildBadges(long bytes,
                                            long crcCnt,
                                            double crcRate,
                                            Integer expiredByTimeout) {
        Map<String, String> badges = new LinkedHashMap<>();

        if (bytes > 0) {
            badges.put("traffic", "hasTraffic");
        }

        if (crcCnt > 0) {
            if (crcRate >= CRC_RATE_CRIT) {
                badges.put("crc", "critical");
            } else if (crcRate >= CRC_RATE_WARN) {
                badges.put("crc", "warning");
            } else {
                badges.put("crc", "info");
            }
        }

        if (expiredByTimeout != null && expiredByTimeout == 1) {
            badges.put("timeout", "warning");
        }

        return badges;
    }

    private Map<String, String> buildDiagnostics(double durSec,
                                                 long frames,
                                                 double crcRate,
                                                 long bytes,
                                                 double bps,
                                                 long bytesReq,
                                                 long bytesRes,
                                                 Integer expiredByTimeout,
                                                 double tsFirst,
                                                 double tsLast) {

        Map<String, String> m = new LinkedHashMap<>();

        // === 시간 정보 ===
        if (tsFirst > 0) {
            String startTime = formatTimestamp(tsFirst);
            m.put("startTime", "시작 시간: " + startTime);
        }

        // === 처리량 정보 ===
        if (durSec > 0 && bps > 0) {
            String throughput = formatBps(bps);
            m.put("throughput", "평균 처리량: " + throughput);
        }

        // === CRC 에러 ===
        if (crcRate > CRC_RATE_CRIT) {
            m.put("crcCrit", "⚠️ CRC 오류율이 매우 높습니다: " + round(crcRate * 100, 3) + "%");
        } else if (crcRate > CRC_RATE_WARN) {
            m.put("crcWarn", "⚠️ CRC 오류율이 다소 높습니다: " + round(crcRate * 100, 3) + "%");
        }

        // === 트래픽 방향성 분석 ===
        if (bytesReq > 0 && bytesRes > 0) {
            double ratio = (double) bytesRes / bytesReq;
            String direction = getTrafficDirection(ratio);
            m.put("trafficDirection",
                    String.format("트래픽 비율 (응답/요청): %.2f %s", ratio, direction));
        }

        // === 평균 프레임 크기 ===
        if (frames > 0 && bytes > 0) {
            double avgFrameSize = (double) bytes / (double) frames;
            m.put("avgFrame", "평균 프레임 크기: " + round(avgFrameSize, 1) + " bytes");
        }

        // === 세션 상태 ===
        if (expiredByTimeout != null && expiredByTimeout == 1) {
            m.put("timeout", "⚠️ 세션이 타임아웃으로 종료되었습니다");
        }

        // === 이상 징후 ===
        if (durSec <= 0 && bytes > 0) {
            m.put("durationAnomaly", "⚠️ 트래픽은 있으나 지속 시간이 0초입니다");
        }

        if (durSec > 3600) {
            m.put("longSession", "ℹ️ 장시간 세션: " + formatDuration(durSec));
        }

        if (m.isEmpty()) {
            m.put("normal", "특이사항 없음");
        }

        return m;
    }

    // === L4 프로토콜 이름 변환 ===
    private String getL4ProtoName(Long l4Proto) {
        if (l4Proto == null) return "Unknown";

        // ClickHouse에서 비트 조합값으로 저장되는 경우가 많음
        // 실제 프로토콜 번호는 하위 8비트에 있을 수 있음
        long protoNum = l4Proto & 0xFF;

        return switch ((int) protoNum) {
            case 1 -> "ICMP";
            case 2 -> "IGMP";
            case 6 -> "TCP";
            case 17 -> "UDP";
            case 41 -> "IPv6";
            case 47 -> "GRE";
            case 50 -> "ESP";
            case 51 -> "AH";
            case 58 -> "ICMPv6";
            case 89 -> "OSPF";
            case 132 -> "SCTP";
            default -> "L4(" + protoNum + ")";
        };
    }

    // === 포맷팅 유틸 ===
    private String formatTimestamp(double epoch) {
        try {
            return TIME_FORMATTER.format(Instant.ofEpochSecond((long) epoch));
        } catch (Exception e) {
            return String.format("%.0f", epoch);
        }
    }

    private String formatBps(double bps) {
        if (bps >= 1_000_000_000) {
            return round(bps / 1_000_000_000.0, 2) + " Gbps";
        } else if (bps >= 1_000_000) {
            return round(bps / 1_000_000.0, 2) + " Mbps";
        } else if (bps >= 1_000) {
            return round(bps / 1_000.0, 2) + " Kbps";
        } else {
            return round(bps, 2) + " bps";
        }
    }

    private String formatDuration(double seconds) {
        if (seconds >= 3600) {
            long hours = (long) (seconds / 3600);
            long mins = (long) ((seconds % 3600) / 60);
            return String.format("%d시간 %d분", hours, mins);
        } else if (seconds >= 60) {
            long mins = (long) (seconds / 60);
            long secs = (long) (seconds % 60);
            return String.format("%d분 %d초", mins, secs);
        } else {
            return round(seconds, 2) + "초";
        }
    }

    private String getTrafficDirection(double ratio) {
        if (ratio > 10) return "(대량 다운로드)";
        if (ratio > 3) return "(다운로드 중심)";
        if (ratio > 0.33) return "(양방향)";
        if (ratio > 0.1) return "(업로드 중심)";
        return "(대량 업로드)";
    }

    // === 기본 유틸 ===
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