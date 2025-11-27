package com.moa.api.grid.util;

/*****************************************************************************
 CLASS NAME    : OrderBySanitizer
 DESCRIPTION   : ORDER BY 절에서 컬럼명과 정렬 방향을 검증 및 세정(sanitize)하는 유틸리티.
 - 정렬 컬럼이 실제 테이블 컬럼(typeMap)에 존재하는지 확인
 - 존재하지 않으면 fallback 컬럼 사용
 - 정렬 방향은 ASC/DESC 외 값이면 DESC로 강제
 AUTHOR        : 방대혁
 ******************************************************************************/

import java.util.Map;

public final class OrderBySanitizer {

    // 생성자 막기
    private OrderBySanitizer() {}

    /**
     * ORDER BY 컬럼명 세정
     *
     * @param sortField  정렬하려는 컬럼명 (프론트 요청)
     * @param typeMap    레이어별 컬럼 타입 맵 (컬럼 존재 여부 확인 용)
     * @param fallback   기본 컬럼명 (없을 때 대체)
     * @return 안전한 정렬 컬럼명
     */
    public static String sanitizeOrderBy(String sortField,
                                         Map<String, String> typeMap,
                                         String fallback) {

        if (sortField == null || sortField.isBlank()) {
            return fallback;
        }

        // 예: " latency " → "latency"
        String raw = SqlIdentifier.safeFieldName(sortField);

        // 실제 존재하는 컬럼이면 그대로 사용
        return (typeMap != null && typeMap.containsKey(raw))
                ? raw
                : fallback;
    }

    /**
     * 정렬 방향 세정
     *
     * @param dir  ASC 또는 DESC
     * @return ASC | DESC (기본 DESC)
     */
    public static String sanitizeDir(String dir) {
        return "ASC".equalsIgnoreCase(dir) ? "ASC" : "DESC";
    }
}
