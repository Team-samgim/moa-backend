package com.moa.api.pivot.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moa.api.pivot.model.RowGroupCursor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class CursorCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CursorCodec() {}

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

    public static String encodeRowGroupCursor(RowGroupCursor cursor) {
        if (cursor == null) return null;
        try {
            String json = MAPPER.writeValueAsString(cursor);
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to encode cursor", e);
        }
    }

    public static RowGroupCursor decodeRowGroupCursor(String cursorStr) {
        if (cursorStr == null || cursorStr.isBlank()) return null;
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(cursorStr);
            String json = new String(bytes, StandardCharsets.UTF_8);
            return MAPPER.readValue(json, RowGroupCursor.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cursor", e);
        }
    }
}
