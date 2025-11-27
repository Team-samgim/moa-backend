package com.moa.api.detail.ethernet.controller;

import com.moa.api.detail.ethernet.dto.EthernetMetricsDTO;
import com.moa.api.detail.ethernet.service.EthernetMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*****************************************************************************
 CLASS NAME    : EthernetDetailController
 DESCRIPTION   : Ethernet 상세 지표 조회용 REST 컨트롤러
 - rowKey 기준으로 단건 상세 메트릭 조회
 AUTHOR        : 방대혁
 ******************************************************************************/
@RestController
@RequestMapping("/api/details/ethernet")
@RequiredArgsConstructor
public class EthernetDetailController {

    /**
     * Ethernet 상세 메트릭 조회 서비스
     */
    private final EthernetMetricsService service;

    /**
     * rowKey 기준 Ethernet 상세 메트릭 조회
     *
     * @param rowKey 세부 지표를 조회할 대상 rowKey
     * @return 200 OK + EthernetMetricsDTO / 404 Not Found
     */
    @GetMapping("/{rowKey}")
    public ResponseEntity<EthernetMetricsDTO> get(@PathVariable String rowKey) {
        return service.getMetrics(rowKey)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
