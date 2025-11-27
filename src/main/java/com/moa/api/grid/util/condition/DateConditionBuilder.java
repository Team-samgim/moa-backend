package com.moa.api.grid.util.condition;

import com.moa.api.grid.dto.SqlDTO;
import com.moa.api.grid.util.SqlIdentifier;
import com.moa.api.grid.util.TemporalExprFactory;

import java.util.List;

/**
 * 날짜 타입 필드용 조건 빌더
 *
 * AUTHOR        : 방대혁
 *
 * 역할
 * - 프론트에서 type = "date" 로 정의된 필드에 대한 WHERE 조건을 만든다.
 * - TemporalExprFactory 를 사용해서 KST 기준 날짜 표현식(kstDate)을 생성한 뒤
 *   equals / before / after / between 같은 날짜 비교 연산을 처리한다.
 */
public class DateConditionBuilder implements FieldConditionBuilder {

    /** timestamp/timestamptz 를 KST 기준 date 표현식으로 만들어주는 팩토리 */
    private final TemporalExprFactory temporal;

    public DateConditionBuilder(TemporalExprFactory temporal) {
        this.temporal = temporal;
    }

    /**
     * 이 빌더가 처리할 타입인지 여부
     *
     * @param type 프론트 기준 타입 (number/string/date/...)
     * @return "date" (대소문자 무시) 인 경우에만 true
     */
    @Override
    public boolean supports(String type) {
        return "date".equalsIgnoreCase(type);
    }

    /**
     * 날짜 타입 조건 SQL 생성
     *
     * @param alias          테이블 별칭 (예: "t")
     * @param field          컬럼명
     * @param op             연산자 (equals, before, after, between, contains)
     * @param val            값 (날짜 문자열, between 의 경우 "from,to" 형태)
     * @param rawTemporalKind 실제 컬럼의 타임스탬프 종류(timestamp, timestamptz, date 등)
     * @return SqlDTO(sql, args)
     *
     * 동작:
     * - temporal.toDateExpr 를 통해 KST 기준 date 표현식(kstDate)을 만든다.
     * - 날짜 비교 연산자는 kstDate 를 기준으로 수행하고, 파라미터는 ?::date 로 바인딩한다.
     * - contains 는 원본 컬럼을 text 로 캐스팅하여 부분 문자열 검색을 수행한다.
     */
    @Override
    public SqlDTO build(String alias, String field, String op, String val, String rawTemporalKind) {
        // KST 기준 date 표현식 (예: (t.ts_server at time zone 'Asia/Seoul')::date)
        String kstDate = temporal.toDateExpr(alias, field, rawTemporalKind);

        // 원본 컬럼 (필요 시 텍스트 검색용)
        String rawCol = SqlIdentifier.quoteWithAlias(alias, field);

        return switch (op) {
            // 같은 날짜
            case "equals" -> SqlDTO.of(kstDate + " = ?::date", List.of(val));

            // 기준 날짜 이전
            case "before" -> SqlDTO.of(kstDate + " < ?::date", List.of(val));

            // 기준 날짜 이후
            case "after" -> SqlDTO.of(kstDate + " > ?::date", List.of(val));

            // 시작~끝 사이 (포함)
            // val 형식: "2025-01-01,2025-01-31"
            case "between" -> {
                String[] parts = val.split(",");
                if (parts.length == 2) {
                    yield SqlDTO.of(
                            kstDate + " BETWEEN ?::date AND ?::date",
                            List.of(parts[0].trim(), parts[1].trim())
                    );
                } else {
                    // 형식이 맞지 않으면 조건 생성 안함
                    yield SqlDTO.empty();
                }
            }

            // 날짜를 문자열로 바꿔서 부분 검색 (보조적 용도)
            case "contains" ->
                    SqlDTO.of(rawCol + "::text ILIKE ?", List.of("%" + val + "%"));

            // 지원하지 않는 연산자
            default -> SqlDTO.empty();
        };
    }
}
