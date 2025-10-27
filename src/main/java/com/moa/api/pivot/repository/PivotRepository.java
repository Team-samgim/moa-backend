package com.moa.api.pivot.repository;

import com.moa.api.pivot.dto.PivotQueryRequestDTO;
import com.moa.api.pivot.dto.PivotQueryResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class PivotRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private String resolveTable(String layer) {
        return switch (layer) {
            case "HTTP_PAGE" -> "page_sample";
            case "ETHERNET"  -> "ethernet_sample";
            case "TCP"       -> "tcp_sample";
            case "HTTP_URI"  -> "uri_sample";
            default -> throw new IllegalArgumentException("Unsupported layer: " + layer);
        };
    }

    public List<String> findColumnNamesForLayer(String layer) {
        String tableName = resolveTable(layer);

        String sql = """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = :tableName
            ORDER BY ordinal_position
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("tableName", tableName);

        return jdbc.query(
                sql,
                params,
                (rs, i) -> rs.getString("column_name")
        );
    }

    // column 축으로 쓸 필드의 상위 값들 뽑기 (ex: src_ip 상위 N개)
    public List<String> findTopColumnValues(
            String layer,
            String columnField,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow timeWindow
    ) {
        // - layer -> 테이블명 매핑
        // - filters -> WHERE 절
        // - timeWindow -> created_at BETWEEN ...
        // - DISTINCT columnField ORDER BY count(*) DESC LIMIT N
        return List.of("ip_num_1", "ip_num_2");
    }

    // rows[] 각각에 대해 RowGroup을 만들어서 반환
    public List<PivotQueryResponseDTO.RowGroup> buildRowGroups(
            String layer,
            List<PivotQueryRequestDTO.RowDef> rows,
            List<PivotQueryRequestDTO.ValueDef> values,
            String columnField,
            List<String> columnValues,
            List<PivotQueryRequestDTO.FilterDef> filters,
            TimeWindow timeWindow
    ) {
        // - 각 rowDef.field별로 집계 실행
        // - columnValues 루프 돌면서 metric 값(합계:total, 평균:rating...) 가져오기
        // - RowGroup 형태로 매핑
        //
        // TODO: 테스트 하드 코드 제거

        return List.of(
            PivotQueryResponseDTO.RowGroup.builder()
                .rowLabel("row_key")
                .rowInfo(
                    PivotQueryResponseDTO.RowInfo.builder()
                        .count(12)
                        .build()
                )
                .cells(
                    Map.of(
                        "ip_num_1", Map.of("합계: total", 103500, "평균: rating", 4.5),
                        "ip_num_2", Map.of("합계: total", 83430, "평균: rating", 3.2)
                    )
                )
                .build(),
            PivotQueryResponseDTO.RowGroup.builder()
                .rowLabel("vlan_id")
                .rowInfo(
                    PivotQueryResponseDTO.RowInfo.builder()
                        .count(5)
                        .build()
                )
                .cells(
                    Map.of(
                        "ip_num_1", Map.of("합계: total", 103500, "평균: rating", 4.5),
                        "ip_num_2", Map.of("합계: total", 83430, "평균: rating", 3.2)
                    )
                )
                .build()
        );
    }

    public static class TimeWindow {
        public final String from;
        public final String to;

        public TimeWindow(String from, String to) {
            this.from = from;
            this.to = to;
        }
    }
}
