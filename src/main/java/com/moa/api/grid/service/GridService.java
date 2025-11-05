package com.moa.api.grid.service;

import com.moa.api.grid.dto.*;
import com.moa.api.grid.repository.GridRepositoryImpl;
import com.moa.api.search.dto.SearchDTO;
import com.moa.api.search.service.SearchExecuteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GridService {

    private final GridRepositoryImpl gridRepository;
    private final SearchExecuteService executeService;

    /**
     * 메인 그리드 데이터 조회
     */
    public SearchResponseDTO getGridData(GridRequestDTO request) {
        List<Map<String, Object>> rows = gridRepository.getGridData(
                request.getLayer(),
                request.getSortField(),
                request.getSortDirection(),
                request.getFilterModel(),
                request.getOffset(),
                request.getLimit()
        );

        // 실제 DB 기반 컬럼 타입 포함 조회
        List<SearchResponseDTO.ColumnDTO> columns = gridRepository.getColumnsWithType(request.getLayer());

        return SearchResponseDTO.builder()
                .layer(request.getLayer())
                .columns(columns)
                .rows(rows)
                .build();
    }

    public FilterResponseDTO getDistinctValues(
            String layer, String field, String filterModel, String search, int offset, int limit, boolean includeSelfFromClient, String orderBy, String order, String baseSpecJson) {

        boolean includeSelf = hasSelfFilter(filterModel, field);
        var page = gridRepository.getDistinctValuesPaged(
                layer, field, filterModel, includeSelf, search, offset, limit, orderBy, order, baseSpecJson);

        return FilterResponseDTO.builder()
                .field(field)
                .values((List<Object>) (List<?>) page.values())
                .total(page.total())
                .offset(page.offset())
                .limit(page.limit())
                .nextOffset(page.nextOffset())          // 더 없으면 null
                .hasMore(page.nextOffset() != null)
                .build();
    }

    private boolean hasSelfFilter(String filterModel, String field) {
        if (filterModel == null || filterModel.isBlank()) return false;
        try {
            var om = new com.fasterxml.jackson.databind.ObjectMapper();
            var root = om.readTree(filterModel);
            if (!root.has(field)) return false;
            var node = root.get(field);
            var mode = node.path("mode").asText("");
            // 같은 필드에 어떤 필터든 있으면 자기 필터 포함해서 DISTINCT 계산
            return "checkbox".equalsIgnoreCase(mode) || "condition".equalsIgnoreCase(mode);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 집계 (date 제외, number/string/ip/mac)
     */
    public AggregateResponseDTO aggregate(AggregateRequestDTO req) {
        return gridRepository.aggregate(req);
    }

    public SearchResponseDTO getGridDataBySearchSpec(SearchDTO req) {
        System.out.println("🔍 [GridService] 받은 요청: " + req);
        System.out.println("  - layer: " + req.getLayer());
        System.out.println("  - columns: " + req.getColumns());
        System.out.println("  - conditions: " + req.getConditions());
        System.out.println("  - time: " + req.getTime());

        // 1) 데이터 조회는 SearchExecuteService 그대로 재사용
        SearchDTO out = executeService.execute(req);

        System.out.println("✅ [GridService] SearchExecuteService 결과:");
        System.out.println("  - rows 개수: " + (out.getRows() != null ? out.getRows().size() : "null"));
        System.out.println("  - total: " + out.getTotal());
        if (out.getRows() != null && !out.getRows().isEmpty()) {
            System.out.println("  - 첫 번째 row keys: " + out.getRows().get(0).keySet());
        }

        // 2) layer 결정
        String layer = (req.getLayer() == null || req.getLayer().isBlank()) ? "HTTP_PAGE" : req.getLayer();

        // 3) 전체 컬럼 메타 가져오기
        var allColumns = gridRepository.getColumnsWithType(layer);
        System.out.println("📋 [GridService] 전체 컬럼 개수: " + allColumns.size());

        // ✅ 4) 프론트에서 요청한 컬럼만 필터링
        List<String> requestedColumns = req.getColumns();
        List<SearchResponseDTO.ColumnDTO> filteredColumns;

        if (requestedColumns != null && !requestedColumns.isEmpty()) {
            System.out.println("🔎 [GridService] 요청된 컬럼: " + requestedColumns);
            // 요청된 컬럼 순서대로 필터링
            filteredColumns = requestedColumns.stream()
                    .map(colName -> allColumns.stream()
                            .filter(col -> col.getName() != null && col.getName().equals(colName))
                            .findFirst()
                            .orElse(null))
                    .filter(col -> col != null)
                    .collect(Collectors.toList());
            System.out.println("✅ [GridService] 필터링된 컬럼 개수: " + filteredColumns.size());
        } else {
            System.out.println("⚠️  [GridService] 요청된 컬럼이 없어서 전체 컬럼 사용");
            // 컬럼 지정 없으면 전체
            filteredColumns = allColumns;
        }

        return SearchResponseDTO.builder()
                .layer(layer)
                .columns(filteredColumns)
                .rows(out.getRows())
                .total(out.getTotal())  // ★ 추가: total 포함
                .build();
    }
}
