package com.moa.api.grid.repository;

import com.moa.api.grid.dto.AggregateRequestDTO;
import com.moa.api.grid.dto.AggregateResponseDTO;
import com.moa.api.grid.dto.DistinctPage;
import com.moa.api.grid.dto.SearchResponseDTO;
import com.moa.api.grid.util.QueryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

import static com.moa.api.grid.util.LayerTableResolver.resolveDataTable;

@Slf4j
@Repository
@RequiredArgsConstructor
public class GridRepositoryImpl implements GridRepositoryCustom {

    private final JdbcTemplate jdbcTemplate;
    private final QueryBuilder queryBuilder;

    @Override
    public DistinctPage getDistinctValuesPaged(
            String layer, String column, String filterModel,
            boolean includeSelf, String search, int offset, int limit,
            String orderBy, String order, String baseSpecJson) {

        Map<String, String> typeMap = buildFrontendTypeMap(layer);
        Map<String, String> temporalMap = buildTemporalKindMap(layer);

        String sql = queryBuilder.buildDistinctPagedSQLOrdered(
                layer, column, filterModel, includeSelf, search, offset, limit + 1,
                orderBy, order, baseSpecJson, typeMap, temporalMap);
        log.info("[DistinctSQL] {}", sql);

        List<String> list = jdbcTemplate.query(sql, (rs, rn) -> {
            Object v = rs.getObject(1);
            return (v instanceof PGobject pg) ? pg.getValue() : (v == null ? null : v.toString());
        });

        boolean hasMore = list.size() > limit;
        if (hasMore) list.remove(list.size() - 1); // 초과분 제거

        Integer next = hasMore ? offset + limit : null;

        long total = 0L;

        return new DistinctPage(list, total, offset, limit, next);
    }

    /**
     * PostgreSQL 컬럼명 + 타입 조회
     */
    public List<SearchResponseDTO.ColumnDTO> getColumnsWithType(String layer) {
        String fqn = resolveDataTable(layer); // e.g. public.http_page_sample
        String schema = fqn.contains(".") ? fqn.split("\\.", 2)[0] : "public";
        String table = fqn.contains(".") ? fqn.split("\\.", 2)[1] : fqn;

        String colSql = """
                    SELECT column_name, data_type, udt_name
                    FROM information_schema.columns
                    WHERE table_schema = ? AND table_name = ?
                    ORDER BY ordinal_position
                """;

        List<Map<String, Object>> result = jdbcTemplate.queryForList(colSql, schema, table);
        // 이하 동일

        Map<String, String> labelMap = loadLabelKoMap(layer);

        List<SearchResponseDTO.ColumnDTO> columns = new ArrayList<>();
        for (Map<String, Object> row : result) {
            String name = row.get("column_name").toString();
            String dataType = row.get("data_type").toString().toLowerCase();
            String udtType = row.get("udt_name").toString().toLowerCase();

            // DB 타입 → 표준화된 타입으로 매핑
            String mappedType = mapToFrontendType(udtType, dataType, name);
            String labelKo = labelMap.getOrDefault(name, null);

            columns.add(new SearchResponseDTO.ColumnDTO(name, mappedType, labelKo));
        }

        return columns;
    }

    /**
     * PostgreSQL 타입 → 프론트용 타입 매핑
     */
    private String mapToFrontendType(String udtType, String dataType, String colName) {
        String t = udtType != null ? udtType : dataType;
        t = t.toLowerCase();
        colName = colName.toLowerCase();

        // 문자열형
        if (t.contains("char") || t.contains("text") || t.contains("varchar")) return "string";

        // 숫자형
        if (t.contains("int") || t.contains("numeric") || t.contains("decimal")
                || t.contains("float") || t.contains("double") || t.contains("real")) return "number";

        // 날짜/시간형
        if (t.contains("date") || t.contains("time") || t.contains("timestamp")) return "date";

        // 불리언형
        if (t.contains("bool")) return "boolean";

        // 네트워크형
        if (t.contains("inet") || colName.contains("ip")) return "ip";
        if (t.contains("mac") || colName.contains("mac")) return "mac";

        // JSON형
        if (t.contains("json")) return "json";

        // 기본값
        return "string";
    }

