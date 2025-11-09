package com.moa.api.dashboard.dto;

import java.sql.Timestamp;
import java.util.List;

// ============================================
// 전체 대시보드 응답
// ============================================

public record DashboardResponseDTO(
        List<TrafficTrendDTO> trafficTrend,
        TcpErrorRateDTO tcpErrorRate,
        List<TrafficByCountryDTO> trafficByCountry,
        HttpStatusCodeDTO httpStatusCodes,
        List<TopDomainDTO> topDomains,
        ResponseTimeDTO responseTime,
        Timestamp queryStartTime,
        Timestamp queryEndTime
) {}