package com.moa.api.grid.util;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.moa.api.grid.dto.SqlDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * IN 절(SQL IN clause) 생성 유틸리티
 *
 * AUTHOR        : 방대혁
 *
 * 역할
 * - 필터 모델의 "values" 배열(ArrayNode)을 기반으로
 *   안전한 IN 조건 SQL과 바인딩 파라미터 목록(SqlDTO)을 만들어준다.
 *
 * 지원 기능
 * - dateIn      : 날짜(일 단위) IN 조건 (timestamp/timestamptz → KST date 변환 포함)
 * - inWithType  : 타입(number/string 등)에 맞는 IN 조건 생성
 */
public class InClauseBuilder {

    /** KST 변환 및 timestamp 표현식 생성을 위한 팩토리 */
    private final TemporalExprFactory temporal;

    public InClauseBuilder(TemporalExprFactory temporal) {
        this.temporal = temporal;
    }

    /**
     * 날짜 IN 절 생성
     *
     * 사용 예:
     *   filter: { field: ts, mode: checkbox, values: ["2024-01-01", "2024-01-02"] }
     *   → toDateExpr 로 컬럼을 KST 기준 date 로 변환한 뒤
     *     WHERE kst_date IN (?::date, ?::date)
     *
     * @param alias           테이블 별칭 (예: "t")
     * @param field           컬럼명 (예: "ts_server")
     * @param values          날짜 문자열 배열 (ArrayNode)
     * @param rawTemporalKind 원시 시간 타입 정보 (timestamp / timestamptz / date 등)
     * @return IN 절 SqlDTO (값이 없으면 SqlDTO.empty())
     */
    public SqlDTO dateIn(String alias, String field, ArrayNode values, String rawTemporalKind) {
        int n = values == null ? 0 : values.size();
        if (n == 0) return SqlDTO.empty(); // 값 없으면 조건 생성 안 함

        // timestamp/timestamptz → KST date 표현식
        String kstDate = temporal.toDateExpr(alias, field, rawTemporalKind);

        // ?::date, ?::date ... 형태 placeholder 생성
        String placeholders = placeholders(n, "?::date");

        // 바인딩 파라미터 준비
        List<Object> args = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            args.add(values.get(i).asText());
        }

        return SqlDTO.of(kstDate + " IN (" + placeholders + ")", args);
    }

    /**
     * 타입별 IN 절 생성
     *
     * - number 타입 : 컬럼 그대로 비교 (t."pkt_len" IN (?, ?, ?))
     * - 그 외       : ::text 캐스팅 후 문자열 비교 (t."http_host"::text IN (?, ?, ?))
     *
     * @param alias  테이블 별칭
     * @param field  컬럼명
     * @param values 값 배열 (ArrayNode)
     * @param type   프론트 타입(number/string/…)
     * @return IN 절 SqlDTO
     *         values 가 비어 있으면 항상 거짓인 1=0 을 반환하여
     *         필터 적용 시 빈 결과가 되도록 한다.
     */
    public SqlDTO inWithType(String alias, String field, ArrayNode values, String type) {
        int n = values == null ? 0 : values.size();
        if (n == 0) return SqlDTO.raw("1=0"); // 선택값이 없으면 매칭되지 않음

        String f = SqlIdentifier.quoteWithAlias(alias, field);
        String placeholders = placeholders(n, "?");
        List<Object> args = new ArrayList<>(n);

        // 숫자 타입이면 숫자로 파싱해서 그대로 비교
        if ("number".equalsIgnoreCase(type)) {
            for (int i = 0; i < n; i++) {
                args.add(parseNum(values.get(i).asText()));
            }
            return SqlDTO.of(f + " IN (" + placeholders + ")", args);
        }

        // 그 외 타입은 문자열 비교용으로 ::text 캐스팅
        for (int i = 0; i < n; i++) {
            args.add(values.get(i).asText());
        }
        return SqlDTO.of(f + "::text IN (" + placeholders + ")", args);
    }

    /**
     * "?, ?, ?" 같은 placeholder 문자열 생성
     *
     * @param n     개수
     * @param token 기본 토큰 (예: "?", "?::date")
     * @return "?, ?, ?" 또는 "?::date, ?::date" 형태 문자열
     */
    public static String placeholders(int n, String token) {
        if (n <= 0) return "";
        return Stream.generate(() -> token)
                .limit(n)
                .collect(Collectors.joining(", "));
    }

    /**
     * 문자열을 number 로 파싱
     * - 정수 형태면 Long
     * - 소수점 포함이면 Double
     * - 파싱 실패 시 0 반환
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
