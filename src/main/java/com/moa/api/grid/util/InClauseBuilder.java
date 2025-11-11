package com.moa.api.grid.util;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.moa.api.grid.dto.SqlDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InClauseBuilder {
    private final TemporalExprFactory temporal;

    public InClauseBuilder(TemporalExprFactory temporal) {
        this.temporal = temporal;
    }

    public SqlDTO dateIn(String alias, String field, ArrayNode values, String rawTemporalKind) {
        int n = values == null ? 0 : values.size();
        if (n == 0) return SqlDTO.empty();
        String kstDate = temporal.toDateExpr(alias, field, rawTemporalKind);
        String placeholders = placeholders(n, "?::date");
        List<Object> args = new ArrayList<>(n);
        for (int i = 0; i < n; i++) args.add(values.get(i).asText());
        return SqlDTO.of(kstDate + " IN (" + placeholders + ")", args);
    }

    public SqlDTO inWithType(String alias, String field, ArrayNode values, String type) {
        int n = values == null ? 0 : values.size();
        if (n == 0) return SqlDTO.raw("1=0");
        String f = SqlIdentifier.quoteWithAlias(alias, field);
        String placeholders = placeholders(n, "?");
        List<Object> args = new ArrayList<>(n);
        if ("number".equalsIgnoreCase(type)) {
            for (int i = 0; i < n; i++) args.add(parseNum(values.get(i).asText()));
            return SqlDTO.of(f + " IN (" + placeholders + ")", args);
        }
        for (int i = 0; i < n; i++) args.add(values.get(i).asText());
        return SqlDTO.of(f + "::text IN (" + placeholders + ")", args);
    }

    public static String placeholders(int n, String token) {
        if (n <= 0) return "";
        return Stream.generate(() -> token).limit(n).collect(Collectors.joining(", "));
    }

    private Number parseNum(String s) {
        if (s == null) return 0;
        try {
            return s.contains(".") ? Double.valueOf(s) : Long.valueOf(s);
        } catch (Exception e) {
            return 0;
        }
    }
}