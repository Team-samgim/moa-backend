package com.moa.api.pivot.util;

import com.moa.api.chart.dto.PivotChartRequestDTO;
import com.moa.api.pivot.dto.request.PivotQueryRequestDTO;
import com.moa.api.pivot.model.PivotLayer;
import com.moa.api.pivot.model.PivotQueryContext;
import com.moa.api.pivot.model.TimeWindow;
import com.moa.api.pivot.repository.SqlSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PivotChartQueryBuilder {

    private final SqlSupport sql;

    /**
     * TopN 축 값 조회 쿼리 생성
     */
    public QueryWithParams buildTopNAxisQuery(
            PivotQueryContext ctx,
            PivotChartRequestDTO.AxisDef axisDef,
            PivotQueryRequestDTO.ValueDef metricDef,
            List<PivotQueryRequestDTO.FilterDef> baseFilters,
            int limit
    ) {
        String layerCode = ctx.getLayer().getCode();
        String table = ctx.table();
        String timeField = ctx.getTimeField();
        TimeWindow tw = ctx.getTimeWindow();

        String axisExpr = ctx.col(axisDef.getField());
        String metricExpr = buildMetricExpr(metricDef, ctx.getLayer());

        MapSqlParameterSource ps = new MapSqlParameterSource();
        String where = sql.where(layerCode, timeField, tw, baseFilters, ps);
        ps.addValue("lim", limit);

        String sqlText = String.format("""
            SELECT %s AS k,
                   %s AS m
            FROM %s
            %s
            GROUP BY %s
            ORDER BY m DESC
            LIMIT :lim
            """, axisExpr, metricExpr, table, where, axisExpr);

        return new QueryWithParams(sqlText, ps);
    }

    /**
     * 차트 데이터 조회 쿼리 생성 (X축, Y축, 메트릭값)
     */
    public QueryWithParams buildChartDataQuery(
            PivotQueryContext ctx,
            String colField,
            String rowField,
            PivotQueryRequestDTO.ValueDef metricDef,
            List<String> xCategories,
            List<String> yCategories,
            List<PivotQueryRequestDTO.FilterDef> baseFilters
    ) {
        String layerCode = ctx.getLayer().getCode();
        String table = ctx.table();
        String timeField = ctx.getTimeField();
        TimeWindow tw = ctx.getTimeWindow();

        String colExpr = ctx.col(colField);
        String rowExpr = ctx.col(rowField);
        String metricExpr = buildMetricExpr(metricDef, ctx.getLayer());

        // 최종 필터 = baseFilters + X IN (...) + Y IN (...)
        List<PivotQueryRequestDTO.FilterDef> finalFilters = new ArrayList<>(baseFilters);

        PivotQueryRequestDTO.FilterDef xFilter = new PivotQueryRequestDTO.FilterDef();
        xFilter.setField(colField);
        xFilter.setOp("IN");
        xFilter.setValue(new ArrayList<>(xCategories));
        finalFilters.add(xFilter);

        PivotQueryRequestDTO.FilterDef yFilter = new PivotQueryRequestDTO.FilterDef();
        yFilter.setField(rowField);
        yFilter.setOp("IN");
        yFilter.setValue(new ArrayList<>(yCategories));
        finalFilters.add(yFilter);

        MapSqlParameterSource ps = new MapSqlParameterSource();
        String where = sql.where(layerCode, timeField, tw, finalFilters, ps);

        String sqlText = String.format("""
            SELECT %s AS c,
                   %s AS r,
                   %s AS m
            FROM %s
            %s
            GROUP BY %s, %s
            """, colExpr, rowExpr, metricExpr, table, where, colExpr, rowExpr);

        return new QueryWithParams(sqlText, ps);
    }

    /**
     * 메트릭 표현식 생성
     */
    public String buildMetricExpr(PivotQueryRequestDTO.ValueDef v, PivotLayer layer) {
        String agg = v.getAgg() != null ? v.getAgg().toLowerCase() : "sum";

        if ("count".equals(agg)) {
            return "COUNT(*)";
        } else {
            String valCol = sql.col(layer.getCode(), v.getField());
            return switch (agg) {
                case "avg" -> "AVG(" + valCol + ")";
                case "min" -> "MIN(" + valCol + ")";
                case "max" -> "MAX(" + valCol + ")";
                default -> "SUM(" + valCol + ")";
            };
        }
    }

    /**
     * 쿼리와 파라미터를 함께 담는 DTO
     */
    public record QueryWithParams(String sql, MapSqlParameterSource params) {}
}