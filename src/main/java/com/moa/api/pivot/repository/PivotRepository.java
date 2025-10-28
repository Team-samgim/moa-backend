package com.moa.api.pivot.repository;

import com.moa.api.pivot.dto.PivotQueryRequestDTO;
import com.moa.api.pivot.dto.PivotQueryResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class PivotRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private String resolveTable(String layer) {
        return switch (layer) {
            case "HTTP_PAGE" -> "page_sample";
            case "ETHERNET"  -> "ethernet_sample";
            case "TCP"       -> "tcp_sample";
            case "HTTP_URI"  -> "uri_sample";
            default -> throw new IllegalArgumentException("Unsupported layer: " + layer);
        };
    }

    public List<String> findColumnNamesForLayer(String layer) {
        String tableName = resolveTable(layer);

        String sql = """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = :tableName
            ORDER BY ordinal_position
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("tableName", tableName);

        return jdbc.query(
                sql,
                params,
                (rs, i) -> rs.getString("column_name")
        );
    }

    // 공통 WHERE 절 빌더
    private String buildWhereClause(
            String timeColumn,
            TimeWindow tw,
            List<PivotQueryRequestDTO.FilterDef> filters,
            MapSqlParameterSource paramsOut
    ) {
        StringBuilder where = new StringBuilder();
        where.append(" WHERE ").append(timeColumn).append(" BETWEEN :from AND :to ");

        paramsOut.addValue("from", tw.from); // LocalDateTime
        paramsOut.addValue("to", tw.to);     // LocalDateTime

        if (filters != null) {
            int idx = 0;
            for (PivotQueryRequestDTO.FilterDef f : filters) {
                String paramName = "f" + idx;
                where.append(" AND ").append(f.getField()).append(" ")
                        .append(f.getOp()).append(" :").append(paramName).append(" ");
                paramsOut.addValue(paramName, f.getValue());
                idx++;
            }
        }

        return where.toString();
    }

    // 2) column 축으로 쓰일 상위 distinct 값 구하기
    public List<String> findTopColumnValues(
            String layer,
            String columnField,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw
    ) {
        String tableName = resolveTable(layer);

        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = buildWhereClause("created_at", tw, filters, params);

        String sql = """
            SELECT %s AS col_val, COUNT(*) AS cnt
            FROM %s
            %s
            GROUP BY %s
            ORDER BY cnt DESC
            LIMIT 10
        """.formatted(columnField, tableName, where, columnField);

        return jdbc.query(sql, params, (rs, i) -> rs.getString("col_val"));
    }

    // 3-a) rowField 전체 요약 (rowField로는 group하지 않고 columnField로만 group)
    //     -> RowGroup.cells 에 들어갈 맵
    private Map<String, Map<String, Object>> fetchSummaryByColumn(
            String layer,
            String rowField,
            String columnField,
            List<String> columnValues,
            List<PivotQueryRequestDTO.ValueDef> metrics,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw
    ) {
        String tableName = resolveTable(layer);

        // SELECT 절 구성: SUM(field) AS "alias", ...
        StringBuilder aggSelect = new StringBuilder();
        for (int i = 0; i < metrics.size(); i++) {
            var m = metrics.get(i);
            if (i > 0) aggSelect.append(", ");
            aggSelect.append(m.getAgg().toUpperCase())
                    .append("(").append(m.getField()).append(")")
                    .append(" AS \"").append(m.getAlias()).append("\"");
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = buildWhereClause("created_at", tw, filters, params);

        String sql = """
            SELECT %s AS col_val,
                   %s
            FROM %s
            %s
            GROUP BY %s
        """.formatted(columnField, aggSelect, tableName, where, columnField);

        Map<String, Map<String, Object>> result = new LinkedHashMap<>();

        jdbc.query(sql, params, rs -> {
            String colVal = rs.getString("col_val");
            if (!columnValues.contains(colVal)) {
                return;
            }

            Map<String, Object> metricMap = new LinkedHashMap<>();
            for (PivotQueryRequestDTO.ValueDef m : metrics) {
                Object v = rs.getObject(m.getAlias());
                metricMap.put(m.getAlias(), v);
            }
            result.put(colVal, metricMap);
        });

        for (String colVal : columnValues) {
            result.putIfAbsent(colVal, new LinkedHashMap<>());
        }

        return result;
    }

    // 3-b) rowField 값별 + columnField 값별로 집계
    private Map<String /*row_val*/, Map<String /*col_val*/, Map<String,Object>>> fetchBreakdownByRowAndColumn(
            String layer,
            String rowField,
            String columnField,
            List<PivotQueryRequestDTO.ValueDef> metrics,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw
    ) {
        String tableName = resolveTable(layer);

        StringBuilder aggSelect = new StringBuilder();
        for (int i = 0; i < metrics.size(); i++) {
            var m = metrics.get(i);
            if (i > 0) aggSelect.append(", ");
            aggSelect.append(m.getAgg().toUpperCase())
                    .append("(").append(m.getField()).append(")")
                    .append(" AS \"").append(m.getAlias()).append("\"");
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = buildWhereClause("created_at", tw, filters, params);

        String sql = """
            SELECT %s AS row_val,
                   %s AS col_val,
                   %s
            FROM %s
            %s
            GROUP BY %s, %s
        """.formatted(rowField, columnField, aggSelect, tableName, where, rowField, columnField);

        Map<String, Map<String, Map<String, Object>>> tmp = new LinkedHashMap<>();

        jdbc.query(sql, params, rs -> {
            String rowVal = rs.getString("row_val");
            String colVal = rs.getString("col_val");

            Map<String, Object> metricMap = new LinkedHashMap<>();
            for (PivotQueryRequestDTO.ValueDef m : metrics) {
                Object v = rs.getObject(m.getAlias());
                metricMap.put(m.getAlias(), v);
            }

            tmp.computeIfAbsent(rowVal, k -> new LinkedHashMap<>())
                    .put(colVal, metricMap);
        });

        // 반환 형태에 맞게 캐스팅
        Map<String, Map<String, Map<String,Object>>> casted = tmp;
        return (Map) casted;
    }

    private List<String> fetchDistinctRowValues(
            String layer,
            String rowField,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw
    ) {
        String tableName = resolveTable(layer);

        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = buildWhereClause("created_at", tw, filters, params);

        String sql = """
            SELECT %s AS row_val,
                   COUNT(*) AS cnt
            FROM %s
            %s
            GROUP BY %s
            ORDER BY cnt DESC
        """.formatted(rowField, tableName, where, rowField);

        return jdbc.query(sql, params, (rs, i) -> rs.getString("row_val"));
    }

    public List<PivotQueryResponseDTO.RowGroup> buildRowGroups(
            String layer,
            List<PivotQueryRequestDTO.RowDef> rows,   // ex: [{field:"vlan_id"}, {field:"country_name_res"}]
            List<PivotQueryRequestDTO.ValueDef> values, // metrics
            String columnField,                      // ex: "src_port"
            List<String> columnValues,               // ex: ["ip_num_1","ip_num_2"]
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw
    ) {

        List<PivotQueryResponseDTO.RowGroup> groups = new ArrayList<>();

        for (PivotQueryRequestDTO.RowDef rowDef : rows) {

            String rowField = rowDef.getField();

            // 1) rowField의 distinct 값 목록
            List<String> distinctRowVals = fetchDistinctRowValues(layer, rowField, filters, tw);
            int distinctCount = distinctRowVals.size();

            // 2) 상위 요약(그룹 전체) -> column별 agg
            Map<String, Map<String, Object>> summaryCells =
                    fetchSummaryByColumn(layer, rowField, columnField, columnValues, values, filters, tw);

            // 3) row별 breakdown (row_val + column_val 별 agg)
            Map<String, Map<String, Map<String, Object>>> breakdown =
                    fetchBreakdownByRowAndColumn(layer, rowField, columnField, values, filters, tw);

            // 4) RowGroup.items 만들기
            List<PivotQueryResponseDTO.RowGroupItem> items = new ArrayList<>();
            for (String rowVal : distinctRowVals) {

                Map<String, Map<String, Object>> childCells = new LinkedHashMap<>();

                Map<String, Map<String, Object>> byColForThisRowVal = breakdown.getOrDefault(rowVal, Map.of());

                for (String colVal : columnValues) {
                    Map<String, Object> metricMap = byColForThisRowVal.getOrDefault(colVal, Map.of());
                    childCells.put(colVal, metricMap);
                }

                items.add(
                        PivotQueryResponseDTO.RowGroupItem.builder()
                                .valueLabel(rowVal)
                                .displayLabel(rowVal)
                                .cells(childCells)
                                .build()
                );
            }

            // 5) RowGroup 자체 구성
            PivotQueryResponseDTO.RowGroup group =
                    PivotQueryResponseDTO.RowGroup.builder()
                            .rowLabel(rowField)
                            .displayLabel(rowField + " (" + distinctCount + ")")
                            .rowInfo(
                                    PivotQueryResponseDTO.RowInfo.builder()
                                            .count(distinctCount)
                                            .build()
                            )
                            .cells(summaryCells)
                            .items(items)
                            .build();

            groups.add(group);
        }

        return groups;
    }

    // 조회 범위
    public static class TimeWindow {
        public final LocalDateTime from;
        public final LocalDateTime to;

        public TimeWindow(LocalDateTime from, LocalDateTime to) {
            this.from = from;
            this.to = to;
        }
    }
}
