package com.moa.api.grid.util.condition;

import com.moa.api.grid.dto.SqlDTO;
import com.moa.api.grid.util.SqlIdentifier;

public class TextConditionBuilder implements FieldConditionBuilder {
    @Override
    public boolean supports(String type) {
        return type == null || type.isBlank() || "string".equalsIgnoreCase(type);
    }

    @Override
    public SqlDTO build(String alias, String field, String op, String val, String rawTemporalKind) {
        String f = SqlIdentifier.quoteWithAlias(alias, field);
        return switch (op) {
            case "contains" -> SqlDTO.of(f + "::text ILIKE ?", java.util.List.of("%" + val + "%"));
            case "equals" -> SqlDTO.of(f + "::text = ?", java.util.List.of(val));
            case "startsWith" -> SqlDTO.of(f + "::text ILIKE ?", java.util.List.of(val + "%"));
            case "endsWith" -> SqlDTO.of(f + "::text ILIKE ?", java.util.List.of("%" + val));
            default -> SqlDTO.empty();
        };
    }
}