package com.moa.api.detail.uri.controller;

import com.moa.api.detail.uri.dto.HttpUriMetricsDTO;
import com.moa.api.detail.uri.service.HttpUriMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * HTTP URI 상세 정보 API
 *
 * GET /api/http/uri/{rowKey}
 */
@RestController
@RequestMapping("/api/details/http-uri")
@RequiredArgsConstructor
public class HttpUriMetricsController {

    private final HttpUriMetricsService service;

    @GetMapping("/{rowKey}")
    public HttpUriMetricsDTO getOne(@PathVariable String rowKey) {
        return service.getByRowKey(rowKey);
    }
}