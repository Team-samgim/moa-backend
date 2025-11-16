package com.moa.api.dashboard.dto.response;

import java.util.List;

/**
 * 사용 가능한 필터 옵션
 * 실제 DB에 존재하는 값들만 반환
 */
public record DashboardFiltersDTO(
        List<String> countries,      // ["Korea", "USA", "Japan", ...]
        List<String> browsers,       // ["Chrome", "Safari", "Firefox", ...]
        List<String> devices,        // ["Desktop", "Mobile", "Tablet", ...]
        List<String> httpHosts,      // ["example.com", "api.example.com", ...]
        List<String> httpMethods     // ["GET", "POST", "PUT", "DELETE"]
) {}