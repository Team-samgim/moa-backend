package com.moa.api.detail.uri.controller;

/*****************************************************************************
 CLASS NAME    : HttpUriMetricsController
 DESCRIPTION   : HTTP URI 상세 메트릭 조회용 REST Controller
 AUTHOR        : 방대혁
 ******************************************************************************/
/**
 * HTTP URI 상세 메트릭 조회 Controller
 *
 * - Endpoint: GET /api/details/http-uri/{rowKey}
 */

import com.moa.api.detail.uri.dto.HttpUriMetricsDTO;
import com.moa.api.detail.uri.service.HttpUriMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/details/http-uri")
@RequiredArgsConstructor
@Validated
public class HttpUriMetricsController {

    private final HttpUriMetricsService service;

    /**
     * HTTP URI 상세 조회
     *
     * @param rowKey Row Key
     * @return HTTP URI 메트릭
     */
    @GetMapping("/{rowKey}")
    public HttpUriMetricsDTO getOne(@PathVariable String rowKey) {
        log.info("GET /api/details/http-uri/{}", rowKey);

        HttpUriMetricsDTO result = service.getByRowKey(rowKey);

        log.debug("Fetched HTTP URI metrics: rowKey={}, srcIp={}, dstIp={}",
                result.rowKey(), result.srcIp(), result.dstIp());

        return result;
    }
}
