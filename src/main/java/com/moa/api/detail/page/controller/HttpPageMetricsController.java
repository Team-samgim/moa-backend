package com.moa.api.detail.page.controller;

import com.moa.api.detail.page.dto.HttpPageMetricsDTO;
import com.moa.api.detail.page.service.HttpPageMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * HTTP Page Metrics Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/details/http-page")
@RequiredArgsConstructor
@Validated
public class HttpPageMetricsController {

    private final HttpPageMetricsService service;

    /**
     * HTTP Page 상세 조회
     *
     * @param rowKey Row Key (형식: {timestamp}#{src_ip}#{dst_ip}#{src_port}#{dst_port}#{page_idx})
     * @return HTTP Page 메트릭 (235개 컬럼)
     */
    @GetMapping("/{rowKey}")
    public HttpPageMetricsDTO getOne(@PathVariable String rowKey) {
        log.info("GET /api/details/http-page/{}", rowKey);

        HttpPageMetricsDTO result = service.getByRowKey(rowKey);

        log.debug("Successfully fetched HTTP Page: srcIp={}, dstIp={}, httpMethod={}",
                result.srcIp(), result.dstIp(), result.httpMethod());

        return result;
    }
}