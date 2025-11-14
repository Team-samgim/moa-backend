package com.moa.api.detail.page.controller;

import com.moa.api.detail.page.dto.HttpPageMetricsDTO;
import com.moa.api.detail.page.service.HttpPageMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/details/http-page")
@RequiredArgsConstructor
public class HttpPageMetricsController {
    private final HttpPageMetricsService service;

    @GetMapping("/{rowKey}")
    public HttpPageMetricsDTO getOne(@PathVariable String rowKey) {
        return service.getByRowKey(rowKey);
    }
}