package com.moa.api.grid.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON 관련 공통 유틸
 *
 * AUTHOR        : 방대혁
 *
 * 사용 목적
 * - 문자열로 들어온 JSON / 값들을 다룰 때
 *   - 따옴표로 감싸진 문자열 정규화
 *   - JSON 파싱
 *   - JsonNode 에서 안전하게 문자열 값 꺼내기
 *   - 대문자 변환, 배열값 추출 등
 */
public class JsonSupport {

    /** Jackson ObjectMapper 주입용 */
    private final ObjectMapper mapper;

    public JsonSupport(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 문자열 정규화
     *
     * 기능:
     * - 앞뒤 공백 제거
     * - 값이 "..." (따옴표로 감싸진 형태) 이면
     *   JSON 문자열로 간주하고 파싱해서 실제 문자열로 변환
     *   (예: "\"a\\nb\"" → a\nb)
     * - 그 외에는 trim 된 문자열 그대로 반환
     *
     * @param s 원본 문자열
     * @return 정규화된 문자열 (null 이면 "" 반환)
     */
    public String normalize(String s) {
        if (s == null) return "";
        String t = s.trim();

        // 양 끝이 큰따옴표로 감싸져 있으면 JSON 문자열로 가정
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
            try {
                // "\"a\\nb\"" 같은 형태를 실제 문자열로 디코딩
                return mapper.readValue(t, String.class);
            } catch (Exception ignore) {
                // 파싱 실패 시 그냥 원본 trim 값 반환
            }
        }
        return t;
    }

    /**
     * JSON 문자열 → JsonNode 파싱
     *
     * @param json JSON 문자열
     * @return 파싱된 JsonNode
     * @throws RuntimeException 파싱 실패 시 런타임 예외
     */
    public JsonNode parse(String json) {
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * JsonNode 에서 첫 번째로 존재하는 키의 문자열 값 반환
     *
     * 예:
     *   textOrNull(node, "field", "label", "name")
     *   → field / label / name 순으로 찾아서, 존재하는 첫 값의 asText(null) 반환
     *
     * @param obj  대상 JsonNode (객체)
     * @param keys 후보 키 목록
     * @return 문자열 값 또는 null
     */
    public static String textOrNull(JsonNode obj, String... keys) {
        for (String k : keys) {
            JsonNode v = obj.path(k);
            if (v != null && !v.isNull()) return v.asText(null);
        }
        return null;
    }

    /**
     * 특정 키의 값을 대문자로 가져오기
     *
     * - 값이 없거나 null 이면 null
     * - 값이 있으면 asText() 후 upper-case 로 변환
     *
     * @param obj JsonNode
     * @param key 키
     * @return 대문자 문자열 또는 null
     */
    public static String optUpper(JsonNode obj, String key) {
        JsonNode v = obj.path(key);
        if (v == null || v.isNull()) return null;
        String s = v.asText();
        return s == null ? null : s.toUpperCase();
    }

    /**
     * 배열 형태(JsonNode)에서 문자열 리스트 추출
     *
     * - arr 가 배열이면 각 요소를 asText() 해서 List<String> 으로 반환
     * - 요소가 null 이면 리스트에는 null 삽입
     *
     * @param arr 배열 JsonNode
     * @return 문자열 리스트 (배열이 아니거나 null 이면 빈 리스트)
     */
    public static List<String> readStringValues(JsonNode arr) {
        List<String> vals = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            for (JsonNode v : arr) {
                vals.add(v.isNull() ? null : v.asText());
            }
        }
        return vals;
    }
}
