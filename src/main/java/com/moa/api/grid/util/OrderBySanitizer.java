package com.moa.api.grid.util;

import java.util.Map;

public final class OrderBySanitizer {
    private OrderBySanitizer() {}

    public static String sanitizeOrderBy(String sortField, Map<String, String> typeMap, String fallback) {
        if (sortField == null || sortField.isBlank()) return fallback;
        String raw = SqlIdentifier.safeFieldName(sortField);
        return (typeMap != null && typeMap.containsKey(raw)) ? raw : fallback;
    }

    public static String sanitizeDir(String dir) {
        return "ASC".equalsIgnoreCase(dir) ? "ASC" : "DESC";
    }
}