package com.moa.api.grid.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/*****************************************************************************
 CLASS NAME    : TypeResolver
 DESCRIPTION   : 필터/조건에서 사용하는 컬럼 타입을 결정하는 유틸리티.
 - filterModel JSON에서 type 우선 적용
 - 없으면 typeMap(컬럼 메타 정보)에서 추론
 - 최종 fallback: string
 AUTHOR        : 방대혁
 ******************************************************************************/
public class TypeResolver {

    /**
     * 필터 항목의 실제 데이터 타입을 결정한다.
     *
     * @param field    필드명(예: response_time, status_code)
     * @param node     filterModel 내부 JSON 노드
     * @param typeMap  레이어별 컬럼 타입 메타 (string/number/date/ip/mac/boolean…)
     * @return 타입 문자열 (소문자)
     */
    public String resolveForField(String field, JsonNode node, Map<String, String> typeMap) {

        // 1) filterModel에서 type 직접 지정한 경우 우선 사용
        String t = node.path("type").asText(null);
        if (t != null && !t.isBlank()) {
            return t.toLowerCase();
        }

        // 2) 메타 정보(typeMap) 기반 추론
        String safeField = SqlIdentifier.safeFieldName(field);
        String fromMap = typeMap != null ? typeMap.get(safeField) : null;
        if (fromMap != null && !fromMap.isBlank()) {
            return fromMap.toLowerCase();
        }

        // 3) fallback: 문자열(string)
        return "string";
    }
}