    // 컬럼 -> 프론트타입(string/number/date/ip/mac...)
    private Map<String, String> buildFrontendTypeMap(String layer) {
        List<SearchResponseDTO.ColumnDTO> cols = getColumnsWithType(layer);
        Map<String, String> m = new HashMap<>();
        for (var c : cols) m.put(c.getName(), c.getType());
        return m;
    }

    private Map<String, String> buildTemporalKindMap(String layer) {
        String fqn = resolveDataTable(layer);
        String schema = fqn.contains(".") ? fqn.split("\\.", 2)[0] : "public";
        String table = fqn.contains(".") ? fqn.split("\\.", 2)[1] : fqn;

        String sql = """
                    SELECT column_name, udt_name
                    FROM information_schema.columns
                    WHERE table_schema = ? AND table_name = ?
                """;
        Map<String, String> m = new HashMap<>();
        for (var row : jdbcTemplate.queryForList(sql, schema, table)) {
            String col = row.get("column_name").toString();
            String udt = row.get("udt_name").toString().toLowerCase();
            if (udt.contains("timestamptz")) m.put(col, "timestamptz");
            else if ("timestamp".equals(udt)) m.put(col, "timestamp");
            else if (udt.contains("date")) m.put(col, "date");
            else if (udt.contains("time")) m.put(col, "timestamp");
        }
        return m;
    }

    public Map<String, String> getFrontendTypeMap(String layer) {
        return buildFrontendTypeMap(layer);
    }

    public Map<String, String> getTemporalKindMap(String layer) {
        return buildTemporalKindMap(layer);
    }

