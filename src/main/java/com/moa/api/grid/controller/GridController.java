package com.moa.api.grid.controller;

import com.moa.api.grid.dto.*;
import com.moa.api.grid.service.GridService;
import com.moa.api.search.dto.SearchDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class GridController {

    private final GridService gridService;

    @PostMapping("/grid/search")
    public SearchResponseDTO gridSearchBySearchSpec(@Valid @RequestBody SearchDTO req) {
        log.debug("[/grid/search] layer={}, cols={}, time?={}, conds?={}",
                req.getLayer(), req.getColumns(),
                req.getTime() != null, req.getConditions() != null);
        return gridService.getGridDataBySearchSpec(req);
    }

    /**
     * DISTINCT 요청 (URL 길이/인코딩 이슈 방지)
     */
    @PostMapping("/grid/filtering")
    public FilterResponseDTO getDistinctValuesPost(@Valid @RequestBody DistinctValuesRequestDTO req) {
        sanitize(req);
        log.info("[POST /grid/filtering] layer={}, field={}, hasBaseSpec={}, hasFilterModel={}",
                req.getLayer(), req.getField(),
                nonEmpty(req.getBaseSpec()), nonEmpty(req.getFilterModel()));

        return gridService.getDistinctValues(
                req.getLayer(),
                req.getField(),
                req.getFilterModel(),
                emptyToBlank(req.getSearch()),
                Math.max(0, req.getOffset()),
                Math.max(1, req.getLimit()),
                /* includeSelfFromClient: 무시(서비스에서 자동 판단) */ false,
                req.getOrderBy(),
                uppercaseOrDefault(req.getOrder(), "DESC"),
                req.getBaseSpec()
        );
    }

    @GetMapping({"/filtering", "/grid/filtering"})
    public FilterResponseDTO getDistinctValuesGet(
            @RequestParam(defaultValue = "ethernet") String layer,
            @RequestParam String field,
            @RequestParam(required = false) String filterModel,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = "0") int offset,
            @RequestParam(required = false, defaultValue = "100") int limit,
            @RequestParam(required = false, defaultValue = "false") boolean includeSelf,
            @RequestParam(required = false) String orderBy,
            @RequestParam(required = false, defaultValue = "DESC") String order,
            @RequestParam(required = false) String baseSpec
    ) {
        // 정규화
        offset = Math.max(0, offset);
        limit = Math.max(1, Math.min(500, limit));
        order = uppercaseOrDefault(order, "DESC");
        search = emptyToBlank(search);

        log.info("[GET /filtering] layer={}, field={}, hasBaseSpec={}, hasFilterModel={}",
                layer, field, nonEmpty(baseSpec), nonEmpty(filterModel));

        return gridService.getDistinctValues(
                layer, field, filterModel, search, offset, limit,
                /* includeSelfFromClient: 무시(서비스에서 자동 판단) */ includeSelf,
                orderBy, order, baseSpec
        );
    }

    /**
     * 집계
     */
    @PostMapping("/aggregate")
    public AggregateResponseDTO aggregate(@Valid @RequestBody AggregateRequestDTO request) {
        return gridService.aggregate(request);
    }

    /* ---------- private utils ---------- */

    private static void sanitize(DistinctValuesRequestDTO r) {
        r.setOffset(Math.max(0, r.getOffset()));
        r.setLimit(Math.max(1, Math.min(500, r.getLimit())));
        r.setOrder(uppercaseOrDefault(r.getOrder(), "DESC"));
        r.setSearch(emptyToBlank(r.getSearch()));
    }

    private static boolean nonEmpty(String s) {
        return s != null && !s.isBlank();
    }

    private static String uppercaseOrDefault(String s, String def) {
        return (s == null || s.isBlank()) ? def : s.toUpperCase();
    }

    private static String emptyToBlank(String s) {
        return (s == null) ? "" : s;
    }
}