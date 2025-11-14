package com.moa.api.grid.util.condition;

import com.moa.api.grid.dto.SqlDTO;
import com.moa.api.grid.util.SqlIdentifier;

public class NumberConditionBuilder implements FieldConditionBuilder {
    @Override
    public boolean supports(String type) {
        return "number".equalsIgnoreCase(type);
    }

    @Override
    public SqlDTO build(String alias, String field, String op, String val, String rawTemporalKind) {
        String f = SqlIdentifier.quoteWithAlias(alias, field);
        Number n = parseNum(val);
        return switch (op) {
            case "=", "equals" -> SqlDTO.of(f + " = ?", java.util.List.of(n));
            case ">", "<", ">=", "<=" -> SqlDTO.of(f + " " + op + " ?", java.util.List.of(n));
            case "contains" -> SqlDTO.of(f + "::text ILIKE ?", java.util.List.of("%" + val + "%"));
            default -> SqlDTO.empty();
        };
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