package com.moa.api.grid.service;

import com.moa.api.grid.dto.*;
import com.moa.api.grid.repository.GridRepositoryImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GridService {

    private final GridRepositoryImpl gridRepository;

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
            String layer, String field, String filterModel, String search, int offset, int limit, boolean includeSelfFromClient) {

        boolean includeSelf = hasSelfFilter(filterModel, field);
        var page = gridRepository.getDistinctValuesPaged(
                layer, field, filterModel, includeSelf, search, offset, limit);

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
}
