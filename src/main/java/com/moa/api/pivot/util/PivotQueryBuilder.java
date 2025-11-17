package com.moa.api.pivot.util;

import com.moa.api.chart.dto.request.DrilldownTimeSeriesRequestDTO;
import com.moa.api.pivot.dto.request.DistinctValuesRequestDTO;
import com.moa.api.pivot.dto.request.PivotQueryRequestDTO;
import com.moa.api.pivot.model.PivotQueryContext;
import com.moa.api.pivot.repository.SqlSupport;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PivotQueryBuilder {

    private final SqlSupport sqlSupport;
    private final PivotWhereBuilder whereBuilder;

    @Getter
    @AllArgsConstructor
    public static class NamedSql {
        private final String sql;
        private final MapSqlParameterSource params;
    }

    private String layerKey(PivotQueryContext ctx) {
        // 🔧 enum 설계에 맞게 수정
        return ctx.getLayer().name();
    }

    private String resolveTimeField(PivotQueryContext ctx) {
        return (ctx.getTimeField() != null && !ctx.getTimeField().isBlank())
                ? ctx.getTimeField()
                : "ts_server_nsec";
    }

    /* ================== 1) Distinct Values: COUNT 쿼리 ================== */

    public NamedSql buildDistinctValuesCountSql(
            PivotQueryContext ctx,
            DistinctValuesRequestDTO req
    ) {
        String layerKey = layerKey(ctx);
        String table = sqlSupport.table(layerKey);
        String col   = sqlSupport.col(layerKey, req.getField());

        // 기본 where (시간 + 필터)
        var wc = whereBuilder.build(
                layerKey,
                resolveTimeField(ctx),
                ctx.getTimeWindow(),
                ctx.getFilters()
        );
        String where = wc.getWhere();
        MapSqlParameterSource ps = wc.getParams();

        // 검색어
        if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
            where += " AND " + col + " ILIKE :kw ";
            ps.addValue("kw", "%" + req.getKeyword() + "%");
        }

        String sql = """
            SELECT COUNT(DISTINCT %s)
            FROM %s
            %s
        """.formatted(col, table, where);

        return new NamedSql(sql, ps);
    }

    /* ================== 2) Distinct Values: 페이지 쿼리 ================== */

    public NamedSql buildDistinctValuesPageSql(
            PivotQueryContext ctx,
            DistinctValuesRequestDTO req,
            int effectiveLimit
    ) {
        String layerKey = layerKey(ctx);
        String table = sqlSupport.table(layerKey);
        String col   = sqlSupport.col(layerKey, req.getField());

        // 기본 where (시간 + 필터)
        var wc = whereBuilder.build(
                layerKey,
                resolveTimeField(ctx),
                ctx.getTimeWindow(),
                ctx.getFilters()
        );
        String whereForCount = wc.getWhere();
        String whereForPage  = wc.getWhere();
        MapSqlParameterSource ps = wc.getParams();

        // 검색어
        if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
            String kwExpr = " AND " + col + " ILIKE :kw ";
            whereForCount += kwExpr;
            whereForPage  += kwExpr;
            ps.addValue("kw", "%" + req.getKeyword() + "%");
        }

        // 커서 조건 (페이지 쿼리에만)
        if (req.getCursor() != null && !req.getCursor().isBlank()) {
            whereForPage = sqlSupport.appendCursorCondition(
                    layerKey,
                    req.getField(),
                    req.getOrder(),
                    whereForPage,
                    req.getCursor(),
                    ps
            );
        }

        String ord = "DESC".equalsIgnoreCase(req.getOrder()) ? "DESC" : "ASC";

        String sql = """
            SELECT DISTINCT %s AS val
            FROM %s
            %s
            ORDER BY %s %s NULLS LAST
            LIMIT :lim
        """.formatted(col, table, whereForPage, col, ord);

        ps.addValue("lim", effectiveLimit);

        return new NamedSql(sql, ps);
    }

    /* ================== 3) Column 축 상위 값 ================== */

    public NamedSql buildTopColumnValuesSql(
            PivotQueryContext ctx,
            String columnField
    ) {
        String layerKey = layerKey(ctx);
        String table = sqlSupport.table(layerKey);
        String col   = sqlSupport.col(layerKey, columnField);

        var wc = whereBuilder.build(
                layerKey,
                resolveTimeField(ctx),
                ctx.getTimeWindow(),
                ctx.getFilters()
        );

        String where = wc.getWhere();
        MapSqlParameterSource ps = wc.getParams();

        String sql = """
            SELECT %s AS col_val, COUNT(*) AS cnt
            FROM %s
            %s
            GROUP BY %s
            ORDER BY cnt DESC, %s ASC
        """.formatted(col, table, where, col, col);

        return new NamedSql(sql, ps);
    }

    public record QueryWithParams(String sql, MapSqlParameterSource params) {}

    public QueryWithParams buildDrilldownTimeSeriesQuery(
            PivotQueryContext ctx,
            DrilldownTimeSeriesRequestDTO req
    ) {
        String layerKey = ctx.getLayer().name();

        String table      = sqlSupport.table(layerKey);
        String rowColumn  = sqlSupport.col(layerKey, req.getRowField());
        String timeColumn = sqlSupport.col(layerKey, req.getTimeField());
        String metricCol  = sqlSupport.col(layerKey, req.getMetric().getField());

        String metricExpr = resolveMetricExpression(req.getMetric(), metricCol); // ex) "AVG(col)"

        // ===== 1) 필터 머지 (ctx 기본 필터 + 드릴다운 전용 필터)
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
            colFilter.setOp("eq");
            colFilter.setValue(req.getSelectedColKey());
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

        // ===== 2) whereBuilder + SqlSupport 로 where + params 만들기
        var wc = whereBuilder.build(
                layerKey,
                timeColumn,              // 시간 컬럼
                ctx.getTimeWindow(),     // TimeWindow
                mergedFilters            // 합쳐진 필터
        );
        String where = wc.getWhere();             // " WHERE ..." 포함
        MapSqlParameterSource ps = wc.getParams();

        // ===== 3) time bucket (필요하면 나중에 확장)
        String timeExpr = resolveTimeExpression(timeColumn, req.getTimeBucket());
        // ex) RAW → timeColumn, MINUTE → floor(...) 등

        String sqlText = """
            SELECT %s AS row_key,
                   %s AS ts,
                   %s AS m
            FROM %s
            %s
            GROUP BY row_key, ts
            ORDER BY ts ASC
            """.formatted(rowColumn, timeExpr, metricExpr, table, where);

        return new QueryWithParams(sqlText, ps);
    }

    private String resolveMetricExpression(PivotQueryRequestDTO.ValueDef metric, String metricCol) {
        String agg = metric.getAgg();
        if (agg == null || agg.isBlank()) {
            return metricCol; // 그냥 값
        }
        String upper = agg.toUpperCase();
        // 필요하면 AVG, SUM, COUNT, MAX, MIN 등만 허용하도록 방어 코딩
        return upper + "(" + metricCol + ")";
    }

    private String resolveTimeExpression(String timeColumn, String timeBucket) {
        if (timeBucket == null || timeBucket.isBlank() || "RAW".equalsIgnoreCase(timeBucket)) {
            // ts_server_nesc가 double(sec)이라면 그대로 반환
            return timeColumn;
        }
        // 예: 분 단위 버킷팅을 하고 싶다면 나중에 이런 식으로 확장 가능
        // if ("MINUTE".equalsIgnoreCase(timeBucket)) {
        //     return "FLOOR(" + timeColumn + " / 60) * 60";
        // }
        return timeColumn;
    }
}
