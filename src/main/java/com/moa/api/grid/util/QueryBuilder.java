package com.moa.api.grid.util;

/*****************************************************************************
 CLASS NAME    : QueryBuilder
 DESCRIPTION   : Grid API의 모든 동적 SQL 생성 로직을 담당하는 핵심 빌더.
 - Export용 SELECT 쿼리 생성
 - DISTINCT 페이징 쿼리 생성 (CTE 기반)
 - WHERE 절 생성(필터/베이스 스펙)
 - 정렬(OrderBy) 보안 처리
 AUTHOR        : 방대혁
 ******************************************************************************/

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

@Slf4j
@Component
public class QueryBuilder {

    private final WhereBuilder whereBuilder;
    private final TemporalExprFactory temporal;
    private final LayerTableResolver tableResolver;
    private final GridProperties properties;

    /**
     * 생성자: WhereBuilder, TemporalFactory, 테이블맵핑 Resolver, 설정값 주입
     */
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

    /***************************************************************************
     *                           EXPORT SELECT SQL
     ***************************************************************************
     * CSV Export 시 필요한 SELECT 쿼리 생성
     * - 컬럼 검증
     * - 안전한 정렬 컬럼/방향 적용
     * - WHERE 절(BaseSpec + FilterModel)
     ***************************************************************************/
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

        // SELECT 절: "column" 형태로 인용
        String[] quotedColumns = columns.stream()
                .map(SqlIdentifier::safeFieldName)
                .map(SqlIdentifier::quote)
                .toArray(String[]::new);

        // WHERE 절 구성
        SqlDTO whereBase = whereBuilder.fromBaseSpec(baseSpecJson, typeMap, rawTemporalKindMap);
        SqlDTO whereFilter = whereBuilder.fromFilterModel(filterModel, typeMap, rawTemporalKindMap);
        SqlDTO where = SqlDTO.and(whereBase, whereFilter);

        // 정렬
        String safeSort = sanitizeOrderBy(sortField, typeMap, properties.getDefaultOrderBy());
        String safeDir = sanitizeDir(sortDirection);

        // 정렬 컬럼 numeric 변환(정렬 일관성)
        String orderExpr = SqlIdentifier.quote(safeSort);
        if (properties.getDefaultOrderBy().equalsIgnoreCase(safeSort)) {
            orderExpr += "::numeric";
        }

        // Builder 패턴으로 최종 쿼리 생성
        SqlDTO query = SqlQueryBuilder
                .select(quotedColumns)
                .from(table + " t")
                .where(where)
                .orderBy(orderExpr, safeDir)
                .build();

        log.info("[QueryBuilder] Export SQL: {} | args={}", query.getSql(), query.getArgs());
        return query;
    }

    /***************************************************************************
     *                     DISTINCT + PAGINATION SQL (CTE 기반)
     ***************************************************************************
     * DISTINCT 값 + 정렬 + 페이징을 수행하기 위한 고급 SQL 생성기
     * - base CTE: 필터 적용 후 원본 v, ord 생성
     * - dedup CTE: GROUP BY v로 중복제거 후 첫번째 정렬(ord) 사용
     * - main   : 페이징된 결과 반환
     ***************************************************************************/
    public SqlDTO buildDistinctPagedSQLOrdered(
            String layer, String column, String filterModel, boolean includeSelf,
            String search, int offset, int limit,
            String orderBy, String order,
            String baseSpecJson,
            Map<String, String> typeMap,
            Map<String, String> rawTemporalKindMap) {

        String table = tableResolver.resolveDataTable(layer);
        Objects.requireNonNull(column, "column");

        // 정렬 검증
        String safeOrderBy = (orderBy != null && typeMap != null && typeMap.containsKey(orderBy))
                ? orderBy
                : properties.getDefaultOrderBy();
        String safeOrder = sanitizeDir(order);

        // WHERE 절 구성
        SqlDTO whereBase = whereBuilder.fromBaseSpec(baseSpecJson, typeMap, rawTemporalKindMap);
        SqlDTO whereFilter = includeSelf
                ? whereBuilder.fromFilterModel(filterModel, typeMap, rawTemporalKindMap)
                : whereBuilder.fromFilterModelExcluding(filterModel, column, typeMap, rawTemporalKindMap);
        SqlDTO where = SqlDTO.and(whereBase, whereFilter);

        // SELECT/ORDER 표현식 생성
        DistinctQueryParts parts = buildDistinctQueryParts(
                column, safeOrderBy, typeMap, rawTemporalKindMap);

        // NOT NULL
        SqlDTO notNullCondition = SqlDTO.raw(parts.notNullExpr + " IS NOT NULL");
        SqlDTO baseWhere = SqlDTO.and(where, notNullCondition);

        // search prefix 적용
        if (search != null && !search.isBlank()) {
            SqlDTO searchCondition = SqlDTO.of(parts.selectExpr + " ILIKE ?", List.of(search + "%"));
            baseWhere = SqlDTO.and(baseWhere, searchCondition);
        }

        // Base CTE
        SqlQueryBuilder baseCte = SqlQueryBuilder
                .select(parts.selectExpr + " AS v", parts.orderExpr + " AS ord")
                .from(table + " t")
                .where(baseWhere);

        // Dedup CTE
        SqlQueryBuilder dedupCte = SqlQueryBuilder
                .select("v", "MIN(ord) AS first_ord")
                .from("base")
                .groupBy("v");

        // Main Query
        SqlQueryBuilder mainQuery = SqlQueryBuilder
                .select("v")
                .from("dedup")
                .orderBy("first_ord", safeOrder)
                .offset(offset)
                .limit(limit);

        // 전체 CTE 쿼리 빌드
        SqlDTO query = SqlQueryBuilder.cte()
                .with("base", baseCte)
                .with("dedup", dedupCte)
                .mainQuery(mainQuery)
                .build();

        log.info("[QueryBuilder] Distinct SQL: {} | args={}", query.getSql(), query.getArgs());
        return query;
    }

    /***************************************************************************
     *                     DISTINCT SELECT/ORDER EXPR Builder
     ***************************************************************************/
    private DistinctQueryParts buildDistinctQueryParts(
            String column, String orderBy,
            Map<String, String> typeMap, Map<String, String> rawTemporalKindMap) {

        String colType = optLower(typeMap, column);
        String rawKind = Optional.ofNullable(rawTemporalKindMap)
                .map(m -> m.get(column))
                .orElse("timestamp");

        // 날짜인 경우 KST 변환 + text 로 비교
        String selectExpr = "date".equals(colType)
                ? temporal.toDateExpr("t", column, rawKind) + "::text"
                : SqlIdentifier.quoteWithAlias("t", column) + "::text";

        // NOT NULL 판별용 원시 expr
        String notNullExpr = "date".equals(colType)
                ? temporal.toDateExpr("t", column, rawKind)
                : SqlIdentifier.quoteWithAlias("t", column);

        // ORDER BY expr
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

    /***************************************************************************
     *                     PUBLIC Helper Methods (다른 서비스에서 사용)
     ***************************************************************************/
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
