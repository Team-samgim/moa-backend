package com.moa.api.grid.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class JsonSupport {
    private final ObjectMapper mapper;

    public JsonSupport(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String normalize(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
            try {
                return mapper.readValue(t, String.class);
            } catch (Exception ignore) {}
        }
        return t;
    }

    public JsonNode parse(String json) {
        try { return mapper.readTree(json); } catch (Exception e) { throw new RuntimeException(e); }
    }

    public static String textOrNull(JsonNode obj, String... keys) {
        for (String k : keys) {
            JsonNode v = obj.path(k);
            if (v != null && !v.isNull()) return v.asText(null);
        }
        return null;
    }

    public static String optUpper(JsonNode obj, String key) {
        JsonNode v = obj.path(key);
        if (v == null || v.isNull()) return null;
        String s = v.asText();
        return s == null ? null : s.toUpperCase();
    }

    public static List<String> readStringValues(JsonNode arr) {
        List<String> vals = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            for (JsonNode v : arr) vals.add(v.isNull() ? null : v.asText());
        }
        return vals;
    }
}