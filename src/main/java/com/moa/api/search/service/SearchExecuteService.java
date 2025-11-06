package com.moa.api.search.service;

import com.moa.api.search.dto.SearchDTO;
import com.moa.api.search.repository.HttpPageFieldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import com.moa.api.search.registry.DataType;
import com.moa.api.search.registry.OpCode;

@Service
@RequiredArgsConstructor
public class SearchExecuteService {

    private final NamedParameterJdbcTemplate jdbc;
    private final HttpPageFieldRepository fieldRepo;

    public SearchDTO execute(SearchDTO req) {
        System.out.println("[SearchExecuteService] execute 시작");

        // 1) 기본값/가드
        String layer = Optional.ofNullable(req.getLayer())
                .filter(StringUtils::hasText)
                .orElse("HTTP_PAGE");
        System.out.println("  - layer: " + layer);

        // layer 기반 테이블명 매핑
        String table = resolveTableFromLayer(layer);
        System.out.println("  - table: " + table);

        SearchDTO.TimeSpec time = Objects.requireNonNull(req.getTime(), "time is required");
        String timeField = Optional.ofNullable(time.getField()).filter(StringUtils::hasText).orElse("ts_server_nsec"); // ✅ 수정
        boolean inclusive = Optional.ofNullable(time.getInclusive()).orElse(true);

        System.out.println("  - timeField: " + timeField);
        System.out.println("  - fromEpoch: " + time.getFromEpoch());
        System.out.println("  - toEpoch: " + time.getToEpoch());
        System.out.println("  - columns: " + req.getColumns());
        System.out.println("  - conditions: " + req.getConditions());

        // 2) 필드/데이터타입 맵(화이트리스트)
        Map<String, String> fieldTypeMap = fieldRepo.findAll().stream()
                .collect(Collectors.toMap(f -> f.getFieldKey(), f -> f.getDataType(), (a, b)->a, LinkedHashMap::new));

        // 3) WHERE절 생성
        StringBuilder where = new StringBuilder();
        MapSqlParameterSource params = new MapSqlParameterSource();

        // 3-1) 기간 조건
        // 주의: ts_server_nsec 컬럼명이지만 실제로는 초(seconds) 단위로 저장됨
        if (time.getFromEpoch() != null) {
            where.append(where.length() == 0 ? " WHERE " : " AND ");
            where.append(" t.").append(safeColumn(timeField, fieldTypeMap));

            // 초 단위로 직접 비교 (나노초 변환 제거)
            where.append(inclusive ? " >= :fromEpoch " : " > :fromEpoch ");
            params.addValue("fromEpoch", time.getFromEpoch());
        }
        if (time.getToEpoch() != null) {
            where.append(where.length() == 0 ? " WHERE " : " AND ");
            where.append(" t.").append(safeColumn(timeField, fieldTypeMap));

            // 초 단위로 직접 비교 (나노초 변환 제거)
            where.append(inclusive ? " <= :toEpoch " : " < :toEpoch ");
            params.addValue("toEpoch", time.getToEpoch());
        }

        // 3-2) 조건들
        String condSql = buildConditionsSql(req, fieldTypeMap, params);
        if (StringUtils.hasText(condSql)) {
            // 전체 NOT은 기간 바깥에 두고 조건만 괄호로 묶어 반전
            where.append(where.length() == 0 ? " WHERE " : " AND ");
            if (req.getNot()) {
                where.append(" NOT ( ").append(condSql).append(" ) ");
            } else {
                where.append(" ( ").append(condSql).append(" ) ");
            }
        }

        // 4) ORDER/LIMIT
        String orderBy = Optional.ofNullable(req.getOptions()).map(SearchDTO.Options::getOrderBy).orElse("ts_server_nsec"); // ✅ 수정
        String order = Optional.ofNullable(req.getOptions()).map(SearchDTO.Options::getOrder).orElse("DESC");
        int limit = Optional.ofNullable(req.getOptions()).map(SearchDTO.Options::getLimit).orElse(100);
        limit = Math.max(1, Math.min(1000, limit));
        int offset = Optional.ofNullable(req.getOptions()).map(SearchDTO.Options::getOffset).orElse(0);
        offset = Math.max(0, offset);

        orderBy = safeColumn(orderBy, fieldTypeMap); // 화이트리스트
        order = "ASC".equalsIgnoreCase(order) ? "ASC" : "DESC";

        // SELECT 절 생성: columns가 있으면 명시적 선택, 없으면 *
        String selectClause;
        if (req.getColumns() != null && !req.getColumns().isEmpty()) {
            String cols = req.getColumns().stream()
                    .map(c -> "t." + safeColumn(c, fieldTypeMap))
                    .collect(Collectors.joining(", "));
            selectClause = "SELECT " + cols;
        } else {
            selectClause = "SELECT *";
        }

        String orderExpr = switch (fieldTypeMap.getOrDefault(orderBy, "TEXT").toUpperCase()) {
            case "NUMBER" -> "t." + orderBy + "::numeric";
            case "DATETIME" -> "t." + orderBy; // timestamp 계열은 그대로
            default -> "t." + orderBy + "::text"; // 문자열 계열은 명시적 캐스팅
        };

        String sql = selectClause + " FROM " + table + " t " +
                where +
                " ORDER BY " + orderExpr + " " + order +
                " LIMIT :limit OFFSET :offset";

        System.out.println("📝 [SearchExecuteService] 생성된 SQL:");
        System.out.println(sql);
        System.out.println("  - 파라미터: " + params.getValues());

        params.addValue("limit", limit);
        params.addValue("offset", offset);

        List<Map<String, Object>> rows = jdbc.queryForList(sql, params);

        System.out.println("[SearchExecuteService] 조회 결과: " + rows.size() + "건");
        if (!rows.isEmpty()) {
            System.out.println("  - 첫 번째 row: " + rows.get(0));
        }

        // (선택) 총 건수
        Integer total = null;
        try {
            String cntSql = "SELECT COUNT(*) FROM " + table + " t " + where;
            total = jdbc.queryForObject(cntSql, params, Integer.class);
        } catch (EmptyResultDataAccessException ignore) {}

        return new SearchDTO(rows, total);
    }

