// 작성자: 최이서
package com.moa.api.pivot.util;

import com.moa.api.pivot.dto.request.PivotQueryRequestDTO;
import com.moa.api.pivot.model.PivotQueryContext;
import com.moa.api.pivot.model.TimeWindow;
import com.moa.api.pivot.repository.SqlSupport;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PivotWhereBuilder {

    private final SqlSupport sqlSupport;

    @Getter
    @AllArgsConstructor
    public static class WhereContext {
        private final String where;                 // " WHERE ..." 포함
        private final MapSqlParameterSource params; // named params
    }

    /** 기존 스타일: layerKey + timeColumn + tw + filters 직접 넘기는 버전 */
    public WhereContext build(
            String layerKey,
            String timeColumn,
            TimeWindow tw,
            List<PivotQueryRequestDTO.FilterDef> filters
    ) {
        MapSqlParameterSource ps = new MapSqlParameterSource();
        String where = sqlSupport.where(layerKey, timeColumn, tw, filters, ps);
        return new WhereContext(where, ps);
    }

    /** PivotQueryContext 기반 sugar */
    public WhereContext build(PivotQueryContext ctx) {
        String layerKey = ctxLayerKey(ctx);
        String timeField = (ctx.getTimeField() != null && !ctx.getTimeField().isBlank())
                ? ctx.getTimeField()
                : "ts_server_nsec";

        return build(layerKey, timeField, ctx.getTimeWindow(), ctx.getFilters());
    }

    private String ctxLayerKey(PivotQueryContext ctx) {
        return ctx.getLayer().name();
    }
}
