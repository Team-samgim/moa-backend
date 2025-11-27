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
 *
 * AUTHOR        : 방대혁
 *
 * 역할:
 * - 그리드용 공통 Repository 구현체
 * - DISTINCT 필터 값 조회
 * - 컬럼 메타데이터 조회
 * - 집계(Aggregate) 쿼리 수행
 *
 * 특징:
 * - QueryBuilder / SqlDTO / SqlQueryBuilder 조합으로 동적 SQL 생성
 * - information_schema 및 메타 테이블 기반으로 타입/레이블 매핑
 * - 레이어별 컬럼 타입, 시계열 타입은 메모리 캐시 사용
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

    /**
     * 레이어별 프론트엔드 타입 캐시
     * - key: layer (소문자)
     * - value: { columnName -> frontendType(string/number/date/ip/mac/json/boolean) }
     */
    private final Map<String, Map<String, String>> frontendTypeCache = new ConcurrentHashMap<>();

    /**
     * 레이어별 시계열 원시 타입 캐시
     * - key: layer (소문자)
     * - value: { columnName -> timestamptz/timestamp/date/... }
     */
    private final Map<String, Map<String, String>> temporalKindCache = new ConcurrentHashMap<>();

    /**
     * DISTINCT 필터용 값 조회 (페이지 단위)
     *
     * - 다른 필터(filterModel, baseSpec)를 모두 반영한 상태에서
     *   지정된 컬럼의 DISTINCT 값을 페이지 단위로 조회
     * - limit+1 로 가져와서 hasMore 여부 판단 후 nextOffset 계산
     *
     * @param layer       레이어(http_page/http_uri/l4_tcp/ethernet)
     * @param column      DISTINCT 대상 컬럼
     * @param filterModel 활성 필터(JSON 문자열)
     * @param includeSelf (하위 호환, 현재는 사용하지 않음)
     * @param search      prefix 검색어
     * @param offset      페이지 offset
     * @param limit       페이지 크기
     * @param orderBy     정렬 컬럼
     * @param order       정렬 방향(ASC/DESC)
     * @param baseSpecJson SearchDTO 기반 baseSpec JSON
     */
    @Override
    public DistinctPageDTO getDistinctValuesPaged(
            String layer, String column, String filterModel,
            boolean includeSelf, String search, int offset, int limit,
            String orderBy, String order, String baseSpecJson) {

        // 레이어 기본값 보정
        String normLayer = (layer == null || layer.isBlank()) ? "ethernet" : layer;

        // 레이어별 타입/시계열 메타 조회 (캐시 활용)
        Map<String, String> typeMap = getFrontendTypeMap(normLayer);
        Map<String, String> temporalMap = getTemporalKindMap(normLayer);

        // DISTINCT + ORDER BY + LIMIT/OFFSET 동적 SQL 생성 (limit+1 로 한 페이지 초과 여부 체크)
        SqlDTO s = queryBuilder.buildDistinctPagedSQLOrdered(
                normLayer, column, filterModel, includeSelf, search, offset, limit + 1,
                orderBy, order, baseSpecJson, typeMap, temporalMap);

        log.debug("[DistinctSQL] {}", s.getSql());

        // 첫 번째 컬럼만 사용 (DISTINCT 대상)
        List<String> list = jdbcTemplate.query(s.getSql(), s.getArgs().toArray(), (rs, rn) -> {
            Object v = rs.getObject(1);
            return (v instanceof PGobject pg) ? pg.getValue() : (v == null ? null : v.toString());
        });

        // hasMore 판별을 위해 limit+1 개 가져옴
        boolean hasMore = list.size() > limit;
        if (hasMore) {
            // 초과분 1개 제거
            list.remove(list.size() - 1);
        }

        Integer next = hasMore ? offset + limit : null;
        // total 은 여기서는 계산하지 않고 0 으로 둔다 (필요하면 COUNT 쿼리 추가 가능)
        return new DistinctPageDTO(list, 0L, offset, limit, next);
    }

    /**
     * 컬럼 메타데이터 조회
     *
     * - information_schema.columns 에서 컬럼명/데이터 타입 조회
     * - 내부 규칙(mapToFrontendType)으로 프론트 타입 매핑
     * - 필드 메타 테이블(http_*_fields 등)의 label_ko 를 함께 매핑
     */
    @Override
    public List<SearchResponseDTO.ColumnDTO> getColumnsWithType(String layer) {
        String fqn = tableResolver.resolveDataTable(layer);
        String[] parts = tableResolver.splitSchemaAndTable(fqn);
        String schema = parts[0];
        String table = parts[1];

        // information_schema 기반 컬럼 메타 조회용 쿼리 (Builder 패턴)
        SqlDTO query = SqlQueryBuilder
                .select("column_name", "data_type", "udt_name")
                .from("information_schema.columns")
                .where(SqlDTO.of("table_schema = ? AND table_name = ?", List.of(schema, table)))
                .orderBy("ordinal_position", "ASC")
                .build();

        List<Map<String, Object>> result =
                jdbcTemplate.queryForList(query.getSql(), query.getArgs().toArray());

        // 설명 라벨 한국어 매핑 로드
        Map<String, String> labelMap = loadLabelKoMap(layer);

        List<SearchResponseDTO.ColumnDTO> columns = new ArrayList<>(result.size());
        for (Map<String, Object> row : result) {
            String name = String.valueOf(row.get("column_name"));
            String dataType = String.valueOf(row.get("data_type")).toLowerCase();
            String udtType = String.valueOf(row.get("udt_name")).toLowerCase();

            // 프론트에서 사용할 타입으로 매핑
            String mappedType = mapToFrontendType(udtType, dataType, name);
            String labelKo = labelMap.getOrDefault(name, null);

            columns.add(new SearchResponseDTO.ColumnDTO(name, mappedType, labelKo));
        }
        return columns;
    }

    /**
     * 집계(Aggregate) 수행
     *
     * - layer 별 테이블에서 baseSpec + filterModel 를 모두 반영한 후
     *   각 필드에 대해 요청된 연산(count/sum/avg/min/max/distinct/topN 등)을 수행
     * - number 타입과 문자열 계열 타입을 분리 처리
     */
    @Override
    public AggregateResponseDTO aggregate(AggregateRequestDTO req) {
        String layer = (req.getLayer() == null || req.getLayer().isBlank())
                ? "ethernet" : req.getLayer();
        String table = queryBuilder.resolveTableName(layer);

        Map<String, String> typeMap = getFrontendTypeMap(layer);
        Map<String, String> temporalMap = getTemporalKindMap(layer);

        // baseSpec 기반 WHERE
        SqlDTO whereBase = queryBuilder.buildWhereFromBaseSpec(
                req.getBaseSpecJson(), typeMap, temporalMap);

        // filterModel 기반 WHERE
        SqlDTO whereFilter = queryBuilder.buildWhereClause(
                req.getFilterModel(), typeMap, temporalMap);

        // 최종 WHERE (baseSpec AND filterModel)
        SqlDTO where = SqlDTO.and(whereBase, whereFilter);

        Map<String, Object> result = new LinkedHashMap<>();
        if (req.getMetrics() == null || req.getMetrics().isEmpty()) {
            return new AggregateResponseDTO(result);
        }

        // 필드별 집계 수행
        req.getMetrics().forEach((field, spec) -> {
            // 요청에 명시된 타입 우선, 없으면 메타 타입 사용
            String declaredType = spec != null ? spec.getType() : null;
            String t = (declaredType != null && !declaredType.isBlank())
                    ? declaredType.toLowerCase()
                    : typeMap.getOrDefault(field, "string").toLowerCase();

            // date 타입은 현재 스킵 (필요 시 추가 구현)
            if ("date".equals(t)) {
                return;
            }

            // 실제 컬럼명 (대소문자 보존을 위해 쌍따옴표 사용)
            String col = "\"" + field + "\"";
            List<String> ops = (spec != null && spec.getOps() != null)
                    ? spec.getOps()
                    : List.of();

            Map<String, Object> agg = new LinkedHashMap<>();

            try {
                if ("number".equals(t)) {
                    // 숫자 집계
                    Map<String, Object> row = numberAgg(table, col, where);
                    if (ops.contains("count")) agg.put("count", row.get("cnt"));
                    if (ops.contains("sum")) agg.put("sum", row.get("s"));
                    if (ops.contains("avg")) agg.put("avg", row.get("a"));
                    if (ops.contains("min")) agg.put("min", row.get("mn"));
                    if (ops.contains("max")) agg.put("max", row.get("mx"));
                } else {
                    // 문자열/JSON 계열 집계
                    boolean isJson = "json".equals(t);
                    Map<String, Object> row = stringAgg(table, col, where, isJson);
                    long cnt = row.get("cnt") == null
                            ? 0L
                            : ((Number) row.get("cnt")).longValue();
                    long uniq = row.get("uniq") == null
                            ? 0L
                            : ((Number) row.get("uniq")).longValue();

                    if (ops.contains("count")) {
                        agg.put("count", cnt);
                    }
                    if (ops.contains("distinct")) {
                        agg.put("distinct", uniq);
                    }

                    // topN 요청이 있고, 전체 건수 > 고유값 개수인 경우에만 상위 값 조회
                    boolean wantTop =
                            ops.contains("top1") || ops.contains("top2") || ops.contains("top3");
                    if (wantTop && cnt > uniq) {
                        List<Map<String, Object>> list =
                                topNList(table, col, where, 3, isJson);

                        java.util.function.Function<Object, Object> valOf = v -> {
                            if (v instanceof PGobject pg) {
                                return pg.getValue();
                            }
                            return v;
                        };

                        Map<String, Object> t1 = list.size() >= 1 ? list.get(0) : null;
                        Map<String, Object> t2 = list.size() >= 2 ? list.get(1) : null;
                        Map<String, Object> t3 = list.size() >= 3 ? list.get(2) : null;

                        if (ops.contains("top1")) {
                            agg.put("top1", t1 == null
                                    ? null
                                    : Map.of(
                                    "value", valOf.apply(t1.get("val")),
                                    "count", t1.get("c")
                            ));
                        }
                        if (ops.contains("top2")) {
                            agg.put("top2", t2 == null
                                    ? null
                                    : Map.of(
                                    "value", valOf.apply(t2.get("val")),
                                    "count", t2.get("c")
                            ));
                        }
                        if (ops.contains("top3")) {
                            agg.put("top3", t3 == null
                                    ? null
                                    : Map.of(
                                    "value", valOf.apply(t3.get("val")),
                                    "count", t3.get("c")
                            ));
                        }
                    }
                }
            } catch (Exception e) {
                // 특정 필드 집계가 실패해도 전체 응답은 내려가도록 swallow
                log.warn("[GridRepositoryImpl] aggregate error for field {}: {}", field, e.getMessage());
            }

            result.put(field, agg);
        });

        return new AggregateResponseDTO(result);
    }

    /**
     * 숫자 집계용 쿼리
     *
     * SELECT
     *   COUNT(col) AS cnt,
     *   SUM(col)   AS s,
     *   AVG(col)   AS a,
     *   MIN(col)   AS mn,
     *   MAX(col)   AS mx
     * FROM table t
     * WHERE ...
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
     * 문자열/JSON 계열 집계용 쿼리
     *
     * SELECT
     *   COUNT(col)                AS cnt,
     *   COUNT(DISTINCT expr)     AS uniq
     * FROM table t
     * WHERE ...
     *
     * @param distinctAsText true 인 경우 text 캐스팅 후 DISTINCT
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
     * 문자열/JSON 컬럼 Top-N 조회
     *
     * SELECT
     *   col::text AS val,
     *   COUNT(*)  AS c
     * FROM table t
     * WHERE ... AND NULLIF(BTRIM(col::text), '') IS NOT NULL
     * GROUP BY col::text
     * ORDER BY c DESC, val ASC
     * LIMIT n
     *
     * - 공백/빈 문자열은 제외
     */
    private List<Map<String, Object>> topNList(String table, String col, SqlDTO where, int limit, boolean distinctAsText) {
        String valExpr = col + "::text";
        // 값이 null 또는 공백 문자열인 경우 제외
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

    /**
     * 레이어별 프론트 타입 맵 조회
     * - 내부 캐시에 없으면 buildFrontendTypeMap 으로 생성
     */
    @Override
    public Map<String, String> getFrontendTypeMap(String layer) {
        String key = (layer == null ? "ethernet" : layer).toLowerCase();
        return frontendTypeCache.computeIfAbsent(key, this::buildFrontendTypeMap);
    }

    /**
     * 레이어별 시계열 원시 타입 맵 조회
     * - 내부 캐시에 없으면 buildTemporalKindMap 으로 생성
     */
    @Override
    public Map<String, String> getTemporalKindMap(String layer) {
        String key = (layer == null ? "ethernet" : layer).toLowerCase();
        return temporalKindCache.computeIfAbsent(key, this::buildTemporalKindMap);
    }

    /**
     * 프론트 타입 맵 빌드
     * - 컬럼 메타(getColumnsWithType)를 기반으로 name -> type 매핑 생성
     */
    private Map<String, String> buildFrontendTypeMap(String layer) {
        List<SearchResponseDTO.ColumnDTO> cols = getColumnsWithType(layer);
        Map<String, String> m = new HashMap<>(cols.size());
        for (var c : cols) {
            m.put(c.getName(), c.getType());
        }
        return m;
    }

    /**
     * 시계열 원시 타입 맵 빌드
     * - information_schema.columns 의 udt_name 기반
     * - timestamptz / timestamp / date / time 등을 구분
     */
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
            if (udt.contains("timestamptz")) {
                m.put(col, "timestamptz");
            } else if ("timestamp".equals(udt)) {
                m.put(col, "timestamp");
            } else if (udt.contains("date")) {
                m.put(col, "date");
            } else if (udt.contains("time")) {
                // time without time zone 등은 프론트 표현을 위해 timestamp 취급
                m.put(col, "timestamp");
            }
        }
        return m;
    }

    /**
     * DB 타입 + 컬럼명 기반 프론트 타입 매핑
     *
     * 규칙:
     * - inet/컬럼명에 ip 포함 → ip
     * - mac/컬럼명에 mac 포함 → mac
     * - 컬럼명에 port 포함 → string
     * - 문자 계열 → string
     * - 숫자 계열 → number
     * - 날짜/시간 계열 → date
     * - bool → boolean
     * - json → json
     * - 그 외 → string
     */
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

    /**
     * 필드 메타 테이블에서 한국어 라벨 로드
     *
     * - metaTable: http_page_fields / http_uri_fields / l4_tcp_fields / ethernet_fields ...
     * - 존재하지 않는 경우 빈 Map 반환
     */
    private Map<String, String> loadLabelKoMap(String layer) {
        String metaTable = tableResolver.resolveFieldMetaTable(layer);
        if (!tableExists("public", metaTable)) {
            return Map.of();
        }

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

    /**
     * 스키마/테이블 존재 여부 확인
     * - Postgres 의 to_regclass 사용
     */
    private boolean tableExists(String schema, String table) {
        String sql = "SELECT to_regclass(?)";
        String reg = jdbcTemplate.queryForObject(sql, String.class, schema + "." + table);
        return reg != null;
    }
}
