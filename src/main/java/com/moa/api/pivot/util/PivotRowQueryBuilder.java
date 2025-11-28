// 작성자: 최이서
package com.moa.api.pivot.util;

import com.moa.api.pivot.dto.request.PivotQueryRequestDTO;
import com.moa.api.pivot.model.PivotQueryContext;
import com.moa.api.pivot.repository.SqlSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Pivot Row/Column 관련 SQL 쿼리 빌더
 * - Row 값 조회 (전체/페이징)
 * - Column별 요약
 * - Row x Column breakdown
 */
@Component
@RequiredArgsConstructor
public class PivotRowQueryBuilder {

    private final SqlSupport sql;

    /**
     * Row 값 전체 조회 쿼리
     */
    public QueryWithParams buildDistinctRowValuesQuery(
            PivotQueryContext ctx,
            String rowField
    ) {
        String table = ctx.table();
        String col = ctx.col(rowField);

        MapSqlParameterSource ps = ctx.getParams();
        String where = ctx.baseWhere();

        String sqlText = String.format("""
            SELECT DISTINCT %s AS val
            FROM %s
            %s
            ORDER BY %s ASC
            """, col, table, where, col);

        return new QueryWithParams(sqlText, ps);
    }

    /**
     * Row 값 페이징 조회 쿼리
     */
    public QueryWithParams buildDistinctRowValuesPageQuery(
            PivotQueryContext ctx,
            String rowField,
            int offset,
            int limit
    ) {
        String table = ctx.table();
        String col = ctx.col(rowField);

        MapSqlParameterSource ps = ctx.getParams();
        String where = ctx.baseWhere();

        String sqlText = String.format("""
            SELECT DISTINCT %s AS val
            FROM %s
            %s
            ORDER BY %s ASC
            LIMIT :limitRows OFFSET :offsetRows
            """, col, table, where, col);

        ps.addValue("limitRows", limit);
        ps.addValue("offsetRows", offset);

        return new QueryWithParams(sqlText, ps);
    }

    /**
     * Column별 요약 쿼리
     */
    public QueryWithParams buildSummaryByColumnQuery(
            PivotQueryContext ctx,
            String columnField,
            List<PivotQueryRequestDTO.ValueDef> metrics
    ) {
        String table = ctx.table();
        String col = ctx.col(columnField);

        String aggSelect = buildAggregateSelect(ctx, metrics);

        MapSqlParameterSource ps = ctx.getParams();
        String where = ctx.baseWhere();

        String sqlText = String.format("""
            SELECT %s AS col_val,
                   %s
            FROM %s
            %s
            GROUP BY %s
            """, col, aggSelect, table, where, col);

        return new QueryWithParams(sqlText, ps);
    }

    /**
     * Row x Column breakdown 쿼리
     */
    public QueryWithParams buildBreakdownQuery(
            PivotQueryContext ctx,
            String rowField,
            String columnField,
            List<PivotQueryRequestDTO.ValueDef> metrics
    ) {
        String table = ctx.table();
        String row = ctx.col(rowField);
        String col = ctx.col(columnField);

        String aggSelect = buildAggregateSelect(ctx, metrics);

        MapSqlParameterSource ps = ctx.getParams();
        String where = ctx.baseWhere();

        String sqlText = String.format("""
            SELECT %s AS row_val,
                   %s AS col_val,
                   %s
            FROM %s
            %s
            GROUP BY %s, %s
            """, row, col, aggSelect, table, where, row, col);

        return new QueryWithParams(sqlText, ps);
    }

    /**
     * 메트릭 집계 SELECT 절 생성
     */
    private String buildAggregateSelect(
            PivotQueryContext ctx,
            List<PivotQueryRequestDTO.ValueDef> metrics
    ) {
        return metrics.stream()
                .map(m -> m.getAgg().toUpperCase() + "(" + ctx.col(m.getField()) + ") AS \"" + m.getAlias() + "\"")
                .collect(Collectors.joining(", "));
    }

    public record QueryWithParams(String sql, MapSqlParameterSource params) {}
}