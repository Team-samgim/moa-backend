package com.moa.api.grid.controller;

import com.moa.api.grid.dto.FilterResponseDTO;
import com.moa.api.grid.dto.GridRequestDTO;
import com.moa.api.grid.dto.SearchResponseDTO;
import com.moa.api.grid.service.GridService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GridController {

    private final GridService gridService;

    /** 메인 데이터 조회 */
    @GetMapping("/randering")
    public SearchResponseDTO getGridData(GridRequestDTO request) {
        return gridService.getGridData(request);
    }

    /** 필터용 DISTINCT 값 조회 */
    @GetMapping("/filtering")
    public FilterResponseDTO getDistinctValues(
            @RequestParam(defaultValue = "ethernet") String layer,
            @RequestParam String field,
            @RequestParam(required = false) String filterModel
    ) {
        return gridService.getDistinctValues(layer, field, filterModel);
    }
}