package com.moa.api.grid.dto;

/*****************************************************************************
 CLASS NAME    : SqlDTO
 DESCRIPTION   : 동적 SQL 문자열과 바인딩 파라미터를 함께 보관하는 DTO
 AUTHOR        : 방대혁
 ******************************************************************************/

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@ToString
@AllArgsConstructor
public class SqlDTO {
    public final String sql;
    public final List<Object> args;

    public static SqlDTO empty() {
        return new SqlDTO("", List.of());
    }

    public static SqlDTO raw(String raw) {
        return new SqlDTO(raw, List.of());
    }

    public static SqlDTO of(String sql, List<Object> args) {
        return new SqlDTO(sql, new ArrayList<>(args));
    }

    public static SqlDTO wrap(SqlDTO inner) {
        return inner.isBlank() ? empty() : new SqlDTO("(" + inner.sql + ")", inner.args);
    }

    public static SqlDTO and(SqlDTO a, SqlDTO b) {
        if (a.isBlank()) return b;
        if (b.isBlank()) return a;
        List<Object> args = new ArrayList<>(a.args);
        args.addAll(b.args);
        return new SqlDTO(a.sql + " AND " + b.sql, args);
    }

    public static SqlDTO join(String sep, List<SqlDTO> list) {
        List<SqlDTO> nonBlank = list.stream()
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
        if (nonBlank.isEmpty()) return empty();

        String joined = nonBlank.stream()
                .map(s -> s.sql)
                .collect(Collectors.joining(sep));

        List<Object> args = new ArrayList<>();
        nonBlank.forEach(s -> args.addAll(s.args));

        return new SqlDTO(joined, args);
    }

    public static SqlDTO sequence(List<SqlDTO> parts) {
        if (parts.isEmpty()) return empty();

        StringBuilder sb = new StringBuilder();
        List<Object> args = new ArrayList<>();

        for (SqlDTO p : parts) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(p.sql);
            args.addAll(p.args);
        }

        return new SqlDTO(sb.toString(), args);
    }

    public boolean isBlank() {
        return sql == null || sql.isBlank();
    }
}
