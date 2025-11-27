package com.moa.api.detail.tcp.controller;

import com.moa.api.detail.tcp.dto.TcpMetricsDTO;
import com.moa.api.detail.tcp.service.TcpMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*****************************************************************************
 CLASS NAME    : TcpMetricsController
 DESCRIPTION   : TCP 상세 메트릭 조회를 처리하는 REST 컨트롤러
 AUTHOR        : 방대혁
 ******************************************************************************/
/**
 * TCP Metrics Controller
 */
@RestController
@RequestMapping("/api/details/tcp")
@RequiredArgsConstructor
public class TcpMetricsController {

    private final TcpMetricsService service;

    /**
     * TCP 세션 메트릭 단건 조회
     *
     * @param rowKey TCP 세션 식별용 Row Key
     * @return TCP 메트릭 DTO
     */
    @GetMapping("/{rowKey}")
    public TcpMetricsDTO one(@PathVariable String rowKey) {
        return service.getByRowKey(rowKey);
    }
}
