package com.moa.api.grid.util.condition;

import com.moa.api.grid.dto.SqlDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * 조건 빌더 레지스트리
 *
 * AUTHOR        : 방대혁
 *
 * 역할
 * - 필드 타입(type)에 따라 알맞은 FieldConditionBuilder를 찾아서
 *   WHERE 조건용 SqlDTO를 만들어주는 팩토리/디스패처 역할
 * - 등록된 빌더 중 supports(type) == true 인 빌더를 순서대로 탐색
 * - 어떤 빌더도 지원하지 않으면 기본 TextConditionBuilder 가 처리
 */
public class ConditionBuilderRegistry {

    /**
     * 실제로 조건을 만들어 줄 FieldConditionBuilder 목록
     * 예) NumberConditionBuilder, IpConditionBuilder, DateConditionBuilder, TextConditionBuilder 등
     */
    private final List<FieldConditionBuilder> builders = new ArrayList<>();

    /**
     * 생성자
     *
     * @param b 외부에서 주입할 FieldConditionBuilder 가변 인자
     *
     * 동작:
     * - 전달된 빌더들을 builders 리스트에 모두 추가
     * - 마지막에 항상 TextConditionBuilder 를 넣어서
     *   어떤 타입이든 최소 텍스트 조건이라도 만들어지도록 보호장치 역할
     */
    public ConditionBuilderRegistry(FieldConditionBuilder... b) {
        // 주입된 커스텀 빌더 등록
        builders.addAll(java.util.Arrays.asList(b));
        // 마지막에 기본 TEXT 빌더 추가(보호용, fallback)
        builders.add(new TextConditionBuilder());
    }

    /**
     * 조건 SQL 생성
     *
     * @param alias          테이블 별칭 (예: "t")
     * @param type           프론트 기준 필드 타입(string, number, date, ip, mac, json 등)
     * @param field          실제 컬럼명
     * @param op             연산자(=, !=, contains, in, between, startsWith 등)
     * @param val            사용자가 입력한 값(원시 문자열)
     * @param rawTemporalKind 시계열 컬럼의 원래 타입 정보(timestamp, timestamptz, date 등)
     * @return SqlDTO        WHERE 절 한 조각 (sql + 바인딩 파라미터)
     *
     * 처리 흐름:
     * 1) 등록된 builders 를 순회하면서 supports(type) == true 인 빌더를 찾음
     * 2) 가장 먼저 매칭되는 빌더의 build(...) 결과를 그대로 반환
     * 3) 어떤 빌더도 매칭되지 않으면 SqlDTO.empty() 반환
     *    (실제 상황에서는 TextConditionBuilder 가 항상 마지막에 걸리므로 거의 사용되지 않음)
     */
    public SqlDTO build(
            String alias,
            String type,
            String field,
            String op,
            String val,
            String rawTemporalKind
    ) {
        for (FieldConditionBuilder b : builders) {
            if (b.supports(type)) {
                return b.build(alias, field, op, val, rawTemporalKind);
            }
        }
        // 이 지점까지 오면 모든 빌더가 type 을 지원하지 않는 경우
        // (실제로는 TextConditionBuilder 가 fallback 이라 거의 도달하지 않음)
        return SqlDTO.empty();
    }
}
