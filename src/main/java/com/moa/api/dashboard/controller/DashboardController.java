package com.moa.api.dashboard.controller;

import com.moa.api.dashboard.dto.request.DashboardRequestDTO;
import com.moa.api.dashboard.dto.response.DashboardResponseDTO;
import com.moa.api.dashboard.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * 대시보드 전체 데이터 조회 (POST, epoch 초 기반)
     */
    @PostMapping(consumes = "application/json")
    public ResponseEntity<DashboardResponseDTO> getDashboardPost(
            @RequestBody(required = false) DashboardRequestDTO body) {

        Long fromEpoch = null;
        Long toEpoch = null;

        if (body != null && body.getRange() != null) {
            fromEpoch = body.getRange().getFromEpoch();
            toEpoch = body.getRange().getToEpoch();
        }

        long nowEpoch = Instant.now().getEpochSecond();
        if (toEpoch == null) {
            toEpoch = nowEpoch;
        }
        if (fromEpoch == null) {
            if (body != null && body.getTimePreset() != null) {
                switch (body.getTimePreset()) {
                    case "1H" -> fromEpoch = toEpoch - 3600L;
                    case "24H" -> fromEpoch = toEpoch - 24L * 3600L;
                    case "7D" -> fromEpoch = toEpoch - 7L * 24L * 3600L;
                    default -> fromEpoch = toEpoch - 3600L;
                }
            } else {
                fromEpoch = toEpoch - 3600L;
                fromEpoch = toEpoch - 3600L;
            }
        }

        if (fromEpoch > toEpoch) {
            return ResponseEntity.badRequest().body(null);
        }

        DashboardResponseDTO response = dashboardService.getDashboardData(fromEpoch, toEpoch);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Dashboard API is running");
    }
}