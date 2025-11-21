package com.moa.api.grid.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moa.api.grid.config.GridProperties;
import com.moa.api.grid.dto.SqlDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.moa.api.grid.util.OrderBySanitizer.sanitizeDir;
import static com.moa.api.grid.util.OrderBySanitizer.sanitizeOrderBy;

/**
 * QueryBuilder
 *
 * 개선사항:
 * 1. SqlQueryBuilder 패턴 활용으로 가독성 대폭 향상
 * 2. StringBuilder 완전 제거
 */
@Slf4j
@Component
public class QueryBuilder {

    private final WhereBuilder whereBuilder;
    private final TemporalExprFactory temporal;
    private final LayerTableResolver tableResolver;
    private final GridProperties properties;

    public QueryBuilder(
            ObjectMapper mapper,
            TemporalExprFactory temporal,
            LayerTableResolver tableResolver,
            GridProperties properties) {
        this.whereBuilder = new WhereBuilder(mapper, temporal);
        this.temporal = temporal;
        this.tableResolver = tableResolver;
        this.properties = properties;
    }

    /**
     * Export용 SELECT 쿼리 생성
     */
    public SqlDTO buildSelectSQLForExport(
            String layer,
            List<String> columns,
            String sortField,
            String sortDirection,
            String filterModel,
            String baseSpecJson,
            Map<String, String> typeMap,
            Map<String, String> rawTemporalKindMap) {

        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("columns is required");
        }

        String table = tableResolver.resolveDataTable(layer);
        String[] quotedColumns = columns.stream()
                .map(SqlIdentifier::safeFieldName)
                .map(SqlIdentifier::quote)
                .toArray(String[]::new);

        SqlDTO whereBase = whereBuilder.fromBaseSpec(baseSpecJson, typeMap, rawTemporalKindMap);
        SqlDTO whereFilter = whereBuilder.fromFilterModel(filterModel, typeMap, rawTemporalKindMap);
        SqlDTO where = SqlDTO.and(whereBase, whereFilter);

        String safeSort = sanitizeOrderBy(sortField, typeMap, properties.getDefaultOrderBy());
        String safeDir = sanitizeDir(sortDirection);

        String orderExpr = SqlIdentifier.quote(safeSort);
        if (properties.getDefaultOrderBy().equalsIgnoreCase(safeSort)) {
            orderExpr += "::numeric";
        }

        // Builder 패턴 활용!
        SqlDTO query = SqlQueryBuilder
                .select(quotedColumns)
                .from(table + " t")
                .where(where)
                .orderBy(orderExpr, safeDir)
                .build();

