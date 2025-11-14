package com.moa.api.grid.util.condition;

import com.moa.api.grid.dto.SqlDTO;
import com.moa.api.grid.util.SqlIdentifier;
import com.moa.api.grid.util.TemporalExprFactory;

import java.util.List;

public class DateConditionBuilder implements FieldConditionBuilder {
    private final TemporalExprFactory temporal;

    public DateConditionBuilder(TemporalExprFactory temporal) {
        this.temporal = temporal;
    }

    @Override
    public boolean supports(String type) {
        return "date".equalsIgnoreCase(type);
    }

    @Override
    public SqlDTO build(String alias, String field, String op, String val, String rawTemporalKind) {
        String kstDate = temporal.toDateExpr(alias, field, rawTemporalKind);
        String rawCol = SqlIdentifier.quoteWithAlias(alias, field);
        return switch (op) {
            case "equals" -> SqlDTO.of(kstDate + " = ?::date", List.of(val));
            case "before" -> SqlDTO.of(kstDate + " < ?::date", List.of(val));
            case "after" -> SqlDTO.of(kstDate + " > ?::date", List.of(val));
            case "between" -> {
                String[] parts = val.split(",");
                if (parts.length == 2)
                    yield SqlDTO.of(kstDate + " BETWEEN ?::date AND ?::date", List.of(parts[0].trim(), parts[1].trim()));
                else yield SqlDTO.empty();
            }
            case "contains" -> SqlDTO.of(rawCol + "::text ILIKE ?", List.of("%" + val + "%"));
            default -> SqlDTO.empty();
        };
    }
}