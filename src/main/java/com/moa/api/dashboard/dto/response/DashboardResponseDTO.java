package com.moa.api.dashboard.dto.response;

import com.moa.api.dashboard.dto.response.*;

import java.sql.Timestamp;
import java.util.List;

// ============================================
// 전체 대시보드 응답
// ============================================

public record DashboardResponseDTO(
        List<TrafficTrendResponseDTO> trafficTrend,
        TcpErrorRateResponseDTO tcpErrorRate,
        List<TrafficByCountryResponseDTO> trafficByCountry,
        HttpStatusCodeResponseDTO httpStatusCodes,
        List<TopDomainResponseDTO> topDomains,
        ResponseTimeResponseDTO responseTime,
        Timestamp queryStartTime,
        Timestamp queryEndTime
) {}