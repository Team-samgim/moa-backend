/**
 * 작성자: 정소영
 */
package com.moa.api.search.service;

import com.moa.api.search.dto.FieldWithOpsDTO;
import com.moa.api.search.dto.OperatorDTO;
import com.moa.api.search.entity.LayerFieldMeta;
import com.moa.api.search.registry.DataType;
import com.moa.api.search.registry.OpCode;
import com.moa.api.search.repository.LayerFieldMetaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchFieldService {

    private final LayerFieldMetaRepository repository;

    // 타입별 연산자 정의 (기존과 동일)
    private static final Map<DataType, List<OperatorDTO>> OPS = Map.of(
            DataType.TEXT, List.of(
                    new OperatorDTO(OpCode.LIKE.name(), "포함", 1, true, 10),
                    new OperatorDTO(OpCode.EQ.name(), "=", 1, false, 20),
                    new OperatorDTO(OpCode.NE.name(), "!=", 1, false, 30),
                    new OperatorDTO(OpCode.STARTS_WITH.name(), "시작", 1, false, 40),
                    new OperatorDTO(OpCode.ENDS_WITH.name(), "끝남", 1, false, 50),
                    new OperatorDTO(OpCode.IN.name(), "목록", -1, false, 60),
                    new OperatorDTO(OpCode.IS_NULL.name(), "값 없음", 0, false, 70),
                    new OperatorDTO(OpCode.IS_NOT_NULL.name(), "값 있음", 0, false, 80)
            ),
            DataType.NUMBER, List.of(
                    new OperatorDTO(OpCode.EQ.name(), "=", 1, true, 10),
                    new OperatorDTO(OpCode.NE.name(), "!=", 1, false, 20),
                    new OperatorDTO(OpCode.GT.name(), ">", 1, false, 30),
                    new OperatorDTO(OpCode.GTE.name(), ">=", 1, false, 40),
                    new OperatorDTO(OpCode.LT.name(), "<", 1, false, 50),
                    new OperatorDTO(OpCode.LTE.name(), "<=", 1, false, 60),
                    new OperatorDTO(OpCode.BETWEEN.name(), "범위", 2, false, 70),
                    new OperatorDTO(OpCode.IN.name(), "목록", -1, false, 80),
                    new OperatorDTO(OpCode.IS_NULL.name(), "값 없음", 0, false, 90),
                    new OperatorDTO(OpCode.IS_NOT_NULL.name(), "값 있음", 0, false, 100)
            ),
            DataType.IP, List.of(
                    new OperatorDTO(OpCode.EQ.name(), "=", 1, true, 10),
                    new OperatorDTO(OpCode.LIKE.name(), "포함", 1, false, 20),
                    new OperatorDTO(OpCode.IN.name(), "목록", -1, false, 30),
                    new OperatorDTO(OpCode.IS_NULL.name(), "값 없음", 0, false, 40),
                    new OperatorDTO(OpCode.IS_NOT_NULL.name(), "값 있음", 0, false, 50)
            ),
            DataType.DATETIME, List.of(
                    new OperatorDTO(OpCode.GTE.name(), "이후(≥)", 1, true, 10),
                    new OperatorDTO(OpCode.LT.name(), "이전(<)", 1, false, 20),
                    new OperatorDTO(OpCode.BETWEEN.name(), "범위", 2, false, 30),
                    new OperatorDTO(OpCode.IN.name(), "목록", -1, false, 40),
                    new OperatorDTO(OpCode.IS_NULL.name(), "값 없음", 0, false, 50),
                    new OperatorDTO(OpCode.IS_NOT_NULL.name(), "값 있음", 0, false, 60)
            ),
            DataType.BOOLEAN, List.of(
                    new OperatorDTO(OpCode.IS_NULL.name(), "값 없음", 0, false, 10),
                    new OperatorDTO(OpCode.IS_NOT_NULL.name(), "값 있음", 0, false, 20)
            )
    );

    /**
     * 레이어별 필드 메타 테이블명 매핑
     */
    private String resolveMetaTableByLayer(String layer) {
        if (layer == null || layer.isBlank()) {
            return null;
        }
        String normalized = layer.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "HTTP_PAGE" -> "http_page_fields";
            case "HTTP_URI" -> "http_uri_fields";
            case "TCP" -> "tcp_fields";
            case "ETHERNET" -> "ethernet_fields";
            default -> {
                log.warn("Unknown layer: {}. No meta table mapping found.", layer);
                yield null;
            }
        };
    }

    /**
     * 메인 API: 레이어의 필드 + 연산자 목록 반환
     */
    public List<FieldWithOpsDTO> listFieldsWithOperators(String layer) {
        String metaTable = resolveMetaTableByLayer(layer);
        if (metaTable == null) {
            log.error("No meta table found for layer: {}", layer);
            return Collections.emptyList();
        }

        // Repository를 통한 테이블 존재 확인
        if (!repository.existsTable(metaTable)) {
            log.error("Meta table does not exist: {}", metaTable);
            return Collections.emptyList();
        }

        // Repository를 통한 필드 조회
        List<LayerFieldMeta> metaList = repository.findAllByTableName(metaTable);

        if (metaList.isEmpty()) {
            log.warn("No fields found in meta table: {}", metaTable);
            return Collections.emptyList();
        }

        // DTO 변환
        return buildFieldWithOpsDTOs(metaList);
    }

    /**
     * 타입별 연산자 목록 반환
     */
    public Map<String, List<OperatorDTO>> operatorsByType() {
        Map<String, List<OperatorDTO>> map = new LinkedHashMap<>();
        OPS.forEach((dt, ops) -> map.put(
                dt.name(),
                ops.stream()
                        .sorted(Comparator.comparingInt(OperatorDTO::getOrderNo))
                        .toList()
        ));
        return map;
    }

    // === Private Helper Methods ===

    private List<FieldWithOpsDTO> buildFieldWithOpsDTOs(List<LayerFieldMeta> metaList) {
        int[] orderCounter = {0};

        return metaList.stream()
                .map(meta -> {
                    DataType dt = parseDataType(meta.getDataType());
                    String normalizedType = dt.name();

                    String label = (meta.getFieldKey() != null) ? meta.getFieldKey() : "";
                    String labelKo = (meta.getLabelKo() != null && !meta.getLabelKo().isBlank())
                            ? meta.getLabelKo()
                            : label;

                    List<OperatorDTO> ops = OPS.getOrDefault(dt, Collections.emptyList())
                            .stream()
                            .sorted(Comparator.comparingInt(OperatorDTO::getOrderNo))
                            .toList();

                    return new FieldWithOpsDTO(
                            meta.getFieldKey(),
                            label,
                            labelKo,
                            normalizedType,
                            meta.getIsInfo() != null && meta.getIsInfo(),
                            (++orderCounter[0]) * 10,
                            ops
                    );
                })
                .sorted(Comparator.comparingInt(f ->
                        f.getOrderNo() != null ? f.getOrderNo() : Integer.MAX_VALUE))
                .collect(Collectors.toList());
    }

    private DataType parseDataType(String raw) {
        if (raw == null || raw.isBlank()) {
            return DataType.TEXT;
        }
        try {
            return DataType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            log.warn("Unknown data type: {}, defaulting to TEXT", raw);
            return DataType.TEXT;
        }
    }
}