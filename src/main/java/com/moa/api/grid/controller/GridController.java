package com.moa.api.grid.controller;

import com.moa.api.grid.dto.*;
import com.moa.api.grid.service.GridService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GridController {

    private final GridService gridService;

    /** ✅ 메인 데이터 조회 */
    @GetMapping("/randering")
    public SearchResponseDTO getGridData(GridRequestDTO request) {
        return gridService.getGridData(request);
    }

    /** ✅ 필터용 DISTINCT 값 조회 */
    @GetMapping("/filtering")
    public FilterResponseDTO getDistinctValues(
            @RequestParam(defaultValue = "ethernet") String layer,
            @RequestParam String field,
            @RequestParam(required = false) String filterModel,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = "0") int offset,
            @RequestParam(required = false, defaultValue = "100") int limit,
            @RequestParam(required = false, defaultValue = "false") boolean includeSelf
    ) {
        return gridService.getDistinctValues(layer, field, filterModel, search, offset, limit, includeSelf);
    }

    /** ✅ 집계(푸터 요약) */
    @PostMapping("/aggregate")
    public AggregateResponseDTO aggregate(@RequestBody AggregateRequestDTO request) {
        return gridService.aggregate(request);
    }
}
