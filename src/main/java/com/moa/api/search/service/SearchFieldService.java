package com.moa.api.search.service;

import com.moa.api.search.dto.FieldWithOpsDTO;
import com.moa.api.search.dto.OperatorDTO;
import com.moa.api.search.registry.DataType;
import com.moa.api.search.registry.OpCode;
import jakarta.persistence.EntityManager;
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

    private final EntityManager entityManager;

    // 타입별 연산자 정의
    private static final Map<DataType, List<OperatorDTO>> OPS = Map.of(
            DataType.TEXT, List.of(
                    new OperatorDTO(OpCode.LIKE.name(), "포함", 1, true,  10),
                    new OperatorDTO(OpCode.EQ.name(),   "=",    1, false, 20),
                    new OperatorDTO(OpCode.NE.name(),   "!=",   1, false, 30),
                    new OperatorDTO(OpCode.STARTS_WITH.name(),"시작", 1,false,40),
                    new OperatorDTO(OpCode.ENDS_WITH.name(),  "끝남", 1,false,50),
                    new OperatorDTO(OpCode.IN.name(),   "목록", -1,false,60),
                    new OperatorDTO(OpCode.IS_NULL.name(),    "값 없음",0,false,70),
                    new OperatorDTO(OpCode.IS_NOT_NULL.name(),"값 있음",0,false,80)
            ),
            DataType.NUMBER, List.of(
                    new OperatorDTO(OpCode.EQ.name(),   "=",   1, true, 10),
                    new OperatorDTO(OpCode.NE.name(),   "!=",  1,false, 20),
                    new OperatorDTO(OpCode.GT.name(),   ">",   1,false, 30),
                    new OperatorDTO(OpCode.GTE.name(),  ">=",  1,false, 40),
                    new OperatorDTO(OpCode.LT.name(),   "<",   1,false, 50),
                    new OperatorDTO(OpCode.LTE.name(),  "<=",  1,false, 60),
                    new OperatorDTO(OpCode.BETWEEN.name(),"범위",2,false,70),
                    new OperatorDTO(OpCode.IN.name(),   "목록",-1,false,80),
                    new OperatorDTO(OpCode.IS_NULL.name(),    "값 없음",0,false,90),
                    new OperatorDTO(OpCode.IS_NOT_NULL.name(),"값 있음",0,false,100)
            ),
            DataType.IP, List.of(
                    new OperatorDTO(OpCode.EQ.name(),   "=",   1,true, 10),
                    new OperatorDTO(OpCode.LIKE.name(), "포함",1,false,20),
                    new OperatorDTO(OpCode.IN.name(),   "목록",-1,false,30),
                    new OperatorDTO(OpCode.IS_NULL.name(),    "값 없음",0,false,40),
                    new OperatorDTO(OpCode.IS_NOT_NULL.name(),"값 있음",0,false,50)
            ),
            DataType.DATETIME, List.of(
                    new OperatorDTO(OpCode.GTE.name(),  "이후(≥)",1,true, 10),
                    new OperatorDTO(OpCode.LT.name(),   "이전(<)",1,false,20),
                    new OperatorDTO(OpCode.BETWEEN.name(),"범위", 2,false,30),
                    new OperatorDTO(OpCode.IN.name(),   "목록", -1,false,40),
                    new OperatorDTO(OpCode.IS_NULL.name(),    "값 없음",0,false,50),
                    new OperatorDTO(OpCode.IS_NOT_NULL.name(),"값 있음",0,false,60)
            ),
            DataType.BOOLEAN, List.of(
                    new OperatorDTO(OpCode.IS_NULL.name(),    "값 없음",0,false,10),
                    new OperatorDTO(OpCode.IS_NOT_NULL.name(),"값 있음",0,false,20)
            )
    );

    /**
     * 레이어별 필드 메타 테이블명 매핑
     * 예: HTTP_PAGE -> http_page_fields
     */
    private String resolveMetaTableByLayer(String layer) {
        if (layer == null || layer.isBlank()) {
            return null;
        }
        String normalized = layer.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "HTTP_PAGE" -> "http_page_fields";
            case "HTTP_URI"  -> "http_uri_fields";
            case "TCP"       -> "tcp_fields";
            case "ETHERNET"  -> "ethernet_fields";
            // 추가 레이어는 여기에 매핑
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

        // 메타 테이블이 존재하는지 확인
        if (!tableExists(metaTable)) {
            log.error("Meta table does not exist: {}", metaTable);
            return Collections.emptyList();
        }

        // 메타 테이블에서 필드 정보 조회
        List<FieldMetaInfo> metaInfos = loadFieldMetaInfo(metaTable);

        if (metaInfos.isEmpty()) {
            log.warn("No fields found in meta table: {}", metaTable);
            return Collections.emptyList();
        }

        // FieldWithOpsDTO 생성
        return metaInfos.stream().map(meta -> {
                    DataType dt = parseDataType(meta.dataType);
                    String normalizedType = dt.name();

                    // label이 없으면 key 사용
                    String label = (meta.label == null || meta.label.isBlank())
                            ? meta.fieldKey
                            : meta.label;

                    // labelKo가 없으면 label 사용
                    String labelKo = (meta.labelKo == null || meta.labelKo.isBlank())
                            ? label
                            : meta.labelKo;

                    // 데이터 타입에 맞는 연산자 목록
                    List<OperatorDTO> ops = OPS.getOrDefault(dt, Collections.emptyList())
                            .stream()
                            .sorted(Comparator.comparingInt(OperatorDTO::getOrderNo))
                            .toList();

                    return new FieldWithOpsDTO(
                            meta.fieldKey,
                            label,
                            labelKo,
                            normalizedType,
                            meta.isInfo,
                            meta.orderNo,
                            ops
                    );
                })
                .sorted(Comparator.comparingInt(f ->
                        f.getOrderNo() != null ? f.getOrderNo() : Integer.MAX_VALUE))
                .collect(Collectors.toList());
    }

    /**
     * 타입별 연산자 목록 반환 (참고용)
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

    /**
     * 메타 테이블에서 필드 정보 조회 (JPA EntityManager 사용)
     * 테이블 구조: field_key, data_type, label_ko, is_info
     */
    private List<FieldMetaInfo> loadFieldMetaInfo(String metaTable) {
        String sql = String.format("""
            SELECT 
                field_key,
                data_type,
                label_ko,
                COALESCE(is_info, false) as is_info
            FROM %s
            ORDER BY field_key
        """, metaTable);

        try {
            @SuppressWarnings("unchecked")
            List<Object[]> results = entityManager.createNativeQuery(sql).getResultList();

            List<FieldMetaInfo> metaInfos = new ArrayList<>();
            int rowNum = 0;

            for (Object[] row : results) {
                FieldMetaInfo info = new FieldMetaInfo();
                info.fieldKey = (String) row[0];
                info.dataType = (String) row[1];
                info.labelKo = (String) row[2];
                info.isInfo = (Boolean) row[3];
                // label은 field_key 사용, orderNo는 rowNum 기반
                info.label = info.fieldKey;
                info.orderNo = (++rowNum) * 10;

                metaInfos.add(info);
            }

            log.info("Loaded {} fields from {} using JPA EntityManager", metaInfos.size(), metaTable);
            return metaInfos;

        } catch (Exception e) {
            log.error("Error loading field meta info from {}: {}", metaTable, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 테이블 존재 여부 확인 (JPA EntityManager 사용)
     */
    private boolean tableExists(String tableName) {
        String sql = """
            SELECT EXISTS (
                SELECT FROM information_schema.tables 
                WHERE table_schema = 'public' 
                AND table_name = :tableName
            )
        """;

        try {
            Boolean exists = (Boolean) entityManager.createNativeQuery(sql)
                    .setParameter("tableName", tableName)
                    .getSingleResult();
            return exists != null && exists;
        } catch (Exception e) {
            log.error("Error checking table existence for {}: {}", tableName, e.getMessage());
            return false;
        }
    }

    /**
     * 문자열을 DataType enum으로 변환
     */
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

    /**
     * 메타 테이블 필드 정보를 담는 내부 클래스
     */
    private static class FieldMetaInfo {
        String fieldKey;
        String label;
        String labelKo;
        String dataType;
        boolean isInfo;
        Integer orderNo;
    }
}