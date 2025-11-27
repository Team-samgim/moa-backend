package com.moa.api.grid.util.condition;

import com.moa.api.grid.dto.SqlDTO;
import com.moa.api.grid.util.SqlIdentifier;

import java.util.List;

/**
 * 숫자 타입 필드용 조건 빌더
 *
 * AUTHOR        : 방대혁
 *
 * 역할
 * - 프론트에서 type 이 "number" 로 지정된 컬럼에 대한 조건절을 생성한다.
 * - 기본 연산자:
 *   - equals, = : 정확히 같은 값
 *   - >, <, >=, <= : 크기 비교
 *   - contains : 숫자를 문자열로 캐스팅해서 부분 문자열 검색
 */
public class NumberConditionBuilder implements FieldConditionBuilder {

    /**
     * 이 빌더가 처리 가능한 타입인지 여부
     *
     * @param type 프론트 기준 타입 (예: "number")
     * @return "number" 인 경우 true
     */
    @Override
    public boolean supports(String type) {
        return "number".equalsIgnoreCase(type);
    }

    /**
     * 숫자 필드에 대한 WHERE 조건 생성
     *
     * @param alias          테이블 별칭 (예: "t")
     * @param field          컬럼명 (예: "res_len")
     * @param op             연산자 (예: "=", ">", "<", ">=", "<=", "equals", "contains")
     * @param val            비교 값 (문자열로 들어온 숫자)
     * @param rawTemporalKind 시간 타입 정보 (숫자 필드는 사용하지 않음)
     * @return SqlDTO(sql, args) / 지원하지 않는 op 는 SqlDTO.empty()
     *
     * 사용되는 패턴
     * - equals, = : f = ?
     * - >, <, >=, <= : f [op] ?
     * - contains : f::text ILIKE '%val%'
     */
    @Override
    public SqlDTO build(String alias, String field, String op, String val, String rawTemporalKind) {
        String f = SqlIdentifier.quoteWithAlias(alias, field);
        Number n = parseNum(val);

        return switch (op) {
            case "=", "equals" ->
                    SqlDTO.of(f + " = ?", List.of(n));
            case ">", "<", ">=", "<=" ->
                    SqlDTO.of(f + " " + op + " ?", List.of(n));
            case "contains" ->
                    SqlDTO.of(f + "::text ILIKE ?", List.of("%" + val + "%"));
            default ->
                    SqlDTO.empty();
        };
    }

    /**
     * 문자열을 Number 로 파싱
     *
     * 규칙
     * - null 이면 0 반환
     * - "." 이 포함되어 있으면 Double, 아니면 Long 으로 파싱
     * - 파싱 실패 시 0 반환 (예: "abc" 등)
     */
    private Number parseNum(String s) {
        if (s == null) return 0;
        try {
            return s.contains(".") ? Double.valueOf(s) : Long.valueOf(s);
        } catch (Exception e) {
            return 0;
        }
    }
}
