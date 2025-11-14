package com.moa.api.pivot.model;

import com.moa.api.pivot.dto.request.PivotQueryRequestDTO;
import com.moa.api.pivot.repository.SqlSupport;
import lombok.Getter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class PivotQueryContext {

    private final PivotLayer layer;
    private final String timeField;
    private final TimeWindow timeWindow;
    private final List<PivotQueryRequestDTO.FilterDef> filters;
    private final SqlSupport sqlSupport;
    private final MapSqlParameterSource params;

    public PivotQueryContext(
            PivotLayer layer,
            String timeField,
            TimeWindow timeWindow,
            List<PivotQueryRequestDTO.FilterDef> filters,
            SqlSupport sqlSupport
    ) {
        this.layer = layer;
        this.timeField = (timeField == null || timeField.isBlank())
                ? layer.getDefaultTimeField()
                : timeField;
        this.timeWindow = timeWindow;
        this.filters = filters != null ? List.copyOf(filters) : Collections.emptyList();
        this.sqlSupport = sqlSupport;
        this.params = new MapSqlParameterSource();
    }

    /** 기본 where (시간 + filters) */
    public String baseWhere() {
        return sqlSupport.where(layer.getCode(), timeField, timeWindow, filters, params);
    }

    /** 추가 필터를 포함한 where 생성 */
    public String whereWith(List<PivotQueryRequestDTO.FilterDef> extraFilters) {
        List<PivotQueryRequestDTO.FilterDef> merged = new ArrayList<>(filters);
        if (extraFilters != null && !extraFilters.isEmpty()) {
            merged.addAll(extraFilters);
        }
        return sqlSupport.where(layer.getCode(), timeField, timeWindow, merged, params);
    }

    public String col(String fieldKey) {
        return sqlSupport.col(layer.getCode(), fieldKey);
    }

    public String table() {
        return sqlSupport.table(layer.getCode());
    }
}
