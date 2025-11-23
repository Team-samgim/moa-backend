package com.moa.api.grid.service;

import com.moa.api.grid.dto.*;
import com.moa.api.grid.repository.GridRepositoryImpl;
import com.moa.api.grid.validation.GridValidator;
import com.moa.api.search.dto.SearchDTO;
import com.moa.api.search.service.SearchExecuteService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GridService {

    private final GridRepositoryImpl gridRepository;
    private final SearchExecuteService executeService;
    private final GridValidator validator;  // 추가
    private final ObjectMapper om = new ObjectMapper();

    /**
     * 필터 Distinct - Validation 추가
     */
    public FilterResponseDTO getDistinctValues(
            String layer,
            String field,
            String filterModel,
            String search,
            int offset,
            int limit,
            boolean includeSelfFromClient,
            String orderBy,
            String order,
            String baseSpecJson) {

        // Validation
        validator.validateLayer(layer);
        validator.validateFilterModel(filterModel);
        validator.validateBaseSpec(baseSpecJson);

        // 클라 의도 OR 자기필터 감지
        boolean includeSelf = includeSelfFromClient || hasSelfFilter(filterModel, field);

        var page = gridRepository.getDistinctValuesPaged(
                layer, field, filterModel, includeSelf, search, offset, limit, orderBy, order, baseSpecJson);

        List<Object> values = page.values().stream().map(v -> (Object) v).toList();

        return FilterResponseDTO.builder()
                .field(field)
                .values(values)
                .total(page.total())
                .offset(page.offset())
                .limit(page.limit())
                .nextOffset(page.nextOffset())
                .hasMore(page.nextOffset() != null)
                .build();
    }

    private boolean hasSelfFilter(String filterModel, String field) {
        if (filterModel == null || filterModel.isBlank() || field == null || field.isBlank()) return false;
        try {
            JsonNode root = om.readTree(filterModel);
            String target = safeFieldName(field);

            for (Iterator<String> it = root.fieldNames(); it.hasNext(); ) {
                String k = it.next();
                if (!safeFieldName(k).equals(target)) continue;

                JsonNode node = root.get(k);
                String mode = node.path("mode").asText("");
                if ("checkbox".equalsIgnoreCase(mode) || "condition".equalsIgnoreCase(mode)) return true;
            }
            return false;
        } catch (Exception e) {
            log.debug("[GridService] hasSelfFilter parse fail: {}", e.getMessage());
            return false;
        }
    }

    private String safeFieldName(String f) {
        if (f == null) return "";
        int i = f.indexOf('-');
        return (i >= 0) ? f.substring(0, i) : f;
    }

    /**
     * 집계
     */
    public AggregateResponseDTO aggregate(AggregateRequestDTO req) {
        // Validation
        validator.validateLayer(req.getLayer());
        validator.validateFilterModel(req.getFilterModel());
        validator.validateBaseSpec(req.getBaseSpecJson());

        return gridRepository.aggregate(req);
    }

    /**
     * 컬럼 메타데이터 조회
     */
    public List<SearchResponseDTO.ColumnDTO> getColumnsWithType(String layer) {
        validator.validateLayer(layer);
        return gridRepository.getColumnsWithType(layer);
    }

    /**
     * 그리드 데이터 (SearchSpec 기반)
     */
    public SearchResponseDTO getGridDataBySearchSpec(SearchDTO req) {
        log.info("[GridService] 받은 요청 layer={}, columns={}, conditions={}, time={}",
                req.getLayer(), req.getColumns(), req.getConditions(), req.getTime());

        // Validation
        validator.validateLayer(req.getLayer());

        SearchDTO out = executeService.execute(req);
        log.info("[GridService] rows={}, total={}",
                (out.getRows() != null ? out.getRows().size() : null), out.getTotal());
        if (out.getRows() != null && !out.getRows().isEmpty()) {
            log.debug("[GridService] 첫 row keys={}", out.getRows().get(0).keySet());
        }

        String layer = (req.getLayer() == null || req.getLayer().isBlank()) ? "http_page" : req.getLayer();

        var allColumns = gridRepository.getColumnsWithType(layer);
        log.info("[GridService] 전체 컬럼 수={}", allColumns.size());

        List<String> requested = req.getColumns();
        List<SearchResponseDTO.ColumnDTO> filteredColumns;
        if (requested != null && !requested.isEmpty()) {
            Map<String, SearchResponseDTO.ColumnDTO> index =
                    allColumns.stream().collect(Collectors.toMap(SearchResponseDTO.ColumnDTO::getName, c -> c, (a, b) -> a));
            filteredColumns = requested.stream()
                    .map(index::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            log.info("[GridService] 필터링된 컬럼 수={}", filteredColumns.size());
        } else {
            filteredColumns = allColumns;
        }

        return SearchResponseDTO.builder()
                .layer(layer)
                .columns(filteredColumns)
                .rows(out.getRows())
                .total(out.getTotal())
                .build();
    }
}