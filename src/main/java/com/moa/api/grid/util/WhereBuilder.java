package com.moa.api.grid.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.moa.api.grid.dto.SqlDTO;
import com.moa.api.grid.util.condition.*;

import java.util.*;

import static com.moa.api.grid.util.JsonSupport.*;

public class WhereBuilder {
    private final JsonSupport json;
    private final TypeResolver typeResolver;
    private final TemporalExprFactory temporal;
    private final InClauseBuilder inBuilder;
    private final ConditionBuilderRegistry registry;
    private final SearchConditionMapper searchMapper;

    public WhereBuilder(ObjectMapper mapper) {
        this.temporal = new TemporalExprFactory();
        this.json = new JsonSupport(mapper);
        this.typeResolver = new TypeResolver();
        this.inBuilder = new InClauseBuilder(temporal);
        this.registry = new ConditionBuilderRegistry(
                new NumberConditionBuilder(),
                new NetworkConditionBuilder(),
                new DateConditionBuilder(temporal),
                new TextConditionBuilder()
        );
        this.searchMapper = new SearchConditionMapper();
    }

    public SqlDTO fromFilterModel(String filterModel,
                                  Map<String, String> typeMap,
                                  Map<String, String> rawTemporalKindMap) {
        if (filterModel == null || filterModel.isBlank()) return SqlDTO.empty();
        try {
            JsonNode filters = json.parse(filterModel);
            List<SqlDTO> clauses = new ArrayList<>();

            for (Iterator<String> it = filters.fieldNames(); it.hasNext(); ) {
                String field = it.next();
                JsonNode node = filters.get(field);
                String mode = node.path("mode").asText("");
                String safeField = SqlIdentifier.safeFieldName(field);
                String type = typeResolver.resolveForField(field, node, typeMap);

                if ("checkbox".equals(mode)) {
                    ArrayNode values = (ArrayNode) node.get("values");
                    if (values != null && values.size() > 0) {
                        if ("date".equalsIgnoreCase(type)) {
                            String rawKind = Optional.ofNullable(rawTemporalKindMap).map(m -> m.get(safeField)).orElse("timestamp");
                            clauses.add(inBuilder.dateIn("t", safeField, values, rawKind));
                        } else {
                            clauses.add(inBuilder.inWithType("t", safeField, values, type));
                        }
                    }
                } else if ("condition".equals(mode)) {
                    ArrayNode conditions = (ArrayNode) node.get("conditions");
                    ArrayNode logicOps = (ArrayNode) node.get("logicOps");
                    List<SqlDTO> exprs = new ArrayList<>();

                    for (int i = 0; i < conditions.size(); i++) {
                        JsonNode cond = conditions.get(i);
                        String op = cond.path("op").asText("");
                        String rawTemporalKind = Optional.ofNullable(rawTemporalKindMap).map(m -> m.getOrDefault(safeField, "timestamp")).orElse("timestamp");

                        SqlDTO expr;
                        if ("between".equalsIgnoreCase(op)) {
                            if ("date".equalsIgnoreCase(type)) {
                                String v1 = textOrNull(cond, "val1", "from");
                                String v2 = textOrNull(cond, "val2", "to");
                                if (v1 != null && v2 != null) {
                                    String kstDate = temporal.toDateExpr("t", safeField, rawTemporalKind);
                                    expr = SqlDTO.of(kstDate + " BETWEEN ?::date AND ?::date", List.of(v1, v2));
                                } else continue;
                            } else if ("number".equalsIgnoreCase(type)) {
                                String a = textOrNull(cond, "min", "val1");
                                String b = textOrNull(cond, "max", "val2");
                                if (a != null && b != null) {
                                    expr = SqlDTO.of(SqlIdentifier.quoteWithAlias("t", safeField) + " BETWEEN ? AND ?", List.of(parseNum(a), parseNum(b)));
                                } else continue;
                            } else {
                                String a = cond.path("val1").asText(null);
                                String b = cond.path("val2").asText(null);
                                if (a != null && b != null) {
                                    expr = SqlDTO.of(SqlIdentifier.quoteWithAlias("t", safeField) + "::text BETWEEN ? AND ?", List.of(a, b));
                                } else continue;
                            }
                        } else {
                            String val = cond.path("val").asText(null);
                            if (val == null || val.isBlank()) continue;
                            expr = registry.build("t", type, safeField, op, val, rawTemporalKind);
                            if (expr.isBlank()) continue;
                        }
                        exprs.add(expr);
                    }

                    if (!exprs.isEmpty()) {
                        StringBuilder whereExpr = new StringBuilder();
                        List<Object> args = new ArrayList<>();
                        for (int i = 0; i < exprs.size(); i++) {
                            if (i == 0) {
                                whereExpr.append(exprs.get(i).sql);
                                args.addAll(exprs.get(i).args);
                            } else {
                                String logic = (logicOps != null && logicOps.size() > i - 1)
                                        ? logicOps.get(i - 1).asText("AND") : "AND";
                                whereExpr.append(' ').append(logic).append(' ').append(exprs.get(i).sql);
                                args.addAll(exprs.get(i).args);
                            }
                        }
                        clauses.add(new SqlDTO("(" + whereExpr + ")", args));
                    }
                }
            }
            return SqlDTO.join(" AND ", clauses);
        } catch (Exception e) {
            return SqlDTO.empty();
        }
    }

