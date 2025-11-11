package com.moa.api.grid.util.condition;

import com.moa.api.grid.dto.SqlDTO;

import java.util.ArrayList;
import java.util.List;

public class ConditionBuilderRegistry {
    private final List<FieldConditionBuilder> builders = new ArrayList<>();

    public ConditionBuilderRegistry(FieldConditionBuilder... b) {
        builders.addAll(java.util.Arrays.asList(b));
        // 마지막에 기본 TEXT 빌더 추가(보호용)
        builders.add(new TextConditionBuilder());
    }

    public SqlDTO build(String alias, String type, String field, String op, String val, String rawTemporalKind) {
        for (FieldConditionBuilder b : builders) {
            if (b.supports(type)) return b.build(alias, field, op, val, rawTemporalKind);
        }
        return SqlDTO.empty();
    }
}