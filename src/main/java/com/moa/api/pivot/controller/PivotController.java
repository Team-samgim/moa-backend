package com.moa.api.pivot.controller;

import com.moa.api.pivot.dto.PivotFieldsResponseDTO;
import com.moa.api.pivot.dto.PivotQueryRequestDTO;
import com.moa.api.pivot.dto.PivotQueryResponseDTO;
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
        return pivotService.getFieldsForLayer(layer);
    }

    // 피벗 결과 조회
    @PostMapping("/query")
    public PivotQueryResponseDTO queryPivot(@RequestBody PivotQueryRequestDTO request) {
        return pivotService.runPivotQuery(request);
    }

    // 그리드 > 피벗 전환 시 초기 설정
    // @PostMapping("/init")
    // public PivotQueryRequest initPivotFromGrid(@RequestBody GridStateRequest gridRequest) {
    //     return pivotService.buildInitialConfig(gridRequest);
    // }
}
