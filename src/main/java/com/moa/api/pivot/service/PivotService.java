package com.moa.api.pivot.service;

import com.moa.api.pivot.dto.PivotFieldsResponseDTO;
import com.moa.api.pivot.dto.PivotQueryRequestDTO;
import com.moa.api.pivot.dto.PivotQueryResponseDTO;
import com.moa.api.pivot.repository.PivotRepository;
import com.moa.api.pivot.repository.PivotRepository.TimeWindow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
        // 1. validate
        validateRequest(request);

        // 2. resolve time window
        TimeWindow tw = resolveTimeWindow(request);

        // 3. column distinct 상위 값 뽑기
        List<String> columnValues = pivotRepository.findTopColumnValues(
                request.getLayer(),
                request.getColumn().getField(),
                request.getFilters(),
                tw
        );

        // 4. rowGroups 빌드 (각 row 필드별 요약 라인)
        var rowGroups = pivotRepository.buildRowGroups(
                request.getLayer(),
                request.getRows(),
                request.getValues(),
                request.getColumn().getField(),
                columnValues,
                request.getFilters(),
                tw
        );

        // 5. summary 만들기
        PivotQueryResponseDTO.Summary summary = PivotQueryResponseDTO.Summary.builder()
                .rowCountText("합계: " + rowGroups.size() + "행")
                .build();

        // 6. 최종 응답 조립
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
        // 레이어 유효성
        // column 1개인지
        // rows/values 중복 필드 없는지
        // values의 agg 허용 가능 여부 등
    }

    private TimeWindow resolveTimeWindow(PivotQueryRequestDTO request) {
        return new TimeWindow("2025-10-27T00:23:45+09:00", "2025-10-27T01:23:45+09:00");
    }
}
