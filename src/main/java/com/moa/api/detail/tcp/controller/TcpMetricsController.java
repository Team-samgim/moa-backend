package com.moa.api.detail.tcp.controller;

import com.moa.api.detail.tcp.dto.TcpMetricsDTO;
import com.moa.api.detail.tcp.service.TcpMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/details/tcp")
@RequiredArgsConstructor
public class TcpMetricsController {

    private final TcpMetricsService service;

    @GetMapping("/{rowKey}")
    public TcpMetricsDTO one(@PathVariable String rowKey) {
        return service.getByRowKey(rowKey);
    }
}