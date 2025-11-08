package com.moa.api.grid.util;

import com.moa.api.grid.dto.SqlDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.moa.api.grid.util.InClauseBuilder.placeholders;

public class SearchConditionMapper {
    public SqlDTO map(String alias, String field, String op, String dataType,
                      java.util.List<String> values) {
        String f = SqlIdentifier.quoteWithAlias(alias, field);
        switch (dataType) {
            case "TEXT":
            case "IP":
            case "MAC": {
                String val = values.isEmpty() ? "" : values.get(0);
                return switch (op) {
                    case "LIKE" -> SqlDTO.of(f + "::text ILIKE ?", List.of("%" + val + "%"));
                    case "STARTS_WITH" -> SqlDTO.of(f + "::text ILIKE ?", List.of(val + "%"));
                    case "ENDS_WITH" -> SqlDTO.of(f + "::text ILIKE ?", List.of("%" + val));
                    case "EQ" -> SqlDTO.of(f + " = ?", List.of(val));
                    case "NE" -> SqlDTO.of(f + " <> ?", List.of(val));
                    case "IN" ->
                            SqlDTO.of(f + " IN (" + placeholders(values.size(), "?") + ")", new ArrayList<>(values));
                    case "IS_NULL" -> SqlDTO.raw(f + " IS NULL");
                    case "IS_NOT_NULL" -> SqlDTO.raw(f + " IS NOT NULL");
                    default -> SqlDTO.empty();
                };
            }
            case "NUMBER": {
                String v1 = values.isEmpty() ? null : values.get(0);
                String v2 = values.size() > 1 ? values.get(1) : null;
                return switch (op) {
                    case "EQ" -> SqlDTO.of(f + " = ?", List.of(parseNum(v1)));
                    case "NE" -> SqlDTO.of(f + " <> ?", List.of(parseNum(v1)));
                    case "GT" -> SqlDTO.of(f + " > ?", List.of(parseNum(v1)));
                    case "GTE" -> SqlDTO.of(f + " >= ?", List.of(parseNum(v1)));
                    case "LT" -> SqlDTO.of(f + " < ?", List.of(parseNum(v1)));
                    case "LTE" -> SqlDTO.of(f + " <= ?", List.of(parseNum(v1)));
                    case "BETWEEN" -> SqlDTO.of(f + " BETWEEN ? AND ?", List.of(parseNum(v1), parseNum(v2)));
                    case "IN" ->
                            SqlDTO.of(f + " IN (" + placeholders(values.size(), "?") + ")", values.stream().map(this::parseNum).collect(Collectors.toList()));
                    case "IS_NULL" -> SqlDTO.raw(f + " IS NULL");
                    case "IS_NOT_NULL" -> SqlDTO.raw(f + " IS NOT NULL");
                    default -> SqlDTO.empty();
                };
            }
            case "DATETIME": {
                String cast = "to_timestamp";
                List<Object> a = values.stream().map(Long::valueOf).collect(Collectors.toList());
                return switch (op) {
                    case "GTE" -> SqlDTO.of(f + " >= " + cast + "(?)", a.subList(0, 1));
                    case "LT" -> SqlDTO.of(f + " < " + cast + "(?)", a.subList(0, 1));
                    case "BETWEEN" ->
                            SqlDTO.of(f + " BETWEEN " + cast + "(?) AND " + cast + "(?)", a.subList(0, Math.min(2, a.size())));
                    case "IN" ->
                            SqlDTO.of(f + " IN (" + values.stream().map(v -> cast + "(?)").collect(Collectors.joining(", ")) + ")", a);
                    case "IS_NULL" -> SqlDTO.raw(f + " IS NULL");
                    case "IS_NOT_NULL" -> SqlDTO.raw(f + " IS NOT NULL");
                    default -> SqlDTO.empty();
                };
            }
            case "BOOLEAN": {
                String bool = values.isEmpty() ? "false" : values.get(0);
                return switch (op) {
                    case "IS_NULL" -> SqlDTO.raw(f + " IS NULL");
                    case "IS_NOT_NULL" -> SqlDTO.raw(f + " IS NOT NULL");
                    case "EQ" -> SqlDTO.of(f + " = ?", List.of(Boolean.valueOf(bool)));
                    default -> SqlDTO.empty();
                };
            }
            default:
                return SqlDTO.empty();
        }
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