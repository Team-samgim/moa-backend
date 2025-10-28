package com.moa.api.grid.service;

import com.moa.api.grid.dto.GridRequestDTO;
import com.moa.api.grid.dto.SearchResponseDTO;
import com.moa.api.grid.dto.FilterResponseDTO;
import com.moa.api.grid.repository.GridRepositoryImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GridService {

    private final GridRepositoryImpl gridRepository;

    /** ✅ 메인 그리드 데이터 조회 */
    public SearchResponseDTO getGridData(GridRequestDTO request) {
        List<Map<String, Object>> rows = gridRepository.getGridData(
                request.getLayer(),
                request.getSortField(),
                request.getSortDirection(),
                request.getFilterModel(),
                request.getOffset(),
                request.getLimit()
        );

        List<String> columns = gridRepository.getColumns(request.getLayer());

        return SearchResponseDTO.builder()
                .layer(request.getLayer())
                .columns(columns)
                .rows(rows)
                .build();
    }

    /** ✅ DISTINCT 필터값 조회 (다른 필터 반영 포함) */
    public FilterResponseDTO getDistinctValues(String layer, String field, String filterModel) {
        List<String> values = gridRepository.getDistinctValues(layer, field, filterModel);
        return FilterResponseDTO.builder()
                .field(field)
                .values((List<Object>) (List<?>) values) // 타입 캐스팅 (String→Object)
                .build();
    }
}
