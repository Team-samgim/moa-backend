package com.moa.api.pivot.controller;

import com.moa.api.pivot.dto.*;
import com.moa.api.pivot.service.PivotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pivot")
@RequiredArgsConstructor
public class PivotController {

    private final PivotService pivotService;

    // 레이어 별 필드 정보 리스트 조회
    @GetMapping("/fields")
    public PivotFieldsResponseDTO getFields(@RequestParam("layer") String layer) {
        return pivotService.getFields(layer);
    }

    /** 피벗 쿼리 실행 */
    @PostMapping("/query")
    public PivotQueryResponseDTO query(@RequestBody PivotQueryRequestDTO req) {
        return pivotService.runPivot(req);
    }

    /** 필터 팝업용 값 목록 (무한 스크롤 + 검색) */
    @PostMapping("/values")
    public DistinctValuesPageDTO values(@RequestBody DistinctValuesRequestDTO req) {
        return pivotService.getDistinctValuesPage(req);
    }

    /** 특정 row group(field)에 대한 subRows + breakdown 조회 */
    @PostMapping("/row-group/items")
    public RowGroupItemsResponseDTO getRowGroupItems(
            @RequestBody RowGroupItemsRequestDTO req
    ) {
        return pivotService.getRowGroupItems(req);
    }

    @PostMapping("/chart")
    public PivotChartResponseDTO chart(@RequestBody PivotChartRequestDTO req) {
        return pivotService.getChart(req);
    }
}
