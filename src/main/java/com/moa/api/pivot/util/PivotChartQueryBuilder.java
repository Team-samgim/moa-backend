package com.moa.api.pivot.util;

import com.moa.api.chart.dto.request.DrilldownTimeSeriesRequestDTO;
import com.moa.api.chart.dto.request.PivotChartRequestDTO;
import com.moa.api.pivot.dto.request.PivotQueryRequestDTO;
import com.moa.api.pivot.model.PivotLayer;
import com.moa.api.pivot.model.PivotQueryContext;
import com.moa.api.pivot.model.TimeWindow;
import com.moa.api.pivot.repository.SqlSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PivotChartQueryBuilder {

    private final SqlSupport sqlSupport;
    private final PivotWhereBuilder whereBuilder;

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
        String where = sqlSupport.where(layerCode, timeField, tw, baseFilters, ps);
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
     * 단일 차트 모드에서 사용
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
        String where = sqlSupport.where(layerCode, timeField, tw, finalFilters, ps);

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
     * 단일 Column 값에 대한 차트 데이터 조회 쿼리 생성
     * 다중 차트 모드에서 사용
     */
    public QueryWithParams buildSingleColumnChartDataQuery(
            PivotQueryContext ctx,
            String colField,
            String colValue,
            String rowField,
            PivotQueryRequestDTO.ValueDef metricDef,
            List<String> yCategories,
            List<PivotQueryRequestDTO.FilterDef> filters
    ) {
        String layerCode = ctx.getLayer().getCode();
        String table = ctx.table();
        String timeField = ctx.getTimeField();
        TimeWindow tw = ctx.getTimeWindow();

        String rowExpr = ctx.col(rowField);
        String metricExpr = buildMetricExpr(metricDef, ctx.getLayer());

        // Row IN (...) 필터 추가
        List<PivotQueryRequestDTO.FilterDef> finalFilters = new ArrayList<>(filters);
        PivotQueryRequestDTO.FilterDef yFilter = new PivotQueryRequestDTO.FilterDef();
        yFilter.setField(rowField);
        yFilter.setOp("IN");
        yFilter.setValue(new ArrayList<>(yCategories));
        finalFilters.add(yFilter);

        MapSqlParameterSource ps = new MapSqlParameterSource();
        String where = sqlSupport.where(layerCode, timeField, tw, finalFilters, ps);

        String sqlText = String.format("""
            SELECT %s AS r,
                   %s AS m
            FROM %s
            %s
            GROUP BY %s
            """, rowExpr, metricExpr, table, where, rowExpr);

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
            String valCol = sqlSupport.col(layer.getCode(), v.getField());
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

    public QueryWithParams buildDrilldownTimeSeriesQuery(
            PivotQueryContext ctx,
            DrilldownTimeSeriesRequestDTO req
    ) {
        String layerKey = ctx.getLayer().name();

        String table      = sqlSupport.table(layerKey);
        String rowColumn  = sqlSupport.col(layerKey, req.getRowField());
        String timeColumn = resolveTimeColumn(layerKey, req.getTimeField());
        String metricCol  = sqlSupport.col(layerKey, req.getMetric().getField());

        String metricExpr = resolveMetricExpression(req.getMetric(), metricCol);

        // 1) 필터 merge (ctx 기본 필터 + req 필터 + 드릴다운 전용 필터)
        List<PivotQueryRequestDTO.FilterDef> mergedFilters = new ArrayList<>();
        if (ctx.getFilters() != null) {
            mergedFilters.addAll(ctx.getFilters());
        }
        if (req.getFilters() != null) {
            mergedFilters.addAll(req.getFilters());
        }

        // colField = selectedColKey
        if (req.getSelectedColKey() != null && !req.getSelectedColKey().isBlank()) {
            PivotQueryRequestDTO.FilterDef colFilter = new PivotQueryRequestDTO.FilterDef();
            colFilter.setField(req.getColField());
            colFilter.setOp("=");                         // "="
            colFilter.setValue(req.getSelectedColKey());  // 단일 값
            mergedFilters.add(colFilter);
        }

        // rowField IN (:rowKeys)
        if (req.getRowKeys() != null && !req.getRowKeys().isEmpty()) {
            PivotQueryRequestDTO.FilterDef rowFilter = new PivotQueryRequestDTO.FilterDef();
            rowFilter.setField(req.getRowField());
            rowFilter.setOp("IN");
            rowFilter.setValue(req.getRowKeys());
            mergedFilters.add(rowFilter);
        }

        // 2) whereBuilder + SqlSupport 로 where + params 생성
        var wc = whereBuilder.build(
                layerKey,
                timeColumn,                   // 시간 컬럼
                ctx.getTimeWindow(),          // TimeWindow
                mergedFilters                 // 필터 총합
        );

        String where = wc.getWhere();         // " WHERE ..." 포함
        MapSqlParameterSource ps = wc.getParams();

        // 시간 컬럼을 초 단위로 변환 (나노초 정밀도 문제 해결)
        String timeExpr;
        // req.getTimeField()에서 실제 필드명 가져오기
        String actualTimeField = req.getTimeField();
        if (actualTimeField == null || actualTimeField.isBlank()) {
            actualTimeField = com.moa.api.pivot.model.PivotLayer.from(layerKey).getDefaultTimeField();
        }

        if ("ts_server_nsec".equalsIgnoreCase(actualTimeField) || actualTimeField.toLowerCase().contains("nsec")) {
            // 나노초 -> 초 단위로 변환 후 소수점 제거 (초 단위로 집계)
            timeExpr = "FLOOR(" + timeColumn + ")";
        } else {
            // 이미 초 단위인 경우
            timeExpr = timeColumn;
        }

        String sqlText = String.format("""
            SELECT %s AS row_key,
                   %s AS ts,
                   %s AS m
            FROM %s
            %s
            GROUP BY %s, %s
            ORDER BY ts ASC
            """, rowColumn, timeExpr, metricExpr, table, where, rowColumn, timeExpr);

        return new QueryWithParams(sqlText, ps);
    }

    private String resolveTimeColumn(String layerKey, String overrideField) {
        String timeField;
        if (overrideField != null && !overrideField.isBlank()) {
            timeField = overrideField;
        } else {
            timeField = com.moa.api.pivot.model.PivotLayer.from(layerKey).getDefaultTimeField();
        }

        return sqlSupport.col(layerKey, timeField);  // 실제 SQL에서 쓸 컬럼명
    }

    private String resolveMetricExpression(PivotQueryRequestDTO.ValueDef metric, String metricCol) {
        String agg = metric.getAgg();
        if (agg == null || agg.isBlank()) {
            return metricCol;
        }
        String upper = agg.toUpperCase();
        // 필요하면 화이트리스트로 제한
        // if (!List.of("SUM","AVG","COUNT","MAX","MIN").contains(upper)) { ... }
        return upper + "(" + metricCol + ")";
    }
}