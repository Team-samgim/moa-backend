package com.moa.api.grid.util.condition;

import com.moa.api.grid.dto.SqlDTO;
import com.moa.api.grid.util.SqlIdentifier;

import java.util.List;

/**
 * 네트워크(IP, MAC) 타입 필드용 조건 빌더
 *
 * AUTHOR        : 방대혁
 *
 * 역할
 * - 프론트에서 type 이 "ip" 또는 "mac" 으로 넘어오는 필드에 대해
 *   equals / startsWith / endsWith / contains 연산을 처리한다.
 * - 모든 비교는 DB 컬럼을 text 로 캐스팅한 뒤 ILIKE 를 사용해 수행한다.
 */
public class NetworkConditionBuilder implements FieldConditionBuilder {

    /**
     * 이 빌더가 처리 가능한 타입인지 여부 반환
     *
     * @param type 프론트 기준 타입 (예: "ip", "mac")
     * @return type 이 "ip" 또는 "mac" 인 경우 true
     */
    @Override
    public boolean supports(String type) {
        return "ip".equalsIgnoreCase(type) || "mac".equalsIgnoreCase(type);
    }

    /**
     * IP / MAC 필드에 대한 WHERE 조건 생성
     *
     * @param alias          테이블 별칭 (예: "t")
     * @param field          컬럼명 (예: "src_ip", "dst_mac")
     * @param op             연산자 (equals, startsWith, endsWith, contains)
     * @param val            비교 값 (문자열)
     * @param rawTemporalKind 시간 타입 정보 (네트워크 필드는 사용하지 않음)
     * @return SqlDTO(sql, args) / 지원하지 않는 op 는 SqlDTO.empty()
     *
     * 사용되는 패턴
     * - equals     : f::text = ?
     * - startsWith : f::text ILIKE '값%'
     * - endsWith   : f::text ILIKE '%값'
     * - contains   : f::text ILIKE '%값%'
     */
    @Override
    public SqlDTO build(String alias, String field, String op, String val, String rawTemporalKind) {
        String f = SqlIdentifier.quoteWithAlias(alias, field);

        return switch (op) {
            case "equals" ->
                    SqlDTO.of(f + "::text = ?", List.of(val));
            case "startsWith" ->
                    SqlDTO.of(f + "::text ILIKE ?", List.of(val + "%"));
            case "endsWith" ->
                    SqlDTO.of(f + "::text ILIKE ?", List.of("%" + val));
            case "contains" ->
                    SqlDTO.of(f + "::text ILIKE ?", List.of("%" + val + "%"));
            default ->
                    SqlDTO.empty();
        };
    }
}
