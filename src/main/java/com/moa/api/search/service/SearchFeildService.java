package com.moa.api.search.service;

import com.moa.api.search.dto.FieldDTO;
import com.moa.api.search.dto.FieldWithOpsDTO;
import com.moa.api.search.dto.OperatorDTO;
import com.moa.api.search.entity.HttpPageField;
import com.moa.api.search.registry.DataType;
import com.moa.api.search.registry.OpCode;
import com.moa.api.search.repository.HttpPageFieldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchFeildService {

    private final HttpPageFieldRepository httpPageFieldRepository;
    private final JdbcTemplate jdbcTemplate;

    // (A) 타입별 연산자: 코드 고정(소스 오브 트루스)
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

    // (B) 코드 기본 필드 목록 (DB 비었을 때 폴백)
    private static final Map<String, List<FieldDTO>> FIELDS_BY_LAYER = Map.of(
            "HTTP_PAGE", List.of(
                    new FieldDTO("src_ip", "src_ip", DataType.IP.name(),     10),
                    new FieldDTO("dst_port", "dst_port", DataType.NUMBER.name(), 20),
                    new FieldDTO("src_port", "src_port", DataType.NUMBER.name(), 30),
                    new FieldDTO("res_code_2xx_cnt", "res_code_2xx_cnt", DataType.NUMBER.name(), 40)
            )
            // 다른 레이어는 필요시 추가
    );

    // === Public APIs ===
    public List<FieldWithOpsDTO> listFieldsWithOperators(String layer) {
        List<FieldDTO> fields = listFields(layer);

        return fields.stream().map(f -> {
            DataType dt = parseType(f.getDataType());
            String normalizedType = dt.name();
            String label = (f.getLabel() == null || f.getLabel().isBlank()) ? f.getKey() : f.getLabel();

            List<OperatorDTO> ops = OPS.getOrDefault(dt, List.of()).stream()
                    .sorted(Comparator.comparingInt(OperatorDTO::getOrderNo))
                    .toList();

            return new FieldWithOpsDTO(
                    f.getKey(),
                    label,
                    normalizedType,
                    f.getOrderNo(),
                    ops
            );
        }).toList();
    }

    public List<FieldDTO> listFields(String layer) {
        // 0) 탭(layer) → 실제 테이블명 매핑
        String table = resolveTableByLayer(layer);

        // 1) information_schema에서 컬럼을 읽어 필드 생성 (레이어 우선)
        if (table != null) {
            List<FieldDTO> fromTable = listFieldsFromTable(table);
            if (!fromTable.isEmpty()) {
                return fromTable;
            }
        }

        // 2) HTTP_PAGE는 기존 메타 테이블(http_page_fields) 폴백
        if ("HTTP_PAGE".equalsIgnoreCase(layer)) {
            List<HttpPageField> rows = httpPageFieldRepository.findAllByOrderByFieldKeyAsc();
            if (!rows.isEmpty()) {
                final int[] order = {0};
                return rows.stream()
                        .map(r -> new FieldDTO(
                                r.getFieldKey(),
                                r.getFieldKey(), // label 기본값 = key
                                parseType(r.getDataType()).name(),
                                order[0] += 10
                        ))
                        .collect(Collectors.toList());
            }
        }

        // 3) 코드 고정 기본값 최종 폴백
        return FIELDS_BY_LAYER.getOrDefault(layer, List.of());
    }

    public Map<String, List<OperatorDTO>> operatorsByType() {
        Map<String, List<OperatorDTO>> map = new LinkedHashMap<>();
        OPS.forEach((dt, ops) -> map.put(
                dt.name(),
                ops.stream().sorted(Comparator.comparingInt(OperatorDTO::getOrderNo)).toList()
        ));
        return map;
    }

    // === Helpers ===
    private DataType parseType(String raw) {
        if (raw == null) return DataType.TEXT;
        try {
            return DataType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return DataType.TEXT;
        }
    }

    private String resolveTableByLayer(String layer) {
        if (layer == null) return null;
        String L = layer.trim().toUpperCase(Locale.ROOT);
        return switch (L) {
            case "HTTP_PAGE" -> "http_page_sample";
            case "ETHERNET"  -> "ethernet_sample";
            default -> null; // 혹은 예외 throw
        };
    }

    private List<FieldDTO> listFieldsFromTable(String tableName) {
        final String sql = """
            select column_name, data_type, ordinal_position
            from information_schema.columns
            where table_schema = 'public' and table_name = ?
            order by ordinal_position
        """;
        final int[] order = {0};
        return jdbcTemplate.query(
                sql,
                (rs, i) -> {
                    String key = rs.getString("column_name");
                    String pgType = rs.getString("data_type");
                    DataType dt = mapPgTypeToDataType(pgType);
                    return new FieldDTO(
                            key,
                            key,                 // label 기본값 = key
                            dt.name(),
                            order[0] += 10       // 화면 정렬용 10단위 증가
                    );
                },
                tableName
        );
    }

    private DataType mapPgTypeToDataType(String pgType) {
        if (pgType == null) return DataType.TEXT;
        String t = pgType.toLowerCase(Locale.ROOT);
        switch (t) {
            // 숫자형
            case "integer":
            case "bigint":
            case "smallint":
            case "numeric":
            case "real":
            case "double precision":
            case "decimal":
                return DataType.NUMBER;
            // 불리언
            case "boolean":
                return DataType.BOOLEAN;
            // 시간/날짜
            case "timestamp without time zone":
            case "timestamp with time zone":
            case "date":
            case "time without time zone":
            case "time with time zone":
                return DataType.DATETIME;
            // 네트워크
            case "inet":
                return DataType.IP;
            // 텍스트류
            case "character varying":
            case "character":
            case "text":
            default:
                return DataType.TEXT;
        }
    }
}