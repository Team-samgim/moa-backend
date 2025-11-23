package com.moa.api.grid.controller;

import com.moa.api.export.dto.ExportGridRequestDTO;
import com.moa.api.grid.dto.*;
import com.moa.api.grid.service.GridAsyncService;
import com.moa.api.grid.service.GridService;
import com.moa.api.search.dto.SearchDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class GridController {

    private final GridService gridService;
    private final GridAsyncService asyncService;

    /**
     * 동기 집계
     */
    @PostMapping("/aggregate")
    public AggregateResponseDTO aggregate(
            @Valid @RequestBody AggregateRequestDTO request) {
        return asyncService.aggregateSync(request);
    }

    /**
     * 비동기 집계 (새로운 API)
     */
    @PostMapping("/aggregate/async")
    public DeferredResult<ResponseEntity<AggregateResponseDTO>> aggregateAsync(
            @Valid @RequestBody AggregateRequestDTO request) {

        // 30초 타임아웃
        DeferredResult<ResponseEntity<AggregateResponseDTO>> result =
                new DeferredResult<>(30000L);

        // 타임아웃 핸들러
        result.onTimeout(() -> {
            log.warn("Aggregate request timed out");
            result.setErrorResult(
                    ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                            .body(null)
            );
        });

        // 비동기 실행
        CompletableFuture<AggregateResponseDTO> future =
                asyncService.aggregateAsync(request);

        // 완료 시 결과 설정
        future.thenAccept(response -> {
                    result.setResult(ResponseEntity.ok(response));
                })
                .exceptionally(ex -> {
                    log.error("Aggregate failed", ex);
                    result.setErrorResult(
                            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body(null)
                    );
                    return null;
                });

        return result;
    }

    /**
     * 캐시 상태 조회 (모니터링용)
     */
    @GetMapping("/aggregate/cache-stats")
    public ResponseEntity<?> getCacheStats() {
        var stats = asyncService.getCacheStats();
        return ResponseEntity.ok(Map.of(
                "cached_results", stats.cachedResults(),
                "running_tasks", stats.runningTasks(),
                "hit_rate", String.format("%.2f%%", stats.hitRate() * 100)
        ));
    }

    /**
     * DISTINCT 필터링 (POST)
     */
    @PostMapping("/grid/filtering")
    public FilterResponseDTO getDistinctValuesPost(
            @Valid @RequestBody DistinctValuesRequestDTO req) {

        sanitize(req);

        log.debug("[POST /grid/filtering] layer={}, field={}", req.getLayer(), req.getField());

        return gridService.getDistinctValues(
                req.getLayer(),
                req.getField(),
                req.getFilterModel(),
                emptyToBlank(req.getSearch()),
                Math.max(0, req.getOffset()),
                Math.max(1, req.getLimit()),
                false,
                req.getOrderBy(),
                uppercaseOrDefault(req.getOrder(), "DESC"),
                req.getBaseSpec()
        );
    }

    /**
     * DISTINCT 필터링 (GET)
     */
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
            @RequestParam(required = false) String baseSpec) {

        offset = Math.max(0, offset);
        limit = Math.max(1, Math.min(500, limit));
        order = uppercaseOrDefault(order, "DESC");
        search = emptyToBlank(search);

        return gridService.getDistinctValues(
                layer, field, filterModel, search, offset, limit,
                false, orderBy, order, baseSpec
        );
    }

    /**
     * 그리드 데이터 조회 (SearchSpec 기반)
     */
    @PostMapping("/grid/search")
    public SearchResponseDTO getGridData(@RequestBody SearchDTO req) {
        log.info("[POST /grid/search] Received request: layer={}, columns={}, conditions={}",
                req.getLayer(), req.getColumns(), req.getConditions());
        return gridService.getGridDataBySearchSpec(req);
    }

    /**
     * 컬럼 메타데이터 조회
     */
    @GetMapping("/grid/columns")
    public List<SearchResponseDTO.ColumnDTO> getColumns(
            @RequestParam(defaultValue = "ethernet") String layer) {
        log.info("[GET /grid/columns] layer={}", layer);
        return gridService.getColumnsWithType(layer);
    }

    private static void sanitize(DistinctValuesRequestDTO r) {
        r.setOffset(Math.max(0, r.getOffset()));
        r.setLimit(Math.max(1, Math.min(500, r.getLimit())));
        r.setOrder(uppercaseOrDefault(r.getOrder(), "DESC"));
        r.setSearch(emptyToBlank(r.getSearch()));
    }

    private static String uppercaseOrDefault(String s, String def) {
        return (s == null || s.isBlank()) ? def : s.toUpperCase();
    }

    private static String emptyToBlank(String s) {
        return (s == null) ? "" : s;
    }
}