package com.moa.api.pivot.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class CursorCodec {

    public static String encode(String value) {
        if (value == null) return null;
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String decode(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        byte[] bytes = Base64.getUrlDecoder().decode(cursor);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private CursorCodec() {}
}
