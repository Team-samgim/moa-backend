package com.moa.api.pivot.service;

import com.moa.api.pivot.dto.*;
import com.moa.api.pivot.model.TimeWindow;
import com.moa.api.pivot.repository.PivotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PivotService {

    private final PivotRepository pivotRepository;

    public PivotFieldsResponseDTO getFields(String layerRaw) {
        String layer = (layerRaw == null || layerRaw.isBlank())
                ? "HTTP_PAGE"
                : layerRaw;

        List<String> cols = pivotRepository.findColumnNamesForLayer(layer);

        List<PivotFieldsResponseDTO.FieldMeta> fieldMetaList = cols.stream()
                .map(col -> PivotFieldsResponseDTO.FieldMeta.builder()
                        .name(col)
                        .build())
                .toList();

        return PivotFieldsResponseDTO.builder()
                .layer(layer)
                .fields(fieldMetaList)
                .build();
    }

    /* ===== 공통: timeRange + customRange → TimeWindow 변환 ===== */
    private TimeWindow resolveTimeWindow(
            PivotQueryRequestDTO.TimeDef time
    ) {
        // 방어 코드 (기본: 최근 1시간, UNIX seconds)
        if (time == null || time.getFromEpoch() == null || time.getToEpoch() == null) {
            long nowSec = Instant.now().getEpochSecond();
            return new TimeWindow(nowSec - 3600, nowSec);  // 최근 1시간
        }

        // 프론트에서 보내준 epoch 그대로 사용 (초 단위라고 가정)
        double from = time.getFromEpoch();
        double to   = time.getToEpoch();

        return new TimeWindow(from, to);
    }


    private LocalDateTime parseToLocalDateTime(String s) {
        if (s == null || s.isBlank()) {
            return LocalDateTime.now(ZoneOffset.UTC);
        }

        if (s.endsWith("Z")) {
            // 2025-01-01T00:00:00Z 형태
            return OffsetDateTime.parse(s).toLocalDateTime();
        }
        // 2025-01-01T00:00:00 형태
        return LocalDateTime.parse(s);
    }

    /* ===== 1) 피벗 실행 ===== */
    public PivotQueryResponseDTO runPivot(PivotQueryRequestDTO req) {

        TimeWindow tw = resolveTimeWindow(req.getTime());

        List<PivotQueryRequestDTO.ValueDef> valueDefs =
                (req.getValues() != null) ? req.getValues() : List.of();

        List<PivotQueryRequestDTO.FilterDef> effectiveFilters =
                resolveTopNFilters(req.getLayer(), valueDefs, req.getFilters(), tw);

        String columnFieldName = (req.getColumn() != null) ? req.getColumn().getField() : null;

        List<String> columnValues = List.of();

        if (columnFieldName != null && !columnFieldName.isBlank()) {
            columnValues = pivotRepository.findTopColumnValues(
                    req.getLayer(),
                    columnFieldName,
                    effectiveFilters,
                    tw
            );
        }

        // row 그룹들
        List<PivotQueryResponseDTO.RowGroup> rowGroups = pivotRepository.buildRowGroups(
                req.getLayer(),
                req.getRows(),
                req.getValues(),
                columnFieldName,
                columnValues,
                effectiveFilters,
                tw
        );

        // metrics 매핑 & summary 는 그대로
        List<PivotQueryResponseDTO.Metric> metrics =
                valueDefs.stream()
                        .map(v -> PivotQueryResponseDTO.Metric.builder()
                                .alias(v.getAlias())
                                .field(v.getField())
                                .agg(v.getAgg())
                                .build()
                        )
                        .toList();

        PivotQueryResponseDTO.ColumnField columnField = PivotQueryResponseDTO.ColumnField.builder()
                .name(columnFieldName)
                .values(columnValues)
                .metrics(metrics)
                .build();

        PivotQueryResponseDTO.Summary summary = PivotQueryResponseDTO.Summary.builder()
                .rowCountText("합계: " + (rowGroups != null ? rowGroups.size() : 0) + "행")
                .build();

        return PivotQueryResponseDTO.builder()
                .columnField(columnField)
                .rowGroups(rowGroups)
                .summary(summary)
                .build();
    }



    /* ===== 2) 필드 값 페이지네이션 (무한 스크롤 + 검색) ===== */
    public DistinctValuesPageDTO getDistinctValuesPage(DistinctValuesRequestDTO req) {
        TimeWindow tw = resolveTimeWindow(req.getTime());
        // 기본값 보정
        if (req.getOrder() == null) req.setOrder("asc");
        if (req.getLimit() == null || req.getLimit() <= 0) req.setLimit(50);

        return pivotRepository.pageDistinctValues(req, tw);
    }

    /* ===== 3) 특정 row group의 subRows + breakdown 조회 ===== */
    public RowGroupItemsResponseDTO getRowGroupItems(RowGroupItemsRequestDTO req) {

        TimeWindow tw = resolveTimeWindow(req.getTime());

        String layer = req.getLayer();
        String rowField = req.getRowField();

        // 🔥 TOP-N 처리
        List<PivotQueryRequestDTO.FilterDef> effectiveFilters =
                resolveTopNFilters(layer, req.getValues(), req.getFilters(), tw);

        String columnFieldName = (req.getColumn() != null)
                ? req.getColumn().getField()
                : null;

        List<String> columnValues = List.of();
        if (columnFieldName != null && !columnFieldName.isBlank()) {
            columnValues = pivotRepository.findTopColumnValues(
                    layer,
                    columnFieldName,
                    effectiveFilters,   // 🔥 변경
                    tw
            );
        }

        int offset = 0;
        if (req.getCursor() != null && req.getCursor().startsWith("offset:")) {
            offset = Integer.parseInt(req.getCursor().substring(7));
        }

        int limit = req.getLimit() != null ? req.getLimit() : 50;

        List<PivotQueryResponseDTO.RowGroupItem> items =
                pivotRepository.buildRowGroupItems(
                        layer,
                        rowField,
                        req.getValues(),
                        columnFieldName,
                        columnValues,
                        effectiveFilters,
                        tw,
                        offset,
                        limit + 1,
                        req.getSort()
                );

        boolean hasMore = items.size() > limit;
        if (hasMore) {
            items = items.subList(0, limit);
        }

        String nextCursor = hasMore ? "offset:" + (offset + limit) : null;

        String rowLabel = rowField + " (" + items.size() + ")";

        return RowGroupItemsResponseDTO.builder()
                .rowField(rowField)
                .rowLabel(rowLabel)
                .items(items)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }


    private List<PivotQueryRequestDTO.FilterDef> resolveTopNFilters(
            String layer,
            List<PivotQueryRequestDTO.ValueDef> values,  // metrics
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow tw
    ) {
        if (filters == null || filters.isEmpty()) {
            return List.of();
        }

        // baseFilters: topN 정보는 제거한 상태 (where 절 구성용 공통)
        List<PivotQueryRequestDTO.FilterDef> baseFilters = new ArrayList<>();
        for (PivotQueryRequestDTO.FilterDef f : filters) {
            PivotQueryRequestDTO.FilterDef copy = new PivotQueryRequestDTO.FilterDef();
            copy.setField(f.getField());
            copy.setOp(f.getOp());
            copy.setValue(f.getValue());
            copy.setOrder(f.getOrder());
            copy.setTopN(null);
            baseFilters.add(copy);
        }

        List<PivotQueryRequestDTO.FilterDef> resolved = new ArrayList<>();

        for (PivotQueryRequestDTO.FilterDef original : filters) {
            PivotQueryRequestDTO.TopNDef topN = original.getTopN();
            boolean enabled = topN != null && Boolean.TRUE.equals(topN.getEnabled());

            if (!enabled) {
                // topN 없는 필터는 baseFilters 버전 중 동일 field를 찾아 사용
                baseFilters.stream()
                        .filter(f -> Objects.equals(f.getField(), original.getField()))
                        .findFirst()
                        .ifPresent(resolved::add);
                continue;
            }

            // 1) 후보 중 TopN
            List<PivotQueryRequestDTO.FilterDef> filtersForTopN = baseFilters;

            // 2) 이 TOP-N에 사용할 metric 찾기 (alias == valueKey)
            PivotQueryRequestDTO.ValueDef metricDef = null;
            if (values != null) {
                for (PivotQueryRequestDTO.ValueDef v : values) {
                    if (topN.getValueKey() != null
                            && topN.getValueKey().equals(v.getAlias())) {
                        metricDef = v;
                        break;
                    }
                }
            }

            if (metricDef == null) {
                // 매칭되는 metric 이 없으면, 그냥 기존 필터의 base 버전 사용 (안전하게 fallback)
                baseFilters.stream()
                        .filter(f -> Objects.equals(f.getField(), original.getField()))
                        .findFirst()
                        .ifPresent(resolved::add);
                continue;
            }

            // 3) repo 를 통해 상위/하위 N dimension 값 조회
            List<String> topNValues = pivotRepository.findTopNDimensionValues(
                    layer,
                    original.getField(),
                    topN,
                    metricDef,
                    filtersForTopN,
                    tw
            );

            // 4) 이 필터를 "field IN (:topNValues)" 필터로 치환
            PivotQueryRequestDTO.FilterDef replaced = new PivotQueryRequestDTO.FilterDef();
            replaced.setField(original.getField());
            replaced.setOp("IN");
            replaced.setValue(topNValues);
            replaced.setOrder(original.getOrder());
            replaced.setTopN(null);

            resolved.add(replaced);
        }

        return resolved;
    }

}
