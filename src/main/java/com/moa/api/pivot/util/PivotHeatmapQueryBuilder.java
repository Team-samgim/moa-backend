package com.moa.api.pivot.util;

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
public class PivotHeatmapQueryBuilder {

    private final SqlSupport sql;

    /**
     * X축 카테고리 조회 쿼리 (상위 N개)
     */
    public QueryWithParams buildXCategoriesQuery(
            PivotQueryContext ctx,
            String colField,
            PivotQueryRequestDTO.ValueDef metric,
            int limit
    ) {
        String layerCode = ctx.getLayer().getCode();
        String table = ctx.table();
        String timeField = ctx.getTimeField();
        TimeWindow tw = ctx.getTimeWindow();
        List<PivotQueryRequestDTO.FilterDef> filters = ctx.getFilters();

        String xCol = ctx.col(colField);
        String metricExpr = buildMetricExpr(metric, ctx.getLayer());

        MapSqlParameterSource ps = new MapSqlParameterSource();
        String where = sql.where(layerCode, timeField, tw, filters, ps);

        String sqlText = String.format("""
            SELECT %s AS cx,
                   %s AS m0
            FROM %s
            %s
            GROUP BY %s
            ORDER BY m0 DESC
            LIMIT :limitX
            """, xCol, metricExpr, table, where, xCol);

        ps.addValue("limitX", limit);

        return new QueryWithParams(sqlText, ps);
    }

    /**
     * Y축 전체 카테고리 수 조회
     */
    public QueryWithParams buildYCountQuery(
            PivotQueryContext ctx,
            String rowField
    ) {
        String layerCode = ctx.getLayer().getCode();
        String table = ctx.table();
        String timeField = ctx.getTimeField();
        TimeWindow tw = ctx.getTimeWindow();
        List<PivotQueryRequestDTO.FilterDef> filters = ctx.getFilters();

        String yCol = ctx.col(rowField);

        MapSqlParameterSource ps = new MapSqlParameterSource();
        String where = sql.where(layerCode, timeField, tw, filters, ps);

        String sqlText = String.format("""
            SELECT COUNT(DISTINCT %s) AS cnt
            FROM %s
            %s
            """, yCol, table, where);

        return new QueryWithParams(sqlText, ps);
    }

    /**
     * Y축 페이지별 카테고리 조회 쿼리
     */
    public QueryWithParams buildYCategoriesPageQuery(
            PivotQueryContext ctx,
            String rowField,
            PivotQueryRequestDTO.ValueDef metric,
            int offset,
            int limit
    ) {
        String layerCode = ctx.getLayer().getCode();
        String table = ctx.table();
        String timeField = ctx.getTimeField();
        TimeWindow tw = ctx.getTimeWindow();
        List<PivotQueryRequestDTO.FilterDef> filters = ctx.getFilters();

        String yCol = ctx.col(rowField);
        String metricExpr = buildMetricExpr(metric, ctx.getLayer());

        MapSqlParameterSource ps = new MapSqlParameterSource();
        String where = sql.where(layerCode, timeField, tw, filters, ps);

        String sqlText = String.format("""
            SELECT %s AS ry,
                   %s AS m0
            FROM %s
            %s
            GROUP BY %s
            ORDER BY m0 DESC
            LIMIT :limit OFFSET :offset
            """, yCol, metricExpr, table, where, yCol);

        ps.addValue("limit", limit);
        ps.addValue("offset", offset);

        return new QueryWithParams(sqlText, ps);
    }

    /**
     * 히트맵 셀 값 조회 쿼리
     */
    public QueryWithParams buildCellValuesQuery(
            PivotQueryContext ctx,
            String colField,
            String rowField,
            PivotQueryRequestDTO.ValueDef metric,
            List<String> xCategories,
            List<String> yCategories
    ) {
        String layerCode = ctx.getLayer().getCode();
        String table = ctx.table();
        String timeField = ctx.getTimeField();
        TimeWindow tw = ctx.getTimeWindow();

        String xCol = ctx.col(colField);
        String yCol = ctx.col(rowField);
        String metricExpr = buildMetricExpr(metric, ctx.getLayer());

        List<PivotQueryRequestDTO.FilterDef> filters = new ArrayList<>(
                ctx.getFilters() != null ? ctx.getFilters() : List.of()
        );

        // X, Y IN 필터 추가
        PivotQueryRequestDTO.FilterDef fx = new PivotQueryRequestDTO.FilterDef();
        fx.setField(colField);
        fx.setOp("IN");
        fx.setValue(new ArrayList<>(xCategories));
        filters.add(fx);

        PivotQueryRequestDTO.FilterDef fy = new PivotQueryRequestDTO.FilterDef();
        fy.setField(rowField);
        fy.setOp("IN");
        fy.setValue(new ArrayList<>(yCategories));
        filters.add(fy);

        MapSqlParameterSource ps = new MapSqlParameterSource();
        String where = sql.where(layerCode, timeField, tw, filters, ps);

        String sqlText = String.format("""
            SELECT %s AS cx,
                   %s AS ry,
                   %s AS m0
            FROM %s
            %s
            GROUP BY %s, %s
            """, xCol, yCol, metricExpr, table, where, xCol, yCol);

        return new QueryWithParams(sqlText, ps);
    }

    private String buildMetricExpr(PivotQueryRequestDTO.ValueDef v, PivotLayer layer) {
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

    public record QueryWithParams(String sql, MapSqlParameterSource params) {}
}