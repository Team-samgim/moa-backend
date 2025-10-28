package com.moa.api.pivot.service;

import com.moa.api.pivot.dto.PivotFieldsResponseDTO;
import com.moa.api.pivot.dto.PivotQueryRequestDTO;
import com.moa.api.pivot.dto.PivotQueryResponseDTO;
import com.moa.api.pivot.repository.PivotRepository;
import com.moa.api.pivot.repository.PivotRepository.TimeWindow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PivotService {

    private final PivotRepository pivotRepository;

    public PivotFieldsResponseDTO getFieldsForLayer(String layer) {
        // 1. DB에서 실제 컬럼명 가져오기
        List<String> columnNames = pivotRepository.findColumnNamesForLayer(layer);

        // 2. 임시 FieldMeta 리스트
        List<PivotFieldsResponseDTO.FieldMeta> fieldMetaList = columnNames.stream()
            .map(col -> PivotFieldsResponseDTO.FieldMeta.builder()
                .name(col)
                .build())
            .toList();

        return PivotFieldsResponseDTO.builder()
            .layer(layer)
            .fields(fieldMetaList)
            .build();
    }

    // /api/pivot/query
    public PivotQueryResponseDTO runPivotQuery(PivotQueryRequestDTO request) {
        validateRequest(request);

        PivotRepository.TimeWindow tw = resolveTimeWindow(request);

        List<String> columnValues = pivotRepository.findTopColumnValues(
                request.getLayer(),
                request.getColumn().getField(),
                request.getFilters(),
                tw
        );

        var rowGroups = pivotRepository.buildRowGroups(
                request.getLayer(),
                request.getRows(),
                request.getValues(),
                request.getColumn().getField(),
                columnValues,
                request.getFilters(),
                tw
        );

        PivotQueryResponseDTO.Summary summary = PivotQueryResponseDTO.Summary.builder()
                .rowCountText("합계: " + rowGroups.size() + "행")
                .build();

        return PivotQueryResponseDTO.builder()
                .columnField(
                        PivotQueryResponseDTO.ColumnField.builder()
                                .name(request.getColumn().getField())
                                .values(columnValues)
                                .metrics(
                                        request.getValues().stream()
                                                .map(v ->
                                                        PivotQueryResponseDTO.Metric.builder()
                                                                .alias(v.getAlias())
                                                                .field(v.getField())
                                                                .agg(v.getAgg())
                                                                .build()
                                                )
                                                .toList()
                                )
                                .build()
                )
                .rowGroups(rowGroups)
                .summary(summary)
                .build();
    }

    private void validateRequest(PivotQueryRequestDTO request) {
        // TODO: 나중에 필드 유효성 체크, 중복 금지 등 추가하기
        // 레이어 유효성
        // column 1개인지
        // rows/values 중복 필드 없는지
        // values의 agg 허용 가능 여부 등
    }

    private PivotRepository.TimeWindow resolveTimeWindow(PivotQueryRequestDTO request) {
        // 1) now를 OffsetDateTime으로 파싱
        //    예: "2025-10-27T07:20:00.000Z"
        OffsetDateTime nowOffset = OffsetDateTime.parse(
                request.getTimeRange().getNow(),
                DateTimeFormatter.ISO_OFFSET_DATE_TIME
        );

        OffsetDateTime fromOffset;
        OffsetDateTime toOffset;

        if ("preset".equals(request.getTimeRange().getType())) {
            String v = request.getTimeRange().getValue(); // "1h", "2h", "24h", "1w"
            Duration dur;
            switch (v) {
                case "1h" -> dur = Duration.ofHours(1);
                case "2h" -> dur = Duration.ofHours(2);
                case "24h" -> dur = Duration.ofHours(24);
                case "1w" -> dur = Duration.ofDays(7);
                default -> throw new IllegalArgumentException("Unsupported preset: " + v);
            }
            toOffset = nowOffset;
            fromOffset = nowOffset.minus(dur);

        } else { // "custom"
            // customRange.from / to 는 ISO 문자열이라고 가정
            String fromStr = request.getCustomRange().getFrom();
            String toStr = request.getCustomRange().getTo();

            // 예: "2025-10-27T00:00:00.000Z"
            OffsetDateTime parsedFrom = OffsetDateTime.parse(fromStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            OffsetDateTime parsedTo   = OffsetDateTime.parse(toStr,   DateTimeFormatter.ISO_OFFSET_DATE_TIME);

            fromOffset = parsedFrom;
            toOffset = parsedTo;
        }

        ZoneId zone = ZoneId.of("UTC");

        LocalDateTime fromLdt = fromOffset.atZoneSameInstant(zone).toLocalDateTime();
        LocalDateTime toLdt   = toOffset.atZoneSameInstant(zone).toLocalDateTime();

        return new PivotRepository.TimeWindow(fromLdt, toLdt);
    }
}
