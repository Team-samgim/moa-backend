package com.moa.api.pivot.service;

import com.moa.api.pivot.dto.request.PivotQueryRequestDTO;
import com.moa.api.pivot.model.PivotLayer;
import com.moa.api.pivot.model.TimeWindow;
import com.moa.api.pivot.repository.PivotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class DefaultTopNResolver implements TopNResolver {

    private final PivotRepository pivotRepository;

    @Override
    public List<PivotQueryRequestDTO.FilterDef> resolveFilters(
            PivotLayer layer,
            List<PivotQueryRequestDTO.ValueDef> values,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw
    ) {
        if (filters == null || filters.isEmpty()) {
            return List.of();
        }

        // 1) baseFilters: topN 정보 제거 (WHERE 공통 조건용)
        List<PivotQueryRequestDTO.FilterDef> baseFilters = new ArrayList<>();
        for (PivotQueryRequestDTO.FilterDef f : filters) {
            PivotQueryRequestDTO.FilterDef copy = new PivotQueryRequestDTO.FilterDef();
            copy.setField(f.getField());
            copy.setOp(f.getOp());
            copy.setValue(f.getValue());
            copy.setOrder(f.getOrder());
            copy.setTopN(null);
            baseFilters.add(copy);
        }

        List<PivotQueryRequestDTO.FilterDef> resolved = new ArrayList<>();

        for (PivotQueryRequestDTO.FilterDef original : filters) {
            PivotQueryRequestDTO.TopNDef topN = original.getTopN();
            boolean enabled = topN != null && Boolean.TRUE.equals(topN.getEnabled());

            // TopN 비활성 → base 필터 그대로 사용
            if (!enabled) {
                baseFilters.stream()
                        .filter(f -> Objects.equals(f.getField(), original.getField()))
                        .findFirst()
                        .ifPresent(resolved::add);
                continue;
            }

            // 이 TopN에 사용할 metric alias 찾기
            PivotQueryRequestDTO.ValueDef metricDef = null;
            if (values != null) {
                for (PivotQueryRequestDTO.ValueDef v : values) {
                    if (topN.getValueKey() != null
                            && topN.getValueKey().equals(v.getAlias())) {
                        metricDef = v;
                        break;
                    }
                }
            }

            // metric 못 찾으면 안전하게 base 필터로 fallback
            if (metricDef == null) {
                baseFilters.stream()
                        .filter(f -> Objects.equals(f.getField(), original.getField()))
                        .findFirst()
                        .ifPresent(resolved::add);
                continue;
            }

            // Repo를 통해 상위/하위 N dimension 값 조회
            List<String> topNValues = pivotRepository.findTopNDimensionValues(
                    layer,
                    original.getField(),
                    topN,
                    metricDef,
                    baseFilters,
                    tw
            );

            // 해당 필터를 field IN (:topNValues) 형태로 치환
            PivotQueryRequestDTO.FilterDef replaced = new PivotQueryRequestDTO.FilterDef();
            replaced.setField(original.getField());
            replaced.setOp("IN");
            replaced.setValue(topNValues);
            replaced.setOrder(original.getOrder());
            replaced.setTopN(null);

            resolved.add(replaced);
        }

        return resolved;
    }
}