    /**
     * layer 값에 따라 테이블명 매핑
     */
    private String resolveTableFromLayer(String layer) {
        return switch (layer.toUpperCase(Locale.ROOT)) {
            case "HTTP_PAGE" -> "http_page_sample";
            case "HTTP_URI" -> "http_uri_sample";
            case "TCP" -> "tcp_sample";
            case "ETHERNET" -> "ethernet_sample";
            default -> throw new IllegalArgumentException("UNKNOWN_LAYER: " + layer);
        };
    }

    /** 조건들 SQL 생성 (join/템플릿/바인딩 처리) */
    private String buildConditionsSql(SearchDTO req,
                                      Map<String, String> fieldTypeMap,
                                      MapSqlParameterSource params) {
        if (req.getConditions() == null || req.getConditions().isEmpty()) return "";

        List<String> parts = new ArrayList<>();
        int idx = 0;
        for (SearchDTO.Condition c : req.getConditions()) {
            String field = c.getField();
            if (!StringUtils.hasText(field)) continue;
            String dataType = fieldTypeMap.get(field);
            if (dataType == null) {
                throw new IllegalArgumentException("INVALID_FIELD: " + field);
            }

            String op = c.getOp();
            if (!StringUtils.hasText(op)) continue;

            // op 허용 검증 및 템플릿 획득 (DB 대신 고정 레지스트리 사용)
            String tpl = templateOf(dataType, op); // 예: "${f} = :v1" / "${f} BETWEEN :v1 AND :v2" / "${f} IN (:list)" ...

            // 필드 바인딩
            String clause = tpl.replace("${f}", "t." + safeColumn(field, fieldTypeMap));

            // 파라미터 이름 유니크하게 치환
            List<Object> values = Optional.ofNullable(c.getValues()).orElse(List.of());
            if (clause.contains(":v1")) {
                String name = "v1_" + idx;
                clause = clause.replace(":v1", ":" + name);
                params.addValue(name, castValue(dataType, values.size() > 0 ? values.get(0) : null));
            }
            if (clause.contains(":v2")) {
                String name = "v2_" + idx;
                clause = clause.replace(":v2", ":" + name);
                params.addValue(name, castValue(dataType, values.size() > 1 ? values.get(1) : null));
            }
            if (clause.contains(":list")) {
                String name = "list_" + idx;
                clause = clause.replace(":list", ":" + name);
                List<Object> casted = values.stream().map(v -> castValue(dataType, v)).toList();
                params.addValue(name, casted);
            }

            // JOIN 처리
            String join = (idx == 0) ? null : Optional.ofNullable(c.getJoin()).orElse("AND");
            if (join != null) {
                parts.add(join.toUpperCase(Locale.ROOT));
            }
            parts.add("(" + clause + ")");
            idx++;
        }
        return String.join(" ", parts);
    }

    /** 고정 레지스트리: 데이터타입/연산자 조합 → SQL 템플릿 */
    private String templateOf(String dataType, String opRaw) {
        DataType dt = parseType(dataType);
        OpCode op = parseOp(opRaw);
        return switch (dt) {
            case TEXT -> textTpl(op);
            case NUMBER -> numberTpl(op);
            case IP -> ipTpl(op);
            case DATETIME -> datetimeTpl(op);
            case BOOLEAN -> booleanTpl(op);
        };
    }

    private DataType parseType(String raw) {
        if (raw == null) return DataType.TEXT;
        try { return DataType.valueOf(raw.trim().toUpperCase(Locale.ROOT)); }
        catch (Exception e) { return DataType.TEXT; }
    }