    public AggregateResponseDTO aggregate(AggregateRequestDTO req) {
        String layer = (req.getLayer() == null || req.getLayer().isBlank()) ? "ethernet" : req.getLayer();
        String table = queryBuilder.resolveTableName(layer);

        // 기존 WHERE/타입 판정 로직 재사용
        Map<String, String> typeMap = getFrontendTypeMap(layer);
        Map<String, String> temporalMap = getTemporalKindMap(layer);
        String whereBase = queryBuilder.buildWhereFromBaseSpec(req.getBaseSpecJson(), typeMap, temporalMap);
        String whereFilter = queryBuilder.buildWhereClause(req.getFilterModel(), typeMap, temporalMap);
        String where = java.util.stream.Stream.of(whereBase, whereFilter)
                .filter(s -> s != null && !s.isBlank())
                .collect(java.util.stream.Collectors.joining(" AND "));

        Map<String, Object> result = new LinkedHashMap<>();

        if (req.getMetrics() == null || req.getMetrics().isEmpty()) {
            return new AggregateResponseDTO(result);
        }

        req.getMetrics().forEach((field, spec) -> {
            String declaredType = spec != null ? spec.getType() : null;
            String t = (declaredType != null && !declaredType.isBlank())
                    ? declaredType.toLowerCase()
                    : typeMap.getOrDefault(field, "string").toLowerCase();

            // date는 제외
            if ("date".equals(t)) return;

            String col = "\"" + field + "\""; // 기존 SELECT와 동일 인용 규칙
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
                } else { // string / ip / mac / json
                    boolean isJson = "json".equals(t);   // json/jsonb 모두 mapToFrontendType에서 "json"으로 옴
                    Map<String, Object> row = stringAgg(table, col, where, isJson);
                    Object cntObj = row.get("cnt");
                    Object uniqObj = row.get("uniq");
                    long cnt = (cntObj == null) ? 0L : ((Number) cntObj).longValue();
                    long uniq = (uniqObj == null) ? 0L : ((Number) uniqObj).longValue();

                    if (ops.contains("count")) agg.put("count", cnt);
                    if (ops.contains("distinct")) agg.put("distinct", uniq);

                    boolean wantTop = ops.contains("top1") || ops.contains("top2") || ops.contains("top3");
                    // 전부 유니크면 Top 계산 스킵
                    if (wantTop && cnt > uniq) {
                        List<Map<String, Object>> list = topNList(table, col, where, 3, isJson);

                        java.util.function.Function<Object, Object> valOf = v -> {
                            if (v instanceof org.postgresql.util.PGobject pg) return pg.getValue();
                            return v;
                        };

                        Map<String, Object> t1 = list.size() >= 1 ? list.get(0) : null;
                        Map<String, Object> t2 = list.size() >= 2 ? list.get(1) : null;
                        Map<String, Object> t3 = list.size() >= 3 ? list.get(2) : null;

                        if (ops.contains("top1")) agg.put("top1", t1 == null ? null :
                                Map.of("value", valOf.apply(t1.get("val")), "count", t1.get("c")));
                        if (ops.contains("top2")) agg.put("top2", t2 == null ? null :
                                Map.of("value", valOf.apply(t2.get("val")), "count", t2.get("c")));
                        if (ops.contains("top3")) agg.put("top3", t3 == null ? null :
                                Map.of("value", valOf.apply(t3.get("val")), "count", t3.get("c")));
                    }
                }
            } catch (Exception e) {
                log.warn("[GridRepositoryImpl] aggregate error for field {}: {}", field, e.getMessage());
            }

            result.put(field, agg);
        });

        return new AggregateResponseDTO(result);
    }

    private Map<String, Object> numberAgg(String table, String col, String where) {
        StringBuilder sql = new StringBuilder()
                .append("SELECT COUNT(").append(col).append(") AS cnt, ")
                .append("SUM(").append(col).append(") AS s, ")
                .append("AVG(").append(col).append(") AS a, ")
                .append("MIN(").append(col).append(") AS mn, ")
                .append("MAX(").append(col).append(") AS mx ")
                .append("FROM ").append(table).append(" t");
        if (where != null && !where.isBlank()) sql.append(" WHERE ").append(where);
        return jdbcTemplate.queryForMap(sql.toString());
    }

    private Map<String, Object> stringAgg(String table, String col, String where, boolean distinctAsText) {
        String distinctExpr = distinctAsText ? (col + "::text") : col;
        StringBuilder sql = new StringBuilder()
                .append("SELECT COUNT(").append(col).append(") AS cnt, ")
                .append("COUNT(DISTINCT ").append(distinctExpr).append(") AS uniq ")
                .append("FROM ").append(table).append(" t");
        if (where != null && !where.isBlank()) sql.append(" WHERE ").append(where);
        return jdbcTemplate.queryForMap(sql.toString());
    }

    private List<Map<String, Object>> topNList(String table, String col, String where, int limit, boolean distinctAsText) {
        String valExpr = col + "::text"; // TopN은 텍스트 기준
        String notEmpty = "NULLIF(BTRIM(" + valExpr + "), '') IS NOT NULL";

        StringBuilder sql = new StringBuilder()
                .append("SELECT ").append(valExpr).append(" AS val, COUNT(*) AS c ")
                .append("FROM ").append(table).append(" t "); // ← 공백 포함

        List<String> conds = new ArrayList<>();
        if (where != null && !where.isBlank()) conds.add(where);
        conds.add(notEmpty); // NULL/빈문자/공백만 제외

        sql.append("WHERE ").append(String.join(" AND ", conds)).append(" ")
                .append("GROUP BY ").append(valExpr).append(" ")
                .append("ORDER BY c DESC, ").append(valExpr).append(" ASC ")
                .append("LIMIT ").append(Math.max(1, limit));

        return jdbcTemplate.queryForList(sql.toString());
    }

    /**
     * 레이어별 필드 메타 테이블명
     */
    private String resolveFieldMetaTable(String layer) {
        return switch (layer.toLowerCase()) {
            case "http_page" -> "http_page_fields";
            case "http_uri" -> "http_uri_fields";
            case "l4_tcp" -> "l4_tcp_fields";
            default -> "ethernet_fields";
        };
    }

    /**
     * 테이블 존재 여부
     */
    private boolean tableExists(String schema, String table) {
        String sql = """
                    SELECT to_regclass(?)
                """;
        String reg = jdbcTemplate.queryForObject(sql, String.class, schema + "." + table);
        return reg != null;
    }

    /**
     * field_key -> label_ko 매핑 로드
     */
    private Map<String, String> loadLabelKoMap(String layer) {
        String metaTable = resolveFieldMetaTable(layer);
        if (!tableExists("public", metaTable)) return Map.of();

        String sql = "SELECT field_key, label_ko FROM public." + metaTable;
        Map<String, String> m = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            m.put(rs.getString("field_key"), rs.getString("label_ko"));
        });
        return m;
    }
}
