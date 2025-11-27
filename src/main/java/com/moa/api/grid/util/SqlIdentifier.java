package com.moa.api.grid.util;

/*****************************************************************************
 CLASS NAME    : SqlIdentifier
 DESCRIPTION   : SQL 식별자(컬럼명/alias) 관련 유틸 클래스.
 - "field-name" 형태의 컬럼에서 suffix 제거(safeFieldName)
 - SQL 표준 인용부호(") 적용(quote, quoteWithAlias)
 SQL 인젝션 위험을 막기 위해 모든 컬럼명에 안전한 quoting을 적용한다.
 AUTHOR        : 방대혁
 ******************************************************************************/
public final class SqlIdentifier {

    private SqlIdentifier() {}

    /**
     * 컬럼명에서 불필요한 suffix('-' 이후)를 제거
     *
     * 예:
     *   - input: "ts_server_nsec-desc"
     *   - output: "ts_server_nsec"
     */
    public static String safeFieldName(String field) {
        if (field == null) return "";
        int i = field.indexOf('-');
        return (i > -1) ? field.substring(0, i) : field;
    }

    /**
     * 컬럼명을 SQL 표준 인용부호("컬럼") 형태로 변환
     *
     * 예:
     *   quote("ts") → "ts"
     *   quote("user") → "user"
     */
    public static String quote(String col) {
        String c = (col == null) ? "" : col;
        return "\"" + c.replace("\"", "\"\"") + "\"";
    }

    /**
     * alias + 컬럼명을 조합하여 "alias"."컬럼" 형태로 래핑
     *
     * 예:
     *   quoteWithAlias("t", "ts") → t."ts"
     */
    public static String quoteWithAlias(String alias, String col) {
        String c = (col == null) ? "" : col;
        return alias + ".\"" + c.replace("\"", "\"\"") + "\"";
    }
}
