/**
 * 작성자: 정소영
 * 설명: 대시보드 조회 요청을 받아 시간 범위와 필터 조건에 따른 위젯 데이터를 반환하는 컨트롤러
 */
package com.moa.api.dashboard.controller;

import com.moa.api.dashboard.dto.request.DashboardRequestDTO;
import com.moa.api.dashboard.dto.response.DashboardResponseDTO;
import com.moa.api.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Dashboard", description = "EUM 대시보드 API")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * EUM 대시보드 데이터 조회 (POST)
     * - 시간 범위 + 필터 조건으로 위젯 데이터 조회
     * - 사용 가능한 필터 옵션도 함께 반환
     */
    @Operation(
            summary = "대시보드 데이터 조회",
            description = "시간 범위와 필터 조건에 따라 대시보드 위젯 데이터를 조회합니다. " +
                    "필터가 적용되지 않은 경우 모든 데이터를 반환합니다."
    )
    @PostMapping
    public ResponseEntity<DashboardResponseDTO> getDashboardData(
            @RequestBody DashboardRequestDTO request
    ) {
        DashboardResponseDTO response = dashboardService.getDashboardData(request);
        return ResponseEntity.ok(response);
    }
}