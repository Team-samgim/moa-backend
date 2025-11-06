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
import java.util.List;

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

        String columnFieldName = (req.getColumn() != null) ? req.getColumn().getField() : null;

        List<String> columnValues = List.of();

        if (columnFieldName != null && !columnFieldName.isBlank()) {
            columnValues = pivotRepository.findTopColumnValues(
                    req.getLayer(),
                    columnFieldName,
                    req.getFilters(),
                    tw
            );
        }

        List<PivotQueryRequestDTO.ValueDef> valueDefs =
                (req.getValues() != null) ? req.getValues() : List.of();

        // row 그룹들
        List<PivotQueryResponseDTO.RowGroup> rowGroups = pivotRepository.buildRowGroups(
                req.getLayer(),
                req.getRows(),
                req.getValues(),
                columnFieldName,
                columnValues,
                req.getFilters(),
                tw
        );

        // 요청의 values -> Metric DTO로 매핑
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
                .name(columnFieldName)   // null 일 수도 있음
                .values(columnValues)    // 빈 리스트일 수도 있음
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

        String columnFieldName = (req.getColumn() != null)
                ? req.getColumn().getField()
                : null;

        List<String> columnValues = List.of();
        if (columnFieldName != null && !columnFieldName.isBlank()) {
            columnValues = pivotRepository.findTopColumnValues(
                    layer,
                    columnFieldName,
                    req.getFilters(),
                    tw
            );
        }

        // cursor 파싱
        int offset = 0;
        if (req.getCursor() != null && req.getCursor().startsWith("offset:")) {
            offset = Integer.parseInt(req.getCursor().substring(7));
        }

        int limit = req.getLimit() != null ? req.getLimit() : 50;

        // items 조회 (limit + 1로 hasMore 판단)
        List<PivotQueryResponseDTO.RowGroupItem> items =
                pivotRepository.buildRowGroupItems(
                        layer,
                        rowField,
                        req.getValues(),
                        columnFieldName,
                        columnValues,
                        req.getFilters(),
                        tw,
                        offset,
                        limit + 1,
                        req.getSort()
                );

        // hasMore 판단 및 초과분 제거
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
}