    private OpCode parseOp(String raw) {
        Objects.requireNonNull(raw, "op is required");
        try { return OpCode.valueOf(raw.trim().toUpperCase(Locale.ROOT)); }
        catch (Exception e) { throw new IllegalArgumentException("INVALID_OPERATOR: " + raw); }
    }

    // TEXT
    private String textTpl(OpCode op) {
        return switch (op) {
            case LIKE -> "${f} ILIKE '%' || :v1 || '%'";
            case EQ -> "${f} = :v1";
            case NE -> "${f} <> :v1";
            case STARTS_WITH -> "${f} ILIKE :v1 || '%'";
            case ENDS_WITH -> "${f} ILIKE '%' || :v1";
            case IN -> "${f} IN (:list)";
            case IS_NULL -> "${f} IS NULL";
            case IS_NOT_NULL -> "${f} IS NOT NULL";
            default -> throw new IllegalArgumentException("Unsupported TEXT op: " + op);
        };
    }

    // NUMBER
    private String numberTpl(OpCode op) {
        return switch (op) {
            case EQ -> "${f} = :v1";
            case NE -> "${f} <> :v1";
            case GT -> "${f} > :v1";
            case GTE -> "${f} >= :v1";
            case LT -> "${f} < :v1";
            case LTE -> "${f} <= :v1";
            case BETWEEN -> "${f} BETWEEN :v1 AND :v2";
            case IN -> "${f} IN (:list)";
            case IS_NULL -> "${f} IS NULL";
            case IS_NOT_NULL -> "${f} IS NOT NULL";
            default -> throw new IllegalArgumentException("Unsupported NUMBER op: " + op);
        };
    }

    // IP (PostgreSQL inet을 TEXT로 비교/목록; 필요 시 캐스팅 조정)
    private String ipTpl(OpCode op) {
        return switch (op) {
            case EQ -> "${f}::text = :v1";
            case LIKE -> "${f}::text ILIKE '%' || :v1 || '%'";
            case IN -> "${f}::text IN (:list)";
            case IS_NULL -> "${f} IS NULL";
            case IS_NOT_NULL -> "${f} IS NOT NULL";
            default -> throw new IllegalArgumentException("Unsupported IP op: " + op);
        };
    }

    // DATETIME (값은 epoch seconds 가정)
    private String datetimeTpl(OpCode op) {
        return switch (op) {
            case GTE -> "${f} >= to_timestamp(:v1)";
            case LT -> "${f} < to_timestamp(:v1)";
            case BETWEEN -> "${f} BETWEEN to_timestamp(:v1) AND to_timestamp(:v2)";
            case IN -> "${f} IN (SELECT to_timestamp(x) FROM unnest(:list) x)";
            case IS_NULL -> "${f} IS NULL";
            case IS_NOT_NULL -> "${f} IS NOT NULL";
            default -> throw new IllegalArgumentException("Unsupported DATETIME op: " + op);
        };
    }

    // BOOLEAN (프론트는 NULL 체크만 사용)
    private String booleanTpl(OpCode op) {
        return switch (op) {
            case IS_NULL -> "${f} IS NULL";
            case IS_NOT_NULL -> "${f} IS NOT NULL";
            default -> throw new IllegalArgumentException("Unsupported BOOLEAN op: " + op);
        };
    }

    /** 정렬/컬럼 화이트리스트 보호용 */
    private String safeColumn(String col, Map<String, String> fieldTypeMap) {
        if (!StringUtils.hasText(col)) throw new IllegalArgumentException("EMPTY_COLUMN");
        // 허용: 메타에 있는 컬럼만
        if (fieldTypeMap.containsKey(col)) return col;
        // 특별 허용(기간 컬럼 등 메타에 없을 수도 있을 때)
        if ("ts_server".equalsIgnoreCase(col) || "ts_server_nsec".equalsIgnoreCase(col)) return col;
        throw new IllegalArgumentException("FORBIDDEN_COLUMN: " + col);
    }

    /** 간단 캐스팅 (필요시 확장) */
    private Object castValue(String dataType, Object raw) {
        if (raw == null) return null;
        DataType dt = parseType(dataType);
        return switch (dt) {
            case NUMBER -> {
                if (raw instanceof Number) yield raw;
                try { yield Long.valueOf(raw.toString()); } catch (Exception e) { yield raw; }
            }
            case DATETIME -> {
                // epoch seconds 기대
                if (raw instanceof Number) yield raw;
                try { yield Long.valueOf(raw.toString()); } catch (Exception e) { yield raw; }
            }
            case BOOLEAN -> {
                if (raw instanceof Boolean) yield raw;
                yield Boolean.valueOf(raw.toString());
            }
            default -> raw.toString(); // TEXT, IP 등
        };
    }
}
