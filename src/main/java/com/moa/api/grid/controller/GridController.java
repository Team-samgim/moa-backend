package com.moa.api.grid.controller;

import com.moa.api.grid.dto.*;
import com.moa.api.grid.service.GridService;
import com.moa.api.search.dto.SearchDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GridController {

    private final GridService gridService;

    /**
     * 메인 데이터 조회
     */
//    @GetMapping("/randering")
//    public SearchResponseDTO getGridData(@Valid @ModelAttribute GridRequestDTO request) {
//        if (request.getLayer() == null || request.getLayer().isBlank()) {
//            throw new IllegalArgumentException("layer is required");
//        }
//        return gridService.getGridData(request);
//    }
    @PostMapping("/grid/search")
    public SearchResponseDTO gridSearchBySearchSpec(@RequestBody SearchDTO req) {
        return gridService.getGridDataBySearchSpec(req);
    }

    /**
     * 필터용 DISTINCT 값 조회
     */
    @GetMapping("/filtering")
    public FilterResponseDTO getDistinctValues(
            @RequestParam(defaultValue = "ethernet") String layer,
            @RequestParam String field,
            @RequestParam(required = false) String filterModel,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = "0") int offset,
            @RequestParam(required = false, defaultValue = "100") int limit,
            @RequestParam(required = false, defaultValue = "false") boolean includeSelf,
            @RequestParam(required = false) String orderBy,
            @RequestParam(required = false, defaultValue = "DESC") String order,
            @RequestParam(required = false) String baseSpec  // ★ SearchDTO JSON
    ) {
        log.info("[/filtering] layer={}, field={}, hasBaseSpec={}, hasFilterModel={}",
                layer, field, baseSpec != null && !baseSpec.isBlank(),
                filterModel != null && !filterModel.isBlank());
        return gridService.getDistinctValues(layer, field, filterModel, search, offset, limit, includeSelf, orderBy, order, baseSpec);
    }

    /**
     * ✅ 집계(푸터 요약)
     */
    @PostMapping("/aggregate")
    public AggregateResponseDTO aggregate(@RequestBody AggregateRequestDTO request) {
        return gridService.aggregate(request);
    }
}
