package com.moa.api.grid.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.moa.api.grid.dto.SqlDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.moa.api.grid.util.LayerTableResolver.resolveDataTable;
import static com.moa.api.grid.util.OrderBySanitizer.sanitizeDir;
import static com.moa.api.grid.util.OrderBySanitizer.sanitizeOrderBy;

/**
 * QueryBuilder: 읽기 쉬운 구조 + 파라미터 바인딩 중심으로 전면 재구성.
 * - JSON 필터 파싱 → Condition 리스트 → SQL/파라미터 생성으로 3단 분리
 * - 모든 값은 '?' 바인딩으로 처리 (IN 포함)
 * - 정렬/컬럼 검증, KST 변환, 베이스 스펙(시간/조건) 결합 유지
 */
@Slf4j
@Component
public class QueryBuilder {

    private final WhereBuilder whereBuilder;
    private final TemporalExprFactory temporal;

    public QueryBuilder(ObjectMapper mapper) {
        this.whereBuilder = new WhereBuilder(mapper);
        this.temporal = new TemporalExprFactory();
    }

    public SqlDTO buildSelectSQLForExport(
            String layer,
            java.util.List<String> columns,
            String sortField,
            String sortDirection,
            String filterModel,
            String baseSpecJson,
            java.util.Map<String, String> typeMap,
            java.util.Map<String, String> rawTemporalKindMap
    ) {
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("columns is required");
        }
        final String table = resolveTableName(layer);
        final String selectCols = String.join(", ", columns.stream()
                .map(SqlIdentifier::safeFieldName)
                .map(SqlIdentifier::quote)
                .toList());

        SqlDTO whereBase = whereBuilder.fromBaseSpec(baseSpecJson, typeMap, rawTemporalKindMap);
        SqlDTO whereFilter = whereBuilder.fromFilterModel(filterModel, typeMap, rawTemporalKindMap);
        SqlDTO where = SqlDTO.and(whereBase, whereFilter);

        String safeSort = sanitizeOrderBy(sortField, typeMap, "ts_server_nsec");
        String safeDir = sanitizeDir(sortDirection);

        StringBuilder sb = new StringBuilder();
        java.util.List<Object> args = new java.util.ArrayList<>();

        sb.append("SELECT ").append(selectCols)
                .append(" FROM ").append(table).append(" t");

        if (!where.isBlank()) {
            sb.append(" WHERE ").append(where.sql);
            args.addAll(where.args);
        }

        sb.append(" ORDER BY ");
        if ("ts_server_nsec".equalsIgnoreCase(safeSort)) {
            sb.append(SqlIdentifier.quote("ts_server_nsec")).append("::numeric ").append(safeDir);
        } else {
            sb.append(SqlIdentifier.quote(safeSort)).append(' ').append(safeDir);
        }

        String sql = sb.toString();
        log.info("[QueryBuilder] Export SQL: {} | args={}", sql, args);
        return new SqlDTO(sql, args);
    }

    public SqlDTO buildDistinctPagedSQLOrdered(
            String layer, String column, String filterModel, boolean includeSelf,
            String search, int offset, int limit,
            String orderBy, String order,
            String baseSpecJson,
            java.util.Map<String, String> typeMap,
            java.util.Map<String, String> rawTemporalKindMap
    ) {
        final String table = resolveTableName(layer);
        final String safeColumn = java.util.Objects.requireNonNull(column, "column");


        String safeOrderBy = (orderBy != null && typeMap != null && typeMap.containsKey(orderBy))
                ? orderBy : "ts_server_nsec";
        String safeOrder = sanitizeDir(order);


        SqlDTO whereBase = whereBuilder.fromBaseSpec(baseSpecJson, typeMap, rawTemporalKindMap);
        SqlDTO whereFilter = includeSelf
                ? whereBuilder.fromFilterModel(filterModel, typeMap, rawTemporalKindMap)
                : whereBuilder.fromFilterModelExcluding(filterModel, safeColumn, typeMap, rawTemporalKindMap);
        SqlDTO where = SqlDTO.and(whereBase, whereFilter);


        String colType = optLower(typeMap, safeColumn);
        String rawKind = Optional.ofNullable(rawTemporalKindMap).map(m -> m.get(safeColumn)).orElse("timestamp");


        String selectExpr = "date".equals(colType)
                ? temporal.toDateExpr("t", safeColumn, rawKind) + "::text"
                : SqlIdentifier.quoteWithAlias("t", safeColumn) + "::text";
        String notNullExpr = "date".equals(colType)
                ? temporal.toDateExpr("t", safeColumn, rawKind)
                : SqlIdentifier.quoteWithAlias("t", safeColumn);


        String orderExpr = SqlIdentifier.quoteWithAlias("t", safeOrderBy) + ("ts_server_nsec".equals(safeOrderBy) ? "::numeric" : "");


        java.util.List<Object> args = new java.util.ArrayList<>();
        StringBuilder sb = new StringBuilder();

        sb.append("WITH base AS (\n")
                .append(" SELECT ").append(selectExpr).append(" AS v, ")
                .append(orderExpr).append(" AS ord\n")
                .append(" FROM ").append(table).append(" t\n");

        if (!where.isBlank()) {
            sb.append(" WHERE ").append(where.sql).append(" AND ");
            args.addAll(where.args);
        } else {
            sb.append(" WHERE ");
        }
        sb.append(notNullExpr).append(" IS NOT NULL\n");

        if (search != null && !search.isBlank()) {
            sb.append(" AND ").append(selectExpr).append(" ILIKE ?\n");
            args.add(search + "%");
        }

        sb.append(")\n, dedup AS (\n")
                .append(" SELECT v, MIN(ord) AS first_ord FROM base GROUP BY v\n")
                .append(")\n")
                .append("SELECT v FROM dedup\n")
                .append("ORDER BY first_ord ").append(safeOrder).append(" \n")
                .append("OFFSET ").append(Math.max(0, offset)).append(' ')
                .append("LIMIT ").append(Math.max(1, limit));

        String sql = sb.toString();
        log.info("[QueryBuilder] Distinct SQL: {} | args={}", sql, args);
        return new SqlDTO(sql, args);
    }

    public String resolveTableName(String layer) {
        return resolveDataTable(layer);
    }

    private String optLower(Map<String, String> m, String key) {
        if (m == null) return "";
        String v = m.get(key);
        return v == null ? "" : v.toLowerCase();
    }

    public SqlDTO buildWhereFromBaseSpec(String baseSpecJson,
                                         Map<String, String> typeMap,
                                         Map<String, String> rawTemporalKindMap) {
        return whereBuilder.fromBaseSpec(baseSpecJson, typeMap, rawTemporalKindMap);
    }

    public SqlDTO buildWhereClause(String filterModel,
                                   Map<String, String> typeMap,
                                   Map<String, String> rawTemporalKindMap) {
        return whereBuilder.fromFilterModel(filterModel, typeMap, rawTemporalKindMap);
    }

    public SqlDTO buildWhereClauseExcludingField(String filterModel,
                                                 String excludeField,
                                                 Map<String, String> typeMap,
                                                 Map<String, String> rawTemporalKindMap) {
        return whereBuilder.fromFilterModelExcluding(filterModel, excludeField, typeMap, rawTemporalKindMap);
    }
}