        log.info("[QueryBuilder] Export SQL: {} | args={}", query.getSql(), query.getArgs());
        return query;
    }

    /**
     * DISTINCT 페이징 쿼리 생성 (CTE 활용)
     */
    public SqlDTO buildDistinctPagedSQLOrdered(
            String layer, String column, String filterModel, boolean includeSelf,
            String search, int offset, int limit,
            String orderBy, String order,
            String baseSpecJson,
            Map<String, String> typeMap,
            Map<String, String> rawTemporalKindMap) {

        String table = tableResolver.resolveDataTable(layer);
        Objects.requireNonNull(column, "column");

        String safeOrderBy = (orderBy != null && typeMap != null && typeMap.containsKey(orderBy))
                ? orderBy : properties.getDefaultOrderBy();
        String safeOrder = sanitizeDir(order);

        // WHERE 절 생성
        SqlDTO whereBase = whereBuilder.fromBaseSpec(baseSpecJson, typeMap, rawTemporalKindMap);
        SqlDTO whereFilter = includeSelf
                ? whereBuilder.fromFilterModel(filterModel, typeMap, rawTemporalKindMap)
                : whereBuilder.fromFilterModelExcluding(filterModel, column, typeMap, rawTemporalKindMap);
        SqlDTO where = SqlDTO.and(whereBase, whereFilter);

        // SELECT/ORDER 표현식 생성
        DistinctQueryParts parts = buildDistinctQueryParts(
                column, safeOrderBy, typeMap, rawTemporalKindMap);

        // Base CTE: 기본 데이터 + NOT NULL + SEARCH
        SqlDTO notNullCondition = SqlDTO.raw(parts.notNullExpr + " IS NOT NULL");
        SqlDTO baseWhere = SqlDTO.and(where, notNullCondition);

        if (search != null && !search.isBlank()) {
            SqlDTO searchCondition = SqlDTO.of(parts.selectExpr + " ILIKE ?", List.of(search + "%"));
            baseWhere = SqlDTO.and(baseWhere, searchCondition);
        }

        SqlQueryBuilder baseCte = SqlQueryBuilder
                .select(parts.selectExpr + " AS v", parts.orderExpr + " AS ord")
                .from(table + " t")
                .where(baseWhere);

        // Dedup CTE: MIN(ord)로 중복 제거
        SqlQueryBuilder dedupCte = SqlQueryBuilder
                .select("v", "MIN(ord) AS first_ord")
                .from("base")
                .groupBy("v");

        // Main Query: 최종 정렬 + 페이징
        SqlQueryBuilder mainQuery = SqlQueryBuilder
                .select("v")
                .from("dedup")
                .orderBy("first_ord", safeOrder)
                .offset(offset)
                .limit(limit);

        // CTE 조합
        SqlDTO query = SqlQueryBuilder.cte()
                .with("base", baseCte)
                .with("dedup", dedupCte)
                .mainQuery(mainQuery)
                .build();

        log.info("[QueryBuilder] Distinct SQL: {} | args={}", query.getSql(), query.getArgs());
        return query;
    }

    // === Helper Methods ===

    /**
     * DISTINCT 쿼리의 SELECT/ORDER 표현식 생성
     */
    private DistinctQueryParts buildDistinctQueryParts(
            String column, String orderBy,
            Map<String, String> typeMap, Map<String, String> rawTemporalKindMap) {

        String colType = optLower(typeMap, column);
        String rawKind = Optional.ofNullable(rawTemporalKindMap)
                .map(m -> m.get(column))
                .orElse("timestamp");

        String selectExpr = "date".equals(colType)
                ? temporal.toDateExpr("t", column, rawKind) + "::text"
                : SqlIdentifier.quoteWithAlias("t", column) + "::text";

        String notNullExpr = "date".equals(colType)
                ? temporal.toDateExpr("t", column, rawKind)
                : SqlIdentifier.quoteWithAlias("t", column);

        String orderExpr = SqlIdentifier.quoteWithAlias("t", orderBy);
        if (properties.getDefaultOrderBy().equals(orderBy)) {
            orderExpr += "::numeric";
        }

        return new DistinctQueryParts(selectExpr, notNullExpr, orderExpr);
    }

    private String optLower(Map<String, String> m, String key) {
        if (m == null) return "";
        String v = m.get(key);
        return v == null ? "" : v.toLowerCase();
    }

    public String resolveTableName(String layer) {
        return tableResolver.resolveDataTable(layer);
    }

    public SqlDTO buildWhereFromBaseSpec(
            String baseSpecJson,
            Map<String, String> typeMap,
            Map<String, String> rawTemporalKindMap) {
        return whereBuilder.fromBaseSpec(baseSpecJson, typeMap, rawTemporalKindMap);
    }

    public SqlDTO buildWhereClause(
            String filterModel,
            Map<String, String> typeMap,
            Map<String, String> rawTemporalKindMap) {
        return whereBuilder.fromFilterModel(filterModel, typeMap, rawTemporalKindMap);
    }

    public SqlDTO buildWhereClauseExcludingField(
            String filterModel,
            String excludeField,
            Map<String, String> typeMap,
            Map<String, String> rawTemporalKindMap) {
        return whereBuilder.fromFilterModelExcluding(filterModel, excludeField, typeMap, rawTemporalKindMap);
    }

    /**
     * DISTINCT 쿼리 파트 DTO
     */
    private record DistinctQueryParts(
            String selectExpr,
            String notNullExpr,
            String orderExpr
    ) {}
}