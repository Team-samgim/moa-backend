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
    private String layer;     // 예: HTTP_PAGE
    private String source;    // 예: http_page_sample (실제 테이블명)
    private TimeSpec time;    // 기간
    private boolean not;      // 전체 NOT
    private List<Condition> conditions; // 조건들
    private Options options;  // 정렬/리밋 등

    @Getter @Setter
    public static class TimeSpec {
        private String field;     // 예: ts_server
        private Long fromEpoch;   // 초 단위 epoch
        private Long toEpoch;     // 초 단위 epoch
        private Boolean inclusive; // 기본 true
    }

    @Getter @Setter
    public static class Condition {
        private String join;       // AND | OR (첫 조건은 null 가능)
        private String field;      // 필드 키
        private String op;         // EQ | BETWEEN | LIKE | IN_CIDR ...
        private List<String> values;  // 값들(문자열로 받고 서버에서 캐스팅)
        private String dataType;   // 프론트가 보내는 힌트(검증은 서버가 DB로 함)
    }

    @Getter
    @Setter
    public static class Options {
        private String orderBy;  // 정렬 컬럼
        private String order;    // ASC | DESC
        private Integer limit;   // 기본 100 (1~1000 가드)
    }


    private List<Map<String, Object>> rows;
    private Integer total; // 선택: 전체 건수(간단히 동일 WHERE로 count)

    public SearchDTO(List<Map<String, Object>> rows, Integer total) {
        this.rows = rows;
        this.total = total;
    }
}
