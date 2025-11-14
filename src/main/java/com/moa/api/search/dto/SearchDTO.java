package com.moa.api.search.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchDTO {
    private String layer;              // 예: HTTP_PAGE
    private List<String> columns;      // FieldPicker에서 선택한 컬럼들
    private TimeSpec time;             // 기간
    private Boolean not;               // 전체 NOT (Boolean으로 변경)
    private List<Condition> conditions; // 조건들
    private Options options;           // 정렬/리밋 등

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSpec {
        private String field;      // 예: ts_server
        private Long fromEpoch;    // 초 단위 epoch
        private Long toEpoch;      // 초 단위 epoch
        private Boolean inclusive; // 기본 true
    }

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Condition {
        private String join;            // AND | OR (첫 조건은 null)
        private String field;           // 필드 키
        private String op;              // EQ | BETWEEN | LIKE | IN | IN_CIDR ...
        private List<Object> values;    // 값들 (Number, String, Boolean 등)
        private String dataType;        // TEXT | NUMBER | IP | BOOLEAN

        // TEXT 타입 전용 필드들
        private String pattern;         // contains | prefix | suffix | exact
        private Boolean caseSensitive;  // false면 ILIKE, true면 LIKE
    }

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Options {
        private String orderBy;  // 정렬 컬럼
        private String order;    // ASC | DESC
        private Integer limit;   // 기본 100 (1~1000 가드)
        private Integer offset;
    }

    // 응답용 필드들
    private List<Map<String, Object>> rows;
    private Integer total; // 전체 건수

    // 응답용 생성자
    public SearchDTO(List<Map<String, Object>> rows, Integer total) {
        this.rows = rows;
        this.total = total;
    }
}