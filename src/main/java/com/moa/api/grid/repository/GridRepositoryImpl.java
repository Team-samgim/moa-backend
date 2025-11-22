package com.moa.api.grid.repository;

import com.moa.api.grid.config.GridProperties;
import com.moa.api.grid.dto.*;
import com.moa.api.grid.util.LayerTableResolver;
import com.moa.api.grid.util.QueryBuilder;
import com.moa.api.grid.util.SqlQueryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GridRepositoryImpl
 */
@Slf4j
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GridRepositoryImpl implements GridRepositoryCustom {

    private final JdbcTemplate jdbcTemplate;
    private final QueryBuilder queryBuilder;
    private final LayerTableResolver tableResolver;
    private final GridProperties properties;

    // 레이어별 메타 캐시 (나중에 Redis로 전환)
    private final Map<String, Map<String, String>> frontendTypeCache = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> temporalKindCache = new ConcurrentHashMap<>();

    @Override
    public DistinctPageDTO getDistinctValuesPaged(
            String layer, String column, String filterModel,
            boolean includeSelf, String search, int offset, int limit,
            String orderBy, String order, String baseSpecJson) {

        String normLayer = (layer == null || layer.isBlank()) ? "ethernet" : layer;
        Map<String, String> typeMap = getFrontendTypeMap(normLayer);
        Map<String, String> temporalMap = getTemporalKindMap(normLayer);

        SqlDTO s = queryBuilder.buildDistinctPagedSQLOrdered(
                normLayer, column, filterModel, includeSelf, search, offset, limit + 1,
                orderBy, order, baseSpecJson, typeMap, temporalMap);

        log.debug("[DistinctSQL] {}", s.getSql());

        List<String> list = jdbcTemplate.query(s.getSql(), s.getArgs().toArray(), (rs, rn) -> {
            Object v = rs.getObject(1);
            return (v instanceof PGobject pg) ? pg.getValue() : (v == null ? null : v.toString());
        });

        boolean hasMore = list.size() > limit;
        if (hasMore) list.remove(list.size() - 1);

        Integer next = hasMore ? offset + limit : null;
        return new DistinctPageDTO(list, 0L, offset, limit, next);
    }

    @Override
    public List<SearchResponseDTO.ColumnDTO> getColumnsWithType(String layer) {
        String fqn = tableResolver.resolveDataTable(layer);
        String[] parts = tableResolver.splitSchemaAndTable(fqn);
        String schema = parts[0];
        String table = parts[1];

        // Builder 패턴 활용!
        SqlDTO query = SqlQueryBuilder
                .select("column_name", "data_type", "udt_name")
                .from("information_schema.columns")
                .where(SqlDTO.of("table_schema = ? AND table_name = ?", List.of(schema, table)))
                .orderBy("ordinal_position", "ASC")
                .build();

        List<Map<String, Object>> result = jdbcTemplate.queryForList(query.getSql(), query.getArgs().toArray());
        Map<String, String> labelMap = loadLabelKoMap(layer);

        List<SearchResponseDTO.ColumnDTO> columns = new ArrayList<>(result.size());
        for (Map<String, Object> row : result) {
            String name = String.valueOf(row.get("column_name"));
            String dataType = String.valueOf(row.get("data_type")).toLowerCase();
            String udtType = String.valueOf(row.get("udt_name")).toLowerCase();

            String mappedType = mapToFrontendType(udtType, dataType, name);
            String labelKo = labelMap.getOrDefault(name, null);
            columns.add(new SearchResponseDTO.ColumnDTO(name, mappedType, labelKo));
        }
        return columns;
    }

    @Override
    public AggregateResponseDTO aggregate(AggregateRequestDTO req) {
        String layer = (req.getLayer() == null || req.getLayer().isBlank())
                ? "ethernet" : req.getLayer();
        String table = queryBuilder.resolveTableName(layer);

        Map<String, String> typeMap = getFrontendTypeMap(layer);
        Map<String, String> temporalMap = getTemporalKindMap(layer);

        SqlDTO whereBase = queryBuilder.buildWhereFromBaseSpec(req.getBaseSpecJson(), typeMap, temporalMap);
        SqlDTO whereFilter = queryBuilder.buildWhereClause(req.getFilterModel(), typeMap, temporalMap);
        SqlDTO where = SqlDTO.and(whereBase, whereFilter);

        Map<String, Object> result = new LinkedHashMap<>();
        if (req.getMetrics() == null || req.getMetrics().isEmpty()) {
            return new AggregateResponseDTO(result);
        }

        req.getMetrics().forEach((field, spec) -> {
            String declaredType = spec != null ? spec.getType() : null;
            String t = (declaredType != null && !declaredType.isBlank())
                    ? declaredType.toLowerCase()
                    : typeMap.getOrDefault(field, "string").toLowerCase();

            if ("date".equals(t)) return;

            String col = "\"" + field + "\"";
            List<String> ops = (spec != null && spec.getOps() != null) ? spec.getOps() : List.of();

            Map<String, Object> agg = new LinkedHashMap<>();

            try {
                if ("number".equals(t)) {
                    Map<String, Object> row = numberAgg(table, col, where);
                    if (ops.contains("count")) agg.put("count", row.get("cnt"));
                    if (ops.contains("sum")) agg.put("sum", row.get("s"));
                    if (ops.contains("avg")) agg.put("avg", row.get("a"));
                    if (ops.contains("min")) agg.put("min", row.get("mn"));
                    if (ops.contains("max")) agg.put("max", row.get("mx"));
                } else {
                    boolean isJson = "json".equals(t);
                    Map<String, Object> row = stringAgg(table, col, where, isJson);
                    long cnt = row.get("cnt") == null ? 0L : ((Number) row.get("cnt")).longValue();
                    long uniq = row.get("uniq") == null ? 0L : ((Number) row.get("uniq")).longValue();

                    if (ops.contains("count")) agg.put("count", cnt);
                    if (ops.contains("distinct")) agg.put("distinct", uniq);

                    boolean wantTop = ops.contains("top1") || ops.contains("top2") || ops.contains("top3");
                    if (wantTop && cnt > uniq) {
                        List<Map<String, Object>> list = topNList(table, col, where, 3, isJson);

                        java.util.function.Function<Object, Object> valOf = v -> {
                            if (v instanceof PGobject pg) return pg.getValue();
                            return v;
                        };

                        Map<String, Object> t1 = list.size() >= 1 ? list.get(0) : null;
                        Map<String, Object> t2 = list.size() >= 2 ? list.get(1) : null;
                        Map<String, Object> t3 = list.size() >= 3 ? list.get(2) : null;

                        if (ops.contains("top1"))
                            agg.put("top1", t1 == null ? null : Map.of("value", valOf.apply(t1.get("val")), "count", t1.get("c")));
                        if (ops.contains("top2"))
                            agg.put("top2", t2 == null ? null : Map.of("value", valOf.apply(t2.get("val")), "count", t2.get("c")));
                        if (ops.contains("top3"))
                            agg.put("top3", t3 == null ? null : Map.of("value", valOf.apply(t3.get("val")), "count", t3.get("c")));
                    }
                }
            } catch (Exception e) {
                log.warn("[GridRepositoryImpl] aggregate error for field {}: {}", field, e.getMessage());
            }

            result.put(field, agg);
        });

        return new AggregateResponseDTO(result);
    }

    /**
     * 숫자 집계 - Builder 패턴 적용
     */
    private Map<String, Object> numberAgg(String table, String col, SqlDTO where) {
        SqlDTO query = SqlQueryBuilder
                .select(
                        "COUNT(" + col + ") AS cnt",
                        "SUM(" + col + ") AS s",
                        "AVG(" + col + ") AS a",
                        "MIN(" + col + ") AS mn",
                        "MAX(" + col + ") AS mx"
                )
                .from(table + " t")
                .where(where)
                .build();

        return jdbcTemplate.queryForMap(query.getSql(), query.getArgs().toArray());
    }

    /**
     * 문자열 집계 - Builder 패턴 적용
     */
    private Map<String, Object> stringAgg(String table, String col, SqlDTO where, boolean distinctAsText) {
        String distinctExpr = distinctAsText ? (col + "::text") : col;

        SqlDTO query = SqlQueryBuilder
                .select(
                        "COUNT(" + col + ") AS cnt",
                        "COUNT(DISTINCT " + distinctExpr + ") AS uniq"
                )
                .from(table + " t")
                .where(where)
                .build();

        return jdbcTemplate.queryForMap(query.getSql(), query.getArgs().toArray());
    }

    /**
     * Top-N 조회 - Builder 패턴 적용
     */
    private List<Map<String, Object>> topNList(String table, String col, SqlDTO where, int limit, boolean distinctAsText) {
        String valExpr = col + "::text";
        SqlDTO notEmptyCondition = SqlDTO.raw("NULLIF(BTRIM(" + valExpr + "), '') IS NOT NULL");
        SqlDTO combinedWhere = SqlDTO.and(where, notEmptyCondition);

        SqlDTO query = SqlQueryBuilder
                .select(valExpr + " AS val", "COUNT(*) AS c")
                .from(table + " t")
                .where(combinedWhere)
                .groupBy(valExpr)
                .orderBy("c", "DESC")
                .orderBy(valExpr, "ASC")
                .limit(limit)
                .build();

        return jdbcTemplate.queryForList(query.getSql(), query.getArgs().toArray());
    }

    @Override
    public Map<String, String> getFrontendTypeMap(String layer) {
        String key = (layer == null ? "ethernet" : layer).toLowerCase();
        return frontendTypeCache.computeIfAbsent(key, this::buildFrontendTypeMap);
    }

    @Override
    public Map<String, String> getTemporalKindMap(String layer) {
        String key = (layer == null ? "ethernet" : layer).toLowerCase();
        return temporalKindCache.computeIfAbsent(key, this::buildTemporalKindMap);
    }

    private Map<String, String> buildFrontendTypeMap(String layer) {
        List<SearchResponseDTO.ColumnDTO> cols = getColumnsWithType(layer);
        Map<String, String> m = new HashMap<>(cols.size());
        for (var c : cols) m.put(c.getName(), c.getType());
        return m;
    }

    private Map<String, String> buildTemporalKindMap(String layer) {
        String fqn = tableResolver.resolveDataTable(layer);
        String[] parts = tableResolver.splitSchemaAndTable(fqn);
        String schema = parts[0];
        String table = parts[1];

        SqlDTO query = SqlQueryBuilder
                .select("column_name", "udt_name")
                .from("information_schema.columns")
                .where(SqlDTO.of("table_schema = ? AND table_name = ?", List.of(schema, table)))
                .build();

        Map<String, String> m = new HashMap<>();
        for (var row : jdbcTemplate.queryForList(query.getSql(), query.getArgs().toArray())) {
            String col = String.valueOf(row.get("column_name"));
            String udt = String.valueOf(row.get("udt_name")).toLowerCase();
            if (udt.contains("timestamptz")) m.put(col, "timestamptz");
            else if ("timestamp".equals(udt)) m.put(col, "timestamp");
            else if (udt.contains("date")) m.put(col, "date");
            else if (udt.contains("time")) m.put(col, "timestamp");
        }
        return m;
    }

    private String mapToFrontendType(String udtType, String dataType, String colName) {
        String t = (udtType != null ? udtType : dataType).toLowerCase();
        String c = colName.toLowerCase();

        if (t.contains("inet") || c.contains("ip")) return "ip";
        if (t.contains("mac") || c.contains("mac")) return "mac";
        if (c.contains("port")) return "string";
        if (t.contains("char") || t.contains("text") || t.contains("varchar")) return "string";
        if (t.contains("int") || t.contains("numeric") || t.contains("decimal") ||
                t.contains("float") || t.contains("double") || t.contains("real")) return "number";
        if (t.contains("date") || t.contains("time") || t.contains("timestamp")) return "date";
        if (t.contains("bool")) return "boolean";
        if (t.contains("json")) return "json";

        return "string";
    }

    private Map<String, String> loadLabelKoMap(String layer) {
        String metaTable = tableResolver.resolveFieldMetaTable(layer);
        if (!tableExists("public", metaTable)) return Map.of();

        SqlDTO query = SqlQueryBuilder
                .select("field_key", "label_ko")
                .from("public." + metaTable)
                .build();

        Map<String, String> m = new HashMap<>();
        jdbcTemplate.query(query.getSql(), rs -> {
            while (rs.next()) {
                m.put(rs.getString("field_key"), rs.getString("label_ko"));
            }
            return null;
        });
        return m;
    }

    private boolean tableExists(String schema, String table) {
        String sql = "SELECT to_regclass(?)";
        String reg = jdbcTemplate.queryForObject(sql, String.class, schema + "." + table);
        return reg != null;
    }
}