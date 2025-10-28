package com.moa.api.grid.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moa.api.grid.dto.DistinctValueResponseDTO;
import com.moa.api.grid.dto.FilterRequestDTO;
import com.moa.api.grid.dto.SearchResponseDTO;
import com.moa.api.grid.repository.GridRepository;
import com.moa.api.grid.util.QueryBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GridService {

    private final GridRepository gridRepository;

    public SearchResponseDTO getGridData(FilterRequestDTO request) {
        List<Map<String, Object>> rows = gridRepository.findRows(request);
        List<String> columns = gridRepository.getColumns(request.getLayer());
        return SearchResponseDTO.builder()
                .layer(request.getLayer())
                .columns(columns)
                .rows(rows)
                .build();
    }

    public DistinctValueResponseDTO getDistinctValues(String layer, String field) {
        Map<String, Object> result = gridRepository.getDistinctValues(layer, field);
        return DistinctValueResponseDTO.builder()
                .field((String) result.get("field"))
                .values((List<Object>) result.get("values"))
                .error((String) result.getOrDefault("error", null))
                .build();
    }
}

