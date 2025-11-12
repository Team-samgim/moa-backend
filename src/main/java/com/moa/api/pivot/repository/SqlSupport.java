package com.moa.api.pivot.repository;

import com.moa.api.pivot.dto.PivotQueryRequestDTO;
import com.moa.api.pivot.exception.BadRequestException;
import com.moa.api.pivot.exception.ColumnNotAllowedException;
import com.moa.api.pivot.model.TimeWindow;
import com.moa.api.pivot.util.CursorCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class SqlSupport {

    private final SimpleColumnLoader columnLoader;
    private final NamedParameterJdbcTemplate jdbc;

    // layer -> table 매핑
    private static final Map<String, String> TABLE = Map.of(
            "HTTP_PAGE", "http_page_sample",
            "Ethernet",  "ethernet_sample",
            "TCP",       "tcp_sample",
            "HTTP_URI",  "http_uri_sample"
    );

    // layer마다 허용 컬럼 캐시
    private final Map<String, Set<String>> columnCache = new ConcurrentHashMap<>();

    // tableName -> (columnName -> data_type) 캐시
    private final Map<String, Map<String, String>> columnTypeCache = new ConcurrentHashMap<>();

    public String table(String layer) {
        String t = TABLE.get(layer);
        if (t == null) {
            throw new BadRequestException("Unsupported layer: " + layer);
        }
        return t;
    }

    /** 컬럼 화이트리스트 + 쿼트 + 타입 기반 캐스팅 */
    public String col(String layer, String raw) {
        if (raw == null || raw.isBlank() || raw.contains(".")) {
            throw new ColumnNotAllowedException(raw);
        }

        String tableName = table(layer);

        // 1) 화이트리스트 체크 (기존 그대로)
        Set<String> allowed = columnCache.computeIfAbsent(layer, l ->
                new HashSet<>(columnLoader.columns(table(l)))
        );

        if (!allowed.contains(raw)) {
            throw new ColumnNotAllowedException(raw);
        }

        String quoted = "\"" + raw + "\"";

        // 2) 컬럼 타입 조회 (information_schema 기반)
        String dataType = getColumnType(tableName, raw);

        // 3) macaddr 타입이면 문자열로 비교하기 위해 ::text 캐스팅
        if ("macaddr".equalsIgnoreCase(dataType)) {
            return quoted + "::text";
        }

        // 필요하면 inet, cidr 같은 것도 여기에서 추가 처리 가능
        // if ("inet".equalsIgnoreCase(dataType)) { ... }

        return quoted;
    }

    private String getColumnType(String tableName, String columnName) {
        Map<String, String> typeMap = columnTypeCache.computeIfAbsent(
                tableName,
                this::loadColumnTypes
        );
        return typeMap.get(columnName);
    }

    private Map<String, String> loadColumnTypes(String tableName) {
        String sql = """
            SELECT column_name, data_type
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = :tableName
        """;
        MapSqlParameterSource ps = new MapSqlParameterSource()
                .addValue("tableName", tableName);

        Map<String, String> map = new HashMap<>();
        jdbc.query(sql, ps, rs -> {
            String colName  = rs.getString("column_name");
            String dataType = rs.getString("data_type"); // ex) macaddr, character varying, integer ...
            map.put(colName, dataType);
        });
        return map;
    }

    /** created_at BETWEEN :from AND :to + filter 처리 */
    public String where(String layer,
                        String timeColumn,
                        TimeWindow tw,
                        List<PivotQueryRequestDTO.FilterDef> filters,
                        MapSqlParameterSource ps) {

        StringBuilder sb = new StringBuilder();
        String tcol = col(layer, timeColumn);

        sb.append(" WHERE ").append(tcol).append(" BETWEEN :from AND :to ");
        ps.addValue("from", tw.getFromEpoch());  // double
        ps.addValue("to", tw.getToEpoch());      // double

        if (filters == null || filters.isEmpty()) return sb.toString();

        int idx = 0;
        for (PivotQueryRequestDTO.FilterDef f : filters) {
            if (f == null || f.getField() == null || f.getField().isBlank()) continue;

            String c = col(layer, f.getField());
            String op = (f.getOp() == null ? "=" : f.getOp().trim().toUpperCase());
            Object v  = f.getValue();

            // IN / NOT IN
            if ("IN".equals(op) || "NOT IN".equals(op)) {
                if (!(v instanceof Collection<?> coll)) continue;

                List<Object> nonEmpty = new ArrayList<>();
                boolean hasEmpty = false;

                for (Object e : coll) {
                    String s = (e == null) ? null : String.valueOf(e);
                    if (s == null || s.isBlank() || "(empty)".equals(s)) {
                        hasEmpty = true;
                    } else {
                        nonEmpty.add(s);
                    }
                }

                if ("IN".equals(op)) {
                    List<String> parts = new ArrayList<>();
                    if (!nonEmpty.isEmpty()) {
                        String p = "f" + (idx++);
                        parts.add(c + " IN (:" + p + ")");
                        ps.addValue(p, nonEmpty);
                    }
                    if (hasEmpty) {
                        parts.add(c + " IS NULL OR " + c + " = ''");
                    }
                    if (!parts.isEmpty()) {
                        sb.append(" AND (").append(String.join(" OR ", parts)).append(")");
                    }
                } else { // NOT IN
                    List<String> parts = new ArrayList<>();
                    if (!nonEmpty.isEmpty()) {
                        String p = "f" + (idx++);
                        parts.add(c + " NOT IN (:" + p + ")");
                        ps.addValue(p, nonEmpty);
                    }
                    if (hasEmpty) {
                        parts.add(c + " IS NOT NULL AND " + c + " <> ''");
                    }
                    if (!parts.isEmpty()) {
                        sb.append(" AND (").append(String.join(" AND ", parts)).append(")");
                    }
                }
                continue;
            }

            // 단건 비교 (null / "(empty)" 처리)
            if (v == null || String.valueOf(v).isBlank() || "(empty)".equals(String.valueOf(v))) {
                if ("=".equals(op)) {
                    sb.append(" AND (").append(c).append(" IS NULL OR ").append(c).append(" = '' )");
                } else if ("!=".equals(op) || "<>".equals(op) || "NOT".equals(op)) {
                    sb.append(" AND (").append(c).append(" IS NOT NULL AND ").append(c).append(" <> '' )");
                } else {
                    sb.append(" AND ").append(c).append(" IS NULL");
                }
            } else {
                String p = "f" + (idx++);
                sb.append(" AND ").append(c).append(" ").append(op).append(" :").append(p);
                ps.addValue(p, v);
            }
        }

        return sb.toString();
    }

    /** keyset 커서 조건 추가 */
    public String appendCursorCondition(String layer,
                                        String field,
                                        String order,
                                        String whereSoFar,
                                        String cursorBase64,
                                        MapSqlParameterSource ps) {

        if (cursorBase64 == null || cursorBase64.isBlank()) return whereSoFar;

        String c = col(layer, field); // ex) "\"page_idx\"" 혹은 "\"mac\"::text"
        String ord = "DESC".equalsIgnoreCase(order) ? "DESC" : "ASC";
        String lastVal = CursorCodec.decode(cursorBase64);

        if (lastVal == null || lastVal.isBlank()) return whereSoFar;

        String cmp = ord.equals("ASC") ? " > " : " < ";

        String tableName = table(layer);               // ex) "http_page_sample"
        String dataType  = getColumnType(tableName, field); // ex) "bigint", "character varying", "macaddr", ...

        Object cursorValue;

        try {
            if ("bigint".equalsIgnoreCase(dataType)) {
                cursorValue = Long.valueOf(lastVal);
            } else if ("integer".equalsIgnoreCase(dataType) || "smallint".equalsIgnoreCase(dataType)) {
                cursorValue = Integer.valueOf(lastVal);
            } else if ("numeric".equalsIgnoreCase(dataType)
                    || "double precision".equalsIgnoreCase(dataType)
                    || "real".equalsIgnoreCase(dataType)) {
                cursorValue = new java.math.BigDecimal(lastVal);
            } else {
                // macaddr / character varying / text 등은 문자열 비교
                cursorValue = lastVal;
            }
        } catch (NumberFormatException ex) {
            // 혹시 디코딩 값이 숫자로 파싱 안 되면 그냥 문자열로 비교
            cursorValue = lastVal;
        }

        ps.addValue("__cursor_val", cursorValue);

        return whereSoFar + " AND " + c + cmp + " :__cursor_val ";
    }



}
