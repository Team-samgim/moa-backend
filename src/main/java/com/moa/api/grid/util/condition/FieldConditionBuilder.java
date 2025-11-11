package com.moa.api.grid.util.condition;

import com.moa.api.grid.dto.SqlDTO;

public interface FieldConditionBuilder {
    boolean supports(String type);
    SqlDTO build(String alias, String field, String op, String val, String rawTemporalKind);
}