package com.moa.api.pivot.service;

import com.moa.api.pivot.dto.request.*;
import com.moa.api.pivot.dto.response.*;
import com.moa.api.pivot.model.PivotFieldMeta;
import com.moa.api.pivot.model.PivotLayer;
import com.moa.api.pivot.model.PivotQueryContext;
import com.moa.api.pivot.model.TimeWindow;
import com.moa.api.pivot.repository.PivotRepository;
import com.moa.api.pivot.repository.SqlSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PivotService {

    private final PivotRepository pivotRepository;
    private final SqlSupport sqlSupport;
    private final TopNResolver topNResolver;

    public PivotFieldsResponseDTO getFields(String layerRaw) {
        PivotLayer layer = PivotLayer.from(layerRaw);
        List<PivotFieldMeta> metaList = pivotRepository.findFieldMetaForLayer(layer);

        List<PivotFieldsResponseDTO.FieldMeta> fieldMetaList = metaList.stream()
                .map(m -> PivotFieldsResponseDTO.FieldMeta.builder()
                        .name(m.getName())
                        .label(m.getLabel())
                        .dataType(m.getDataType())
                        .build()
                )
                .toList();

        return PivotFieldsResponseDTO.builder()
                .layer(layer.getCode())
                .fields(fieldMetaList)
                .build();
    }

    /* 시간 범위 → TimeWindow */
    public TimeWindow resolveTimeWindow(PivotQueryRequestDTO.TimeDef time) {
        if (time == null || time.getFromEpoch() == null || time.getToEpoch() == null) {
            long nowSec = Instant.now().getEpochSecond();
            return new TimeWindow(nowSec - 3600, nowSec);
        }
        return new TimeWindow(time.getFromEpoch(), time.getToEpoch());
    }

    /* ===== 1) 피벗 실행 ===== */
    public PivotQueryResponseDTO runPivot(PivotQueryRequestDTO req) {

        PivotLayer layer = PivotLayer.from(req.getLayer());
        TimeWindow tw = resolveTimeWindow(req.getTime());

        List<PivotQueryRequestDTO.ValueDef> valueDefs =
                (req.getValues() != null) ? req.getValues() : List.of();

        // TopN 적용/치환된 필터
        List<PivotQueryRequestDTO.FilterDef> effectiveFilters =
                topNResolver.resolveFilters(layer, valueDefs, req.getFilters(), tw);

        PivotQueryContext ctx = new PivotQueryContext(
                layer,
                req.getTime() != null ? req.getTime().getField() : null,
                tw,
                effectiveFilters,
                sqlSupport
        );

        String columnFieldName = (req.getColumn() != null) ? req.getColumn().getField() : null;

        List<String> columnValues = List.of();
        if (columnFieldName != null && !columnFieldName.isBlank()) {
            columnValues = pivotRepository.findTopColumnValues(ctx, columnFieldName);
        }

        List<PivotQueryResponseDTO.RowGroup> rowGroups =
                pivotRepository.buildRowGroups(ctx, req.getRows(), req.getValues(), columnFieldName, columnValues);

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

    /* ===== 2) Distinct 값 페이지 ===== */
    public DistinctValuesResponseDTO getDistinctValuesPage(DistinctValuesRequestDTO req) {
        TimeWindow tw = resolveTimeWindow(req.getTime());
        // 기본값 보정
        if (req.getOrder() == null) req.setOrder("asc");
        if (req.getLimit() == null || req.getLimit() <= 0) req.setLimit(50);

        return pivotRepository.pageDistinctValues(req, tw);
    }

    /* ===== 3) 특정 RowGroup의 subRows ===== */
    public RowGroupItemsResponseDTO getRowGroupItems(RowGroupItemsRequestDTO req) {

        PivotLayer layer = PivotLayer.from(req.getLayer());
        TimeWindow tw = resolveTimeWindow(req.getTime());

        List<PivotQueryRequestDTO.FilterDef> effectiveFilters =
                topNResolver.resolveFilters(layer, req.getValues(), req.getFilters(), tw);

        PivotQueryContext ctx = new PivotQueryContext(
                layer,
                req.getTime() != null ? req.getTime().getField() : null,
                tw,
                effectiveFilters,
                sqlSupport
        );

        String columnFieldName = (req.getColumn() != null)
                ? req.getColumn().getField()
                : null;

        List<String> columnValues = List.of();
        if (columnFieldName != null && !columnFieldName.isBlank()) {
            columnValues = pivotRepository.findTopColumnValues(ctx, columnFieldName);
        }

        int offset = 0;
        if (req.getCursor() != null && req.getCursor().startsWith("offset:")) {
            offset = Integer.parseInt(req.getCursor().substring(7));
        }
        int limit = req.getLimit() != null ? req.getLimit() : 50;

        List<PivotQueryResponseDTO.RowGroupItem> items =
                pivotRepository.buildRowGroupItems(
                        ctx,
                        req.getRowField(),
                        req.getValues(),
                        columnFieldName,
                        columnValues,
                        offset,
                        limit + 1,
                        req.getSort()
                );

        boolean hasMore = items.size() > limit;
        if (hasMore) {
            items = items.subList(0, limit);
        }

        String nextCursor = hasMore ? "offset:" + (offset + limit) : null;
        String rowLabel = req.getRowField() + " (" + items.size() + ")";

        return RowGroupItemsResponseDTO.builder()
                .rowField(req.getRowField())
                .rowLabel(rowLabel)
                .items(items)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }

    /* ===== 4) 차트 ===== */
    public PivotChartResponseDTO getChart(PivotChartRequestDTO req) {
        TimeWindow tw = resolveTimeWindow(req.getTime());
        PivotLayer layer = PivotLayer.from(req.getLayer());

        PivotQueryContext ctx = new PivotQueryContext(
                layer,
                req.getTime() != null ? req.getTime().getField() : null,
                tw,
                req.getFilters(),
                sqlSupport
        );

        return pivotRepository.getChart(ctx, req);
    }

    /* ===== 5) Heatmap Table ===== */
    public PivotHeatmapTableResponseDTO getHeatmapTable(PivotHeatmapTableRequestDTO req) {
        TimeWindow tw = resolveTimeWindow(req.getTime());
        PivotLayer layer = PivotLayer.from(req.getLayer());

        PivotQueryContext ctx = new PivotQueryContext(
                layer,
                req.getTime() != null ? req.getTime().getField() : null,
                tw,
                req.getFilters(),
                sqlSupport
        );

        return pivotRepository.getHeatmapTable(ctx, req);
    }
}
