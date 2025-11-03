package com.moa.api.pivot.service;

import com.moa.api.pivot.dto.*;
import com.moa.api.pivot.model.TimeWindow;
import com.moa.api.pivot.repository.PivotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
            PivotQueryRequestDTO.TimeRange timeRange,
            PivotQueryRequestDTO.CustomRange customRange
    ) {
        // 1) 직접 설정(from/to) 우선
        if (customRange != null
                && customRange.getFrom() != null
                && customRange.getTo() != null) {

            LocalDateTime from = parseToLocalDateTime(customRange.getFrom());
            LocalDateTime to   = parseToLocalDateTime(customRange.getTo());

            return new TimeWindow(from, to);
        }

        // 2) preset 사용 (1h, 2h, 24h, 1w ...)
        //    timeRange.now 도 String 이라서 LocalDateTime 으로 변환 필요
        LocalDateTime now = (timeRange != null && timeRange.getNow() != null)
                ? parseToLocalDateTime(timeRange.getNow())
                : LocalDateTime.now(ZoneOffset.UTC);

        String preset = timeRange != null ? timeRange.getValue() : "1h";

        return switch (preset) {
            case "1h"  -> new TimeWindow(now.minusHours(1), now);
            case "2h"  -> new TimeWindow(now.minusHours(2), now);
            case "24h" -> new TimeWindow(now.minusHours(24), now);
            case "1w"  -> new TimeWindow(now.minusDays(7), now);
            default    -> new TimeWindow(now.minusHours(1), now);
        };
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

        TimeWindow tw = resolveTimeWindow(req.getTimeRange(), req.getCustomRange());

        String columnFieldName = (req.getColumn() != null) ? req.getColumn().getField() : null;

        // column 값 상위 10개
        List<String> columnValues = pivotRepository.findTopColumnValues(
                req.getLayer(),
                columnFieldName,
                req.getFilters(),
                tw
        );

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
        List<PivotQueryResponseDTO.Metric> metrics = null;
        if (req.getValues() != null) {
            metrics = req.getValues().stream()
                    .map(v -> PivotQueryResponseDTO.Metric.builder()
                            .alias(v.getAlias())   // "합계: total"
                            .field(v.getField())   // "total"
                            .agg(v.getAgg())       // "sum"
                            .build()
                    )
                    .toList();
        }

        // ColumnField 조립
        PivotQueryResponseDTO.ColumnField columnField = PivotQueryResponseDTO.ColumnField.builder()
                .name(columnFieldName)   // 예: "src_port"
                .values(columnValues)    // 예: ["ip_num_1", "ip_num_2", ...]
                .metrics(metrics)        // 값 기준 metric 정보
                .build();

        // Summary (일단 간단하게 row 개수 기준으로)
        PivotQueryResponseDTO.Summary summary = PivotQueryResponseDTO.Summary.builder()
                .rowCountText("합계: " + (rowGroups != null ? rowGroups.size() : 0) + "행")
                .build();

        // 최종 응답
        return PivotQueryResponseDTO.builder()
                .columnField(columnField)
                .rowGroups(rowGroups)
                .summary(summary)
                .build();
    }


    /* ===== 2) 필드 값 페이지네이션 (무한 스크롤 + 검색) ===== */
    public DistinctValuesPageDTO getDistinctValuesPage(DistinctValuesRequestDTO req) {
        TimeWindow tw = resolveTimeWindow(req.getTimeRange(), req.getCustomRange());
        // 기본값 보정
        if (req.getOrder() == null) req.setOrder("asc");
        if (req.getLimit() == null || req.getLimit() <= 0) req.setLimit(50);

        return pivotRepository.pageDistinctValues(req, tw);
    }
}
