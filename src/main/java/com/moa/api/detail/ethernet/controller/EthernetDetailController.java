package com.moa.api.detail.ethernet.controller;

import com.moa.api.detail.ethernet.dto.EthernetMetricsDTO;
import com.moa.api.detail.ethernet.service.EthernetMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/details/ethernet")
@RequiredArgsConstructor
public class EthernetDetailController {

    private final EthernetMetricsService service;

    @GetMapping("/{rowKey}")
    public ResponseEntity<EthernetMetricsDTO> get(@PathVariable String rowKey) {
        return service.getMetrics(rowKey)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}