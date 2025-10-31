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

    /** 메인 그리드 데이터 조회 */
    public SearchResponseDTO getGridData(GridRequestDTO request) {
        List<Map<String, Object>> rows = gridRepository.getGridData(
                request.getLayer(),
                request.getSortField(),
                request.getSortDirection(),
                request.getFilterModel(),
                request.getOffset(),
                request.getLimit()
        );

        // ✅ 실제 DB 기반 컬럼 타입 포함 조회
        List<SearchResponseDTO.ColumnDTO> columns = gridRepository.getColumnsWithType(request.getLayer());

        return SearchResponseDTO.builder()
                .layer(request.getLayer())
                .columns(columns)
                .rows(rows)
                .build();
    }

    public FilterResponseDTO getDistinctValues(String layer, String field, String filterModel) {
        List<String> values = gridRepository.getDistinctValues(layer, field, filterModel);
        return FilterResponseDTO.builder()
                .field(field)
                .values((List<Object>)(List<?>) values)
                .build();
    }

    /** ✅ 집계 (date 제외, number/string/ip/mac) */
    public AggregateResponseDTO aggregate(AggregateRequestDTO req) {
        return gridRepository.aggregate(req);
    }
}
