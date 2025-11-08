package com.moa.api.grid.util;

public final class SqlIdentifier {
    private SqlIdentifier() {}

    public static String safeFieldName(String field) {
        if (field == null) return "";
        int i = field.indexOf('-');
        return (i > -1) ? field.substring(0, i) : field;
    }

    public static String quote(String col) {
        String c = col == null ? "" : col;
        return "\"" + c.replace("\"", "\"\"") + "\"";
    }

    public static String quoteWithAlias(String alias, String col) {
        String c = col == null ? "" : col;
        return alias + ".\"" + c.replace("\"", "\"\"") + "\"";
    }
}