    public SqlDTO fromFilterModelExcluding(String filterModel, String excludeField,
                                           Map<String, String> typeMap,
                                           Map<String, String> rawTemporalKindMap) {
        if (filterModel == null || filterModel.isBlank()) return SqlDTO.empty();
        try {
            JsonNode filters = json.parse(filterModel);
            String excludeSafe = SqlIdentifier.safeFieldName(excludeField);

            List<SqlDTO> clauses = new ArrayList<>();
            for (Iterator<String> it = filters.fieldNames(); it.hasNext(); ) {
                String field = it.next();
                String safeField = SqlIdentifier.safeFieldName(field);
                if (safeField.equals(excludeSafe)) continue;

                JsonNode node = filters.get(field);
                String mode = node.path("mode").asText("");
                String type = typeResolver.resolveForField(field, node, typeMap);

                if ("checkbox".equals(mode)) {
                    ArrayNode values = (ArrayNode) node.get("values");
                    if (values != null && values.size() > 0) {
                        if ("date".equalsIgnoreCase(type)) {
                            String rawKind = Optional.ofNullable(rawTemporalKindMap).map(m -> m.get(safeField)).orElse("timestamp");
                            clauses.add(inBuilder.dateIn("t", safeField, values, rawKind));
                        } else {
                            clauses.add(inBuilder.inWithType("t", safeField, values, type));
                        }
                    }
                } else if ("condition".equals(mode)) {
                    ArrayNode conditions = (ArrayNode) node.get("conditions");
                    ArrayNode logicOps = (ArrayNode) node.get("logicOps");
                    List<SqlDTO> exprs = new ArrayList<>();

                    for (int i = 0; i < conditions.size(); i++) {
                        JsonNode cond = conditions.get(i);
                        String op = cond.path("op").asText();
                        String val = cond.path("val").asText();
                        if (val == null || val.isBlank()) continue;
                        String rawTemporalKind = Optional.ofNullable(rawTemporalKindMap).map(m -> m.getOrDefault(safeField, "timestamp")).orElse("timestamp");
                        SqlDTO expr = registry.build("t", type, safeField, op, val, rawTemporalKind);
                        if (!expr.isBlank()) exprs.add(expr);
                    }

                    if (!exprs.isEmpty()) {
                        StringBuilder whereExpr = new StringBuilder();
                        List<Object> args = new ArrayList<>();
                        for (int i = 0; i < exprs.size(); i++) {
                            if (i == 0) {
                                whereExpr.append(exprs.get(i).sql);
                                args.addAll(exprs.get(i).args);
                            } else {
                                String logic = (logicOps != null && logicOps.size() > i - 1)
                                        ? logicOps.get(i - 1).asText("AND") : "AND";
                                whereExpr.append(' ').append(logic).append(' ').append(exprs.get(i).sql);
                                args.addAll(exprs.get(i).args);
                            }
                        }
                        clauses.add(new SqlDTO("(" + whereExpr + ")", args));
                    }
                }
            }
            return SqlDTO.join(" AND ", clauses);
        } catch (Exception e) {
            return SqlDTO.empty();
        }
    }

    public SqlDTO fromBaseSpec(String baseSpecJson,
                               Map<String, String> typeMap,
                               Map<String, String> rawTemporalKindMap) {
        if (baseSpecJson == null || baseSpecJson.isBlank()) return SqlDTO.empty();
        try {
            String norm = json.normalize(baseSpecJson);
            JsonNode root = json.parse(norm);
            List<SqlDTO> parts = new ArrayList<>();

            // 1) time
            JsonNode time = root.path("time");
            if (time.isObject()) {
                String field = textOrNull(time, "field");
                Long from = time.hasNonNull("fromEpoch") ? time.get("fromEpoch").asLong() : null;
                Long to = time.hasNonNull("toEpoch") ? time.get("toEpoch").asLong() : null;
                boolean inclusive = time.path("inclusive").asBoolean(true);
                if (field != null && from != null && to != null) {
                    String fType = typeMap != null ? typeMap.getOrDefault(field, "") : "";
                    String raw = rawTemporalKindMap != null ? rawTemporalKindMap.getOrDefault(field, "") : "";
                    parts.add(temporal.timeRangeClause("t", field, from, to, inclusive, fType, raw));
                }
            }

            // 2) conditions
            JsonNode conds = root.path("conditions");
            if (conds.isArray() && conds.size() > 0) {
                List<SqlDTO> cParts = new ArrayList<>();
                for (int i = 0; i < conds.size(); i++) {
                    JsonNode c = conds.get(i);
                    String field = textOrNull(c, "field");
                    String op = textOrNull(c, "op");
                    if (field == null || op == null) continue;

                    String dt = optUpper(c, "dataType");
                    String dataType = (dt == null || dt.isBlank())
                            ? (typeMap != null && typeMap.get(field) != null ? typeMap.get(field).toUpperCase() : "TEXT")
                            : dt;

                    java.util.List<String> vals = readStringValues(c.path("values"));
                    SqlDTO expr = searchMapper.map("t", field, op.toUpperCase(), dataType, vals);
                    if (expr.isBlank()) continue;

                    if (i > 0) {
                        String join = c.path("join").asText("AND").toUpperCase();
                        cParts.add(SqlDTO.raw(join));
                    }
                    cParts.add(SqlDTO.wrap(expr));
                }
                if (!cParts.isEmpty()) {
                    SqlDTO merged = SqlDTO.sequence(cParts);
                    boolean not = root.path("not").asBoolean(false);
                    parts.add(not ? SqlDTO.of("NOT (" + merged.sql + ")", merged.args) : SqlDTO.of("(" + merged.sql + ")", merged.args));
                }
            }
            return SqlDTO.join(" AND ", parts);
        } catch (Exception e) {
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
