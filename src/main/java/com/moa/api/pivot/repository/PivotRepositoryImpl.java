package com.moa.api.pivot.repository;

import com.moa.api.pivot.dto.*;
import com.moa.api.pivot.exception.BadRequestException;
import com.moa.api.pivot.model.PivotFieldMeta;
import com.moa.api.pivot.model.TimeWindow;
import com.moa.api.pivot.util.CursorCodec;
import com.moa.api.pivot.util.ValueUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PivotRepositoryImpl implements PivotRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final SqlSupport sql;

    private String resolveTable(String layer) {
        return switch (layer) {
            case "HTTP_PAGE" -> "http_page_sample";
            case "Ethernet"  -> "ethernet_sample";
            case "TCP"       -> "tcp_sample";
            case "HTTP_URI"  -> "http_uri_sample";
            default -> throw new IllegalArgumentException("Unsupported layer: " + layer);
        };
    }

    @Override
    public List<PivotFieldMeta> findFieldMetaForLayer(String layer) {
        String tableName = resolveFieldsTable(layer); // *_fields 테이블

        String sql = """
        SELECT field_key, data_type, label_ko
        FROM %s
        ORDER BY field_key
        """.formatted(tableName);

        // tableName은 우리가 switch로 화이트리스트 관리하니까 문자열 붙여도 괜찮아
        return jdbc.query(
                sql,
                (rs, i) -> new PivotFieldMeta(
                        rs.getString("field_key"),
                        rs.getString("data_type"),
                        rs.getString("label_ko")
                )
        );
    }

    private String resolveFieldsTable(String layer) {
        return switch (layer) {
            case "HTTP_PAGE" -> "http_page_fields";
            case "Ethernet"  -> "ethernet_fields";
            case "TCP"       -> "tcp_fields";
            case "HTTP_URI"  -> "http_uri_fields";
            default -> throw new IllegalArgumentException("Unsupported layer: " + layer);
        };
    }

    /* ========= 1) 필드 값 페이지네이션 ========= */

    @Override
    public DistinctValuesPageDTO pageDistinctValues(DistinctValuesRequestDTO req, TimeWindow tw) {
        String table = sql.table(req.getLayer());
        String col   = sql.col(req.getLayer(), req.getField());

        MapSqlParameterSource ps = new MapSqlParameterSource();

        // 기본 시간 컬럼: ts_server_nsec
        String timeField = "ts_server_nsec";
        if (req.getTime() != null && req.getTime().getField() != null &&
                !req.getTime().getField().isBlank()) {
            timeField = req.getTime().getField();
        }

        String where = sql.where(req.getLayer(), timeField, tw, req.getFilters(), ps);

        // 검색어
        if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
            where += " AND " + col + " ILIKE :kw ";
            ps.addValue("kw", "%" + req.getKeyword() + "%");
        }

        String whereForCount = where;  // 커서 없음
        String whereForPage  = where;  // 커서 들어감

        // 커서 조건 (페이지 조회용에만 적용)
        if (req.getCursor() != null && !req.getCursor().isBlank()) {
            whereForPage = sql.appendCursorCondition(
                    req.getLayer(),
                    req.getField(),
                    req.getOrder(),
                    whereForPage,
                    req.getCursor(),
                    ps
            );
        }

        String ord = "DESC".equalsIgnoreCase(req.getOrder()) ? "DESC" : "ASC";
        int limit  = Math.min(req.getLimit() != null ? req.getLimit() : 50, 200);

        String countSql = """
            SELECT COUNT(DISTINCT %s)
            FROM %s
            %s
        """.formatted(col, table, whereForCount);

        Integer totalCount = jdbc.queryForObject(countSql, ps, Integer.class);

        String text = """
            SELECT DISTINCT %s AS val
            FROM %s
            %s
            ORDER BY %s %s NULLS LAST
            LIMIT :lim
        """.formatted(col, table, whereForPage, col, ord);
        ps.addValue("lim", limit);

        List<String> items = jdbc.query(text, ps, (rs, i) -> {
            String v = rs.getString("val");
            return (v == null || v.isBlank()) ? "(empty)" : v;
        });

        boolean hasMore   = items.size() == limit;
        String nextCursor = hasMore ? CursorCodec.encode(items.get(items.size() - 1)) : null;

        return new DistinctValuesPageDTO(items, nextCursor, hasMore, totalCount);
    }

    /* ========= 2) Column 축 상위 값 ========= */

    @Override
    public List<String> findTopColumnValues(
            String layer,
            String columnField,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw
    ) {
        String table = sql.table(layer);
        String col   = sql.col(layer, columnField);

        MapSqlParameterSource ps = new MapSqlParameterSource();
        String where = sql.where(layer, "ts_server_nsec", tw, filters, ps);

        String text = """
            SELECT %s AS col_val, COUNT(*) AS cnt
            FROM %s
            %s
            GROUP BY %s
            ORDER BY cnt DESC, %s ASC
        """.formatted(col, table, where, col, col);

        return jdbc.query(text, ps, (rs, i) -> rs.getString("col_val"));
    }

    /* ========= 3) Row distinct 값 ========= */

    public List<String> fetchDistinctRowValues(
            String layer,
            String rowField,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw
    ) {
        String table = sql.table(layer);
        String col = sql.col(layer, rowField);

        MapSqlParameterSource ps = new MapSqlParameterSource();
        String where = sql.where(layer, "ts_server_nsec", tw, filters, ps);

        String text = """
        SELECT DISTINCT %s AS val
        FROM %s
        %s
        ORDER BY %s ASC
    """.formatted(col, table, where, col);

        return jdbc.query(text, ps, (rs, i) -> rs.getString("val"));
    }

    public List<String> fetchDistinctRowValues(
            String layer,
            String rowField,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw,
            int offset,
            int limit
    ) {
        String table = sql.table(layer);
        String col = sql.col(layer, rowField);

        MapSqlParameterSource ps = new MapSqlParameterSource();
        String where = sql.where(layer, "ts_server_nsec", tw, filters, ps);

        String text = """
        SELECT DISTINCT %s AS val
        FROM %s
        %s
        ORDER BY %s ASC
        LIMIT :limit OFFSET :offset
    """.formatted(col, table, where, col);

        ps.addValue("limit", limit);
        ps.addValue("offset", offset);

//        return jdbc.query(text, ps, (rs, i) -> rs.getString("val"));
        log.info("=== fetchDistinctRowValues ===");
        log.info("SQL: {}", text);
        log.info("offset: {}, limit: {}", offset, limit);

        List<String> result = jdbc.query(text, ps, (rs, i) -> rs.getString("val"));

        log.info("Returned {} rows", result.size());

        return result;
    }

    /* ========= 4) Column별 요약 ========= */

    private Map<String, Map<String, Object>> fetchSummaryByColumn(
            String layer,
            String columnField,
            List<String> columnValues,
            List<PivotQueryRequestDTO.ValueDef> metrics,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw
    ) {
        String table = sql.table(layer);
        String col   = sql.col(layer, columnField);

        String aggSelect = metrics.stream()
                .map(m -> m.getAgg().toUpperCase() + "(" + sql.col(layer, m.getField()) + ") AS \"" + m.getAlias() + "\"")
                .collect(Collectors.joining(", "));

        MapSqlParameterSource ps = new MapSqlParameterSource();
        String where = sql.where(layer, "ts_server_nsec", tw, filters, ps);

        String text = """
            SELECT %s AS col_val,
                   %s
            FROM %s
            %s
            GROUP BY %s
        """.formatted(col, aggSelect, table, where, col);

        Map<String, Map<String, Object>> result = new LinkedHashMap<>();

        jdbc.query(text, ps, rs -> {
            String colVal = rs.getString("col_val");
            if (colVal == null || !columnValues.contains(colVal)) return;

            Map<String, Object> metricMap = new LinkedHashMap<>();
            for (PivotQueryRequestDTO.ValueDef m : metrics) {
                metricMap.put(m.getAlias(), rs.getObject(m.getAlias()));
            }
            result.put(colVal, metricMap);
        });

        // 빠진 컬럼 보강
        for (String v : columnValues) {
            result.putIfAbsent(v, new LinkedHashMap<>());
        }

        return result;
    }

    /* ========= 5) Row x Column breakdown ========= */

    private Map<String, Map<String, Map<String, Object>>> fetchBreakdownByRowAndColumn(
            String layer,
            String rowField,
            String columnField,
            List<PivotQueryRequestDTO.ValueDef> metrics,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw
    ) {
        String table = sql.table(layer);
        String row   = sql.col(layer, rowField);
        String col   = sql.col(layer, columnField);

        String aggSelect = metrics.stream()
                .map(m -> m.getAgg().toUpperCase() + "(" + sql.col(layer, m.getField()) + ") AS \"" + m.getAlias() + "\"")
                .collect(Collectors.joining(", "));

        MapSqlParameterSource ps = new MapSqlParameterSource();
        String where = sql.where(layer, "ts_server_nsec", tw, filters, ps);

        String text = """
            SELECT %s AS row_val,
                   %s AS col_val,
                   %s
            FROM %s
            %s
            GROUP BY %s, %s
        """.formatted(row, col, aggSelect, table, where, row, col);

        Map<String, Map<String, Map<String, Object>>> tmp = new LinkedHashMap<>();

        jdbc.query(text, ps, rs -> {
            String rv = rs.getString("row_val");
            String cv = rs.getString("col_val");

            Map<String, Object> metricMap = new LinkedHashMap<>();
            for (PivotQueryRequestDTO.ValueDef m : metrics) {
                metricMap.put(m.getAlias(), rs.getObject(m.getAlias()));
            }

            tmp.computeIfAbsent(rv, k -> new LinkedHashMap<>())
                    .put(cv, metricMap);
        });

        return tmp;
    }

    /* ========= 6) RowGroup 빌드 ========= */

    @Override
    public List<PivotQueryResponseDTO.RowGroup> buildRowGroups(
            String layer,
            List<PivotQueryRequestDTO.RowDef> rows,
            List<PivotQueryRequestDTO.ValueDef> values,
            String columnField,
            List<String> columnValues,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw
    ) {
        List<PivotQueryResponseDTO.RowGroup> groups = new ArrayList<>();

        boolean hasColumn = columnField != null && !columnField.isBlank()
                && columnValues != null && !columnValues.isEmpty();
        boolean hasMetrics = values != null && !values.isEmpty();

        // 1) 컬럼 요약 == 공통: 1번 계산
        Map<String, Map<String, Object>> summaryCells = new LinkedHashMap<>();
        if (hasColumn && hasMetrics) {
            summaryCells =
                    fetchSummaryByColumn(layer, columnField, columnValues, values, filters, tw);
        }

        // 2) 각 row group: distinct 값 + 개수 계산
        for (PivotQueryRequestDTO.RowDef rowDef : rows) {
            String rowField = rowDef.getField();

            // 해당 필드의 distinct 값들
            List<String> rowVals = fetchDistinctRowValues(layer, rowField, filters, tw);
            int distinctCount = rowVals.size();

            groups.add(
                    PivotQueryResponseDTO.RowGroup.builder()
                            .rowLabel(rowField)
                            .displayLabel(rowField + " (" + distinctCount + ")")
                            .rowInfo(PivotQueryResponseDTO.RowInfo.builder()
                                    .count(distinctCount)
                                    .build())
                            .cells(summaryCells)    // 모든 row group이 공유
                            .items(List.of())       // subRows 계산 X
                            .build()
            );
        }

        return groups;
    }


    @Override
    public List<PivotQueryResponseDTO.RowGroupItem> buildRowGroupItems(
            String layer,
            String rowField,
            List<PivotQueryRequestDTO.ValueDef> values,
            String columnField,
            List<String> columnValues,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw,
            int offset,
            int limit,
            PivotQueryRequestDTO.SortDef sort
    ) {
        boolean hasColumn = columnField != null && !columnField.isBlank()
                && columnValues != null && !columnValues.isEmpty();
        boolean hasMetrics = values != null && !values.isEmpty();

        // 🔹 공통: breakdown( row → column → metric ) 은 한 번 계산
        Map<String, Map<String, Map<String, Object>>> breakdown = new LinkedHashMap<>();

        if (hasColumn && hasMetrics) {
            breakdown =
                    fetchBreakdownByRowAndColumn(layer, rowField, columnField, values, filters, tw);
        }

        final Map<String, Map<String, Map<String, Object>>> breakdownFinal = breakdown;

        boolean hasSort =
                sort != null &&
                        sort.getDirection() != null &&
                        sort.getValueField() != null &&
                        sort.getAgg() != null &&
                        sort.getColumnValue() != null &&
                        hasColumn &&
                        hasMetrics;

        if (!hasSort) {
            List<String> rowVals =
                    fetchDistinctRowValues(layer, rowField, filters, tw, offset, limit);

            List<PivotQueryResponseDTO.RowGroupItem> items = new ArrayList<>();
            for (String rv : rowVals) {
                Map<String, Map<String, Object>> childCells = new LinkedHashMap<>();

                if (hasColumn && hasMetrics) {
                    Map<String, Map<String, Object>> byCol = breakdown.getOrDefault(rv, Map.of());
                    for (String cv : columnValues) {
                        childCells.put(cv, byCol.getOrDefault(cv, Map.of()));
                    }
                }

                items.add(
                        PivotQueryResponseDTO.RowGroupItem.builder()
                                .valueLabel(rv)
                                .displayLabel(rv)
                                .cells(childCells)
                                .build()
                );
            }

            return items;
        }

        List<String> allRowVals = fetchDistinctRowValues(layer, rowField, filters, tw);

        String sortAlias = null;
        if (values != null) {
            for (PivotQueryRequestDTO.ValueDef v : values) {
                if (sort.getValueField().equals(v.getField())
                        && sort.getAgg().equalsIgnoreCase(v.getAgg())) {
                    sortAlias = v.getAlias();
                    break;
                }
            }
        }

        final String sortAliasFinal = sortAlias;
        final String sortColumnValue = sort.getColumnValue();
        final String direction = sort.getDirection();

        if (sortAliasFinal == null) {
            return buildRowGroupItems(
                    layer,
                    rowField,
                    values,
                    columnField,
                    columnValues,
                    filters,
                    tw,
                    offset,
                    limit,
                    null    // 재귀: sort 없는 버전으로
            );
        }

        // 2-3) 정렬
        allRowVals.sort((rv1, rv2) -> {
            Map<String, Map<String, Object>> byCol1 =
                    breakdownFinal.getOrDefault(rv1, Collections.emptyMap());
            Map<String, Map<String, Object>> byCol2 =
                    breakdownFinal.getOrDefault(rv2, Collections.emptyMap());

            Map<String, Object> metrics1 =
                    byCol1.getOrDefault(sortColumnValue, Collections.emptyMap());
            Map<String, Object> metrics2 =
                    byCol2.getOrDefault(sortColumnValue, Collections.emptyMap());

            Object o1 = metrics1.get(sortAliasFinal);
            Object o2 = metrics2.get(sortAliasFinal);

            double d1 = (o1 instanceof Number) ? ((Number) o1).doubleValue() : Double.NaN;
            double d2 = (o2 instanceof Number) ? ((Number) o2).doubleValue() : Double.NaN;

            // NaN (값 없음)은 항상 뒤로 보내기
            if (Double.isNaN(d1) && Double.isNaN(d2)) return 0;
            if (Double.isNaN(d1)) return 1;
            if (Double.isNaN(d2)) return -1;

            int cmp = Double.compare(d1, d2);
            if ("desc".equalsIgnoreCase(direction)) {
                cmp = -cmp;
            }

            if (cmp != 0) return cmp;

            // tie-breaker: row 값 알파벳 순
            if (rv1 == null && rv2 == null) return 0;
            if (rv1 == null) return 1;
            if (rv2 == null) return -1;
            return rv1.compareTo(rv2);
        });

        int fromIdx = Math.max(0, offset);
        int toIdx = Math.min(allRowVals.size(), offset + limit);
        if (fromIdx >= toIdx) {
            return List.of();
        }

        List<String> pageRowVals = allRowVals.subList(fromIdx, toIdx);

        // 2-5) pageRowVals 기준으로 RowGroupItem 빌드
        List<PivotQueryResponseDTO.RowGroupItem> items = new ArrayList<>();
        for (String rv : pageRowVals) {
            Map<String, Map<String, Object>> childCells = new LinkedHashMap<>();

            if (hasColumn && hasMetrics) {
                Map<String, Map<String, Object>> byCol = breakdown.getOrDefault(rv, Map.of());
                for (String cv : columnValues) {
                    childCells.put(cv, byCol.getOrDefault(cv, Map.of()));
                }
            }

            items.add(
                    PivotQueryResponseDTO.RowGroupItem.builder()
                            .valueLabel(rv)
                            .displayLabel(rv)
                            .cells(childCells)
                            .build()
            );
        }

        return items;
    }

    @Override
    public List<String> findTopNDimensionValues(
            String layer,
            String field,
            PivotQueryRequestDTO.TopNDef topN,
            PivotQueryRequestDTO.ValueDef metric,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw
    ) {
        if (topN == null || metric == null) {
            return List.of();
        }

        String table = sql.table(layer);
        String dimCol = sql.col(layer, field);              // dimension 컬럼
        String metricCol = sql.col(layer, metric.getField()); // metric 대상 컬럼

        String aggFunc = metric.getAgg() != null
                ? metric.getAgg().toUpperCase()
                : "SUM";

        String orderDir = "bottom".equalsIgnoreCase(topN.getMode())
                ? "ASC"
                : "DESC"; // 기본은 Top = DESC

        int limit = (topN.getN() != null && topN.getN() > 0)
                ? topN.getN()
                : 5;

        MapSqlParameterSource ps = new MapSqlParameterSource();
        String where = sql.where(layer, "ts_server_nsec", tw, filters, ps);

        String text = """
        SELECT %s AS dim_val,
               %s(%s) AS metric_val
        FROM %s
        %s
        GROUP BY %s
        ORDER BY metric_val %s
        LIMIT :lim
    """.formatted(dimCol, aggFunc, metricCol, table, where, dimCol, orderDir);

        ps.addValue("lim", limit);

        return jdbc.query(text, ps, (rs, i) -> rs.getString("dim_val"));
    }

    @Override
    public PivotChartResponseDTO getChart(PivotChartRequestDTO req, TimeWindow tw) {

        String layer = req.getLayer();
        if (layer == null || layer.isBlank()) {
            throw new BadRequestException("layer is required");
        }

        PivotChartRequestDTO.AxisDef colDef = req.getCol();
        PivotChartRequestDTO.AxisDef rowDef = req.getRow();
        PivotQueryRequestDTO.ValueDef metricDef = req.getMetric();

        if (colDef == null || colDef.getField() == null || colDef.getField().isBlank()) {
            throw new BadRequestException("col.field is required");
        }
        if (rowDef == null || rowDef.getField() == null || rowDef.getField().isBlank()) {
            throw new BadRequestException("row.field is required");
        }
        if (metricDef == null || metricDef.getField() == null || metricDef.getField().isBlank()) {
            throw new BadRequestException("metric.field is required");
        }

        String table   = sql.table(layer);
        String colExpr = sql.col(layer, colDef.getField()); // xField용
        String rowExpr = sql.col(layer, rowDef.getField()); // yField용

        MapSqlParameterSource ps = new MapSqlParameterSource();

        // 시간 컬럼
        String timeField = "ts_server_nsec";
        if (req.getTime() != null && req.getTime().getField() != null &&
                !req.getTime().getField().isBlank()) {
            timeField = req.getTime().getField();
        }

        // 기본 필터 (시간 + 테이블에서 사용자가 설정한 필터들)
        List<PivotQueryRequestDTO.FilterDef> baseFilters =
                req.getFilters() != null ? new ArrayList<>(req.getFilters()) : new ArrayList<>();

        // chartType에 따라 column 최대 개수 조정 (multiplePie만 6까지 허용)
        boolean isMultiplePie = "multiplePie".equalsIgnoreCase(req.getChartType());
        int maxColCount = isMultiplePie ? 6 : 5;  // xField 최대 개수
        int maxRowCount = 5;                      // yField 최대 개수

        // 1) x, y 각각의 카테고리 목록 계산 (TOP-N / manual)
        List<String> xCategories = resolveAxisKeys(
                layer, table, colDef, metricDef,
                timeField, tw, baseFilters,
                maxColCount
        );
        List<String> yCategories = resolveAxisKeys(
                layer, table, rowDef, metricDef,
                timeField, tw, baseFilters,
                maxRowCount
        );

        // 아무 값도 없으면 empty 응답
        if (xCategories.isEmpty() || yCategories.isEmpty()) {
            return PivotChartResponseDTO.empty(colDef.getField(), rowDef.getField());
        }

        // 2) 최종 필터: baseFilters + xField IN (...), yField IN (...)
        List<PivotQueryRequestDTO.FilterDef> finalFilters = new ArrayList<>(baseFilters);

        PivotQueryRequestDTO.FilterDef xFilter = new PivotQueryRequestDTO.FilterDef();
        xFilter.setField(colDef.getField());
        xFilter.setOp("IN");
        xFilter.setValue(new ArrayList<>(xCategories)); // "(empty)"는 sql.where에서 처리
        finalFilters.add(xFilter);

        PivotQueryRequestDTO.FilterDef yFilter = new PivotQueryRequestDTO.FilterDef();
        yFilter.setField(rowDef.getField());
        yFilter.setOp("IN");
        yFilter.setValue(new ArrayList<>(yCategories));
        finalFilters.add(yFilter);

        String where = sql.where(layer, timeField, tw, finalFilters, ps);

        // metric 표현식 생성 (SUM, AVG, COUNT 등)
        String metricExpr = buildMetricExpr(metricDef, layer);

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ")
                .append(colExpr).append(" AS c, ")
                .append(rowExpr).append(" AS r, ")
                .append(metricExpr).append(" AS m ")
                .append("FROM ").append(table).append(" ")
                .append(where)
                .append(" GROUP BY ").append(colExpr).append(", ").append(rowExpr);

        String sqlText = sb.toString();

        // 3) (y, x) → metric 값 매핑
        // key: yKey -> (xKey -> value)
        Map<String, Map<String, Double>> valueMap = new HashMap<>();

        jdbc.query(sqlText, ps, rs -> {
            String xKey = ValueUtils.normalizeKey(rs.getString("c"));
            String yKey = ValueUtils.normalizeKey(rs.getString("r"));
            double v = rs.getDouble("m");
            if (rs.wasNull()) {
                v = 0.0;
            }

            valueMap
                    .computeIfAbsent(yKey, k -> new HashMap<>())
                    .put(xKey, v);
        });

        // 4) yCategories, xCategories 순서에 맞춰 2D values 구성
        // values[yIndex][xIndex]
        List<List<Double>> valuesMatrix = new ArrayList<>();

        for (String yKey : yCategories) {
            List<Double> rowValues = new ArrayList<>();
            Map<String, Double> rowMap = valueMap.get(yKey);

            for (String xKey : xCategories) {
                double v = 0.0;
                if (rowMap != null) {
                    Double vv = rowMap.get(xKey);
                    if (vv != null) {
                        v = vv;
                    }
                }
                rowValues.add(v);
            }
            valuesMatrix.add(rowValues);
        }

        // 5) metric 단위 series 구성 (지금은 단일 metric이지만 리스트로 감싸둠)
        String metricField = metricDef.getField();
        String metricAgg   = metricDef.getAgg();

        String metricId = metricField + "::" + (metricAgg != null ? metricAgg : "agg");

        String metricLabel;
        if (metricDef.getAlias() != null && !metricDef.getAlias().isBlank()) {
            metricLabel = metricDef.getAlias();
        } else {
            String upperAgg = metricAgg != null ? metricAgg.toUpperCase() : "VAL";
            metricLabel = upperAgg + ": " + metricField;  // ex) "SUM: total_bill"
        }

        PivotChartResponseDTO.SeriesDef seriesDef =
                PivotChartResponseDTO.SeriesDef.builder()
                        .id(metricId)
                        .label(metricLabel)
                        .field(metricField)
                        .agg(metricAgg)
                        .values(valuesMatrix)
                        .build();

        return PivotChartResponseDTO.builder()
                .xField(colDef.getField())
                .yField(rowDef.getField())
                .xCategories(xCategories)
                .yCategories(yCategories)
                .series(Collections.singletonList(seriesDef))
                .build();
    }

    private List<String> resolveAxisKeys(
            String layer,
            String table,
            PivotChartRequestDTO.AxisDef axisDef,
            PivotQueryRequestDTO.ValueDef metricDef,
            String timeField,
            TimeWindow tw,
            List<PivotQueryRequestDTO.FilterDef> baseFilters,
            int maxCount
    ) {
        if (axisDef == null || axisDef.getField() == null || axisDef.getField().isBlank()) {
            return Collections.emptyList();
        }

        String mode = axisDef.getMode() != null ? axisDef.getMode().toLowerCase() : "topn";

        // 1) manual 모드: selectedItems 그대로 사용 (최대 maxCount까지)
        if ("manual".equals(mode)) {
            List<String> selected = axisDef.getSelectedItems();
            if (selected == null || selected.isEmpty()) {
                return Collections.emptyList();
            }
            List<String> result = new ArrayList<>();
            for (String v : selected) {
                if (v == null || v.isBlank()) {
                    result.add("(empty)");
                } else {
                    result.add(v);
                }
                if (result.size() >= maxCount) break;
            }
            return result;
        }

        // 2) topN 모드 (기본 5, 최대 maxCount)
        int n = 5;
        if (axisDef.getTopN() != null && axisDef.getTopN().getN() != null && axisDef.getTopN().getN() > 0) {
            n = axisDef.getTopN().getN();
        }
        if (n > maxCount) {
            n = maxCount;
        }

        String axisExpr = sql.col(layer, axisDef.getField());
        String metricExpr = buildMetricExpr(metricDef, layer);

        MapSqlParameterSource ps = new MapSqlParameterSource();
        String where = sql.where(layer, timeField, tw, baseFilters, ps);
        ps.addValue("lim", n);

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ")
                .append(axisExpr).append(" AS k, ")
                .append(metricExpr).append(" AS m ")
                .append("FROM ").append(table).append(" ")
                .append(where)
                .append(" GROUP BY ").append(axisExpr)
                .append(" ORDER BY m DESC ")
                .append(" LIMIT :lim");

        String sqlText = sb.toString();

        List<String> result = new ArrayList<>();
        jdbc.query(sqlText, ps, rs -> {
            String key = ValueUtils.normalizeKey(rs.getString("k"));
            result.add(key);
        });

        return result;
    }
    private String buildMetricExpr(PivotQueryRequestDTO.ValueDef v, String layer) {
        String agg = v.getAgg() != null ? v.getAgg().toLowerCase() : "sum";

        if ("count".equals(agg)) {
            return "COUNT(*)";
        } else {
            String valCol = sql.col(layer, v.getField());
            return switch (agg) {
                case "avg" -> "AVG(" + valCol + ")";
                case "min" -> "MIN(" + valCol + ")";
                case "max" -> "MAX(" + valCol + ")";
                default -> "SUM(" + valCol + ")";
            };
        }
    }
}
