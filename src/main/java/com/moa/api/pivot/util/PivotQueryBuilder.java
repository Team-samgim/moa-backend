package com.moa.api.pivot.util;

import com.moa.api.pivot.dto.request.DistinctValuesRequestDTO;
import com.moa.api.pivot.dto.request.PivotQueryRequestDTO;
import com.moa.api.pivot.model.PivotQueryContext;
import com.moa.api.pivot.repository.SqlSupport;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

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
}
