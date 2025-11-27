package com.moa.api.grid.util.condition;

import com.moa.api.grid.dto.SqlDTO;

/**
 * 필드 타입별 WHERE 조건을 생성하는 전략 인터페이스
 *
 * AUTHOR        : 방대혁
 *
 * 역할
 * - 프론트에서 넘어온 type 정보(number/string/date/...) 에 따라
 *   각각 다른 구현체(DateConditionBuilder, TextConditionBuilder 등)가
 *   SQL WHERE 절 조각을 생성하도록 하기 위한 공통 인터페이스.
 *
 * 사용 흐름
 * - ConditionBuilderRegistry 가 type 에 맞는 구현체를 선택해서
 *   build(...) 를 호출한다.
 * - 구현체는 alias/field/op/val/rawTemporalKind 정보를 이용해
 *   SqlDTO(sql, args) 를 반환한다.
 */
public interface FieldConditionBuilder {

    /**
     * 이 빌더가 처리 가능한 타입인지 여부
     *
     * @param type 프론트 기준 타입 (예: "number", "string", "date", "json" 등)
     * @return true 이면 해당 타입에 대한 조건 생성은 이 빌더가 담당
     */
    boolean supports(String type);

    /**
     * 단일 필드에 대한 WHERE 조건 SQL 생성
     *
     * @param alias          테이블 별칭 (예: "t")
     * @param field          컬럼명 (예: "ts_server")
     * @param op             연산자 (equals, contains, before, after, between 등)
     * @param val            값 (프론트에서 온 문자열 값, between 의 경우 "from,to" 형태 등)
     * @param rawTemporalKind 실제 컬럼의 시간 타입 정보
     *                        - timestamptz / timestamp / date / time 등
     *                        - 날짜/시간 변환이 필요한 빌더(DateConditionBuilder 등)에서 사용
     * @return SqlDTO(sql, args)
     *         - sql: WHERE 절에 바로 붙일 수 있는 조건 문자열 (예: "t.col = ?")
     *         - args: PreparedStatement 바인딩 파라미터 목록
     *         - 조건을 만들 수 없는 경우 SqlDTO.empty() 를 반환
     */
    SqlDTO build(String alias, String field, String op, String val, String rawTemporalKind);
}
