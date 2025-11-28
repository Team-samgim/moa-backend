// 작성자: 최이서
package com.moa.api.pivot.util;

public class ValueUtils {
    private ValueUtils() {}

    public static String normalizeKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return "(empty)";
        }
        return raw;
    }
}
