package com.moa.api.grid.util.condition;

import com.moa.api.grid.dto.SqlDTO;
import com.moa.api.grid.util.SqlIdentifier;

import java.util.List;

/**
 * 텍스트(string) 타입 필드용 조건 빌더
 *
 * AUTHOR        : 방대혁
 *
 * 역할
 * - 프론트에서 type 이 "string" 이거나 지정되지 않은 컬럼에 대한 WHERE 조건을 생성한다.
 * - 기본 지원 연산자:
 *   - contains    : 부분 문자열 포함 (ILIKE %val%)
 *   - equals      : 완전 일치 (=)
 *   - startsWith  : 접두사 일치 (val%)
 *   - endsWith    : 접미사 일치 (%val)
 *
 * 특징
 * - DB 컬럼을 항상 ::text 로 캐스팅해서 문자열 기준으로 비교한다.
 * - 대소문자 무시 검색을 위해 ILIKE 사용(PostgreSQL).
 * - 지원하지 않는 op 는 SqlDTO.empty() 를 반환하여 상위 로직에서 무시되도록 한다.
 */
public class TextConditionBuilder implements FieldConditionBuilder {

    /**
     * 이 빌더가 처리 가능한 타입인지 여부
     *
     * @param type 프론트 기준 타입 (예: "string")
     * @return type 이 null, blank 이거나 "string" 인 경우 true
     */
    @Override
    public boolean supports(String type) {
        return type == null || type.isBlank() || "string".equalsIgnoreCase(type);
    }

    /**
     * 텍스트 필드용 WHERE 조건 생성
     *
     * @param alias          테이블 별칭 (예: "t")
     * @param field          컬럼명 (예: "http_host")
     * @param op             연산자 (contains, equals, startsWith, endsWith)
     * @param val            비교 값
     * @param rawTemporalKind 시간 타입 정보 (문자열 필드는 사용하지 않음)
     * @return SqlDTO(sql, args) / 지원되지 않는 op 는 SqlDTO.empty()
     *
     * 생성되는 SQL 예시
     * - contains   : t."http_host"::text ILIKE '%naver.com%'
     * - equals     : t."http_host"::text = 'naver.com'
     * - startsWith : t."http_host"::text ILIKE 'www.%'
     * - endsWith   : t."http_host"::text ILIKE '%.com'
     */
    @Override
    public SqlDTO build(String alias, String field, String op, String val, String rawTemporalKind) {
        // alias와 field 를 이용해 "t"."column" 같은 형태로 안전하게 식별자 생성
        String f = SqlIdentifier.quoteWithAlias(alias, field);

        return switch (op) {
            case "contains" ->
                    SqlDTO.of(f + "::text ILIKE ?", List.of("%" + val + "%"));
            case "equals" ->
                    SqlDTO.of(f + "::text = ?", List.of(val));
            case "startsWith" ->
                    SqlDTO.of(f + "::text ILIKE ?", List.of(val + "%"));
            case "endsWith" ->
                    SqlDTO.of(f + "::text ILIKE ?", List.of("%" + val));
            default ->
                // 정의되지 않은 연산자면 아무 조건도 생성하지 않음
                    SqlDTO.empty();
        };
    }
}
