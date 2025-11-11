package com.moa.api.grid.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public class TypeResolver {
    public String resolveForField(String field, JsonNode node, Map<String, String> typeMap) {
        String t = node.path("type").asText(null);
        if (t != null && !t.isBlank()) return t.toLowerCase();
        String safeField = SqlIdentifier.safeFieldName(field);
        String fromMap = typeMap != null ? typeMap.get(safeField) : null;
        if (fromMap != null && !fromMap.isBlank()) return fromMap.toLowerCase();
        return "string";
    }
}