package com.moa.api.detail.http.controller;

import com.moa.api.detail.http.dto.HttpPageMetricsDTO;
import com.moa.api.detail.http.service.HttpPageMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/details/http-page")
@RequiredArgsConstructor
public class HttpMetricsController {
    private final HttpPageMetricsService service;

    @GetMapping("/{rowKey}")
    public ResponseEntity<HttpPageMetricsDTO> get(@PathVariable String rowKey) {
        return service.getMetrics(rowKey)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}