package com.moa.api.grid.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.moa.api.grid.dto.SqlDTO;
import com.moa.api.grid.exception.GridException;
import com.moa.api.grid.util.condition.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static com.moa.api.grid.util.JsonSupport.*;

/*****************************************************************************
 CLASS NAME    : WhereBuilder
 DESCRIPTION   : filterModel / baseSpec JSON을 기반으로 WHERE 절 SQL을 동적으로 생성.
 - checkbox 모드(IN 절)
 - condition 모드(단일 조건, between, 논리연산 조합)
 - BaseSpec(time, conditions) 처리
 - 타입(Text/Number/Date/IP/MAC 등)별 ConditionBuilder 연결
 AUTHOR        : 방대혁
 ******************************************************************************/
@Slf4j
public class WhereBuilder {

    private final JsonSupport json;
    private final TypeResolver typeResolver;
    private final TemporalExprFactory temporal;
    private final InClauseBuilder inBuilder;
    private final ConditionBuilderRegistry registry;
    private final SearchConditionMapper searchMapper;

    public WhereBuilder(ObjectMapper mapper, TemporalExprFactory temporal) {
        this.temporal = temporal;
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

    /* --------------------------------------------------------------------------
     *  FILTER MODEL(프론트 필터) → SQL
     * -------------------------------------------------------------------------- */

    public SqlDTO fromFilterModel(String filterModel,
                                  Map<String, String> typeMap,
                                  Map<String, String> rawTemporalKindMap) {

        if (filterModel == null || filterModel.isBlank()) return SqlDTO.empty();

        try {
            JsonNode filters = json.parse(filterModel);
            List<SqlDTO> clauses = new ArrayList<>();

            for (Iterator<String> it = filters.fieldNames(); it.hasNext(); ) {
                String field = it.next();
                SqlDTO clause = buildFieldClause(filters.get(field), field, typeMap, rawTemporalKindMap);
                if (!clause.isBlank()) clauses.add(clause);
            }

            return SqlDTO.join(" AND ", clauses);

        } catch (Exception e) {
            log.error("Failed to parse filterModel: {}", filterModel, e);
            throw new GridException(
                    GridException.ErrorCode.INVALID_FILTER_MODEL,
                    "FilterModel 파싱 실패: " + e.getMessage()
            );
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

                SqlDTO clause = buildFieldClause(filters.get(field), field, typeMap, rawTemporalKindMap);
                if (!clause.isBlank()) clauses.add(clause);
            }

            return SqlDTO.join(" AND ", clauses);

        } catch (Exception e) {
            log.error("Failed to parse filterModel (excluding {}): {}", excludeField, filterModel, e);
            throw new GridException(
                    GridException.ErrorCode.INVALID_FILTER_MODEL,
                    "FilterModel 파싱 실패: " + e.getMessage()
            );
        }
    }

    /* --------------------------------------------------------------------------
     *  FILTER FIELD 처리 (checkbox / condition)
     * -------------------------------------------------------------------------- */

    private SqlDTO buildFieldClause(JsonNode node, String field,
                                    Map<String, String> typeMap,
                                    Map<String, String> rawTemporalKindMap) {

        String mode = node.path("mode").asText("");
        String safeField = SqlIdentifier.safeFieldName(field);
        String type = typeResolver.resolveForField(field, node, typeMap);

        return switch (mode) {
            case "checkbox" -> buildCheckboxClause(node, safeField, type, rawTemporalKindMap);
            case "condition" -> buildConditionClause(node, safeField, type, rawTemporalKindMap);
            default -> {
                log.warn("Unknown filter mode '{}' for field '{}'", mode, field);
                yield SqlDTO.empty();
            }
        };
    }

    /* --------------------------------------------------------------------------
     *  Checkbox(IN 절)
     * -------------------------------------------------------------------------- */

    private SqlDTO buildCheckboxClause(JsonNode node, String field, String type,
                                       Map<String, String> rawTemporalKindMap) {

        ArrayNode values = (ArrayNode) node.get("values");
        if (values == null || values.size() == 0) return SqlDTO.empty();

        if ("date".equalsIgnoreCase(type)) {
            String rawKind = Optional.ofNullable(rawTemporalKindMap)
                    .map(m -> m.get(field))
                    .orElse("timestamp");
            return inBuilder.dateIn("t", field, values, rawKind);
        }

        return inBuilder.inWithType("t", field, values, type);
    }

    /* --------------------------------------------------------------------------
     *  Condition 모드: 여러 조건 + 논리연산 조합
     * -------------------------------------------------------------------------- */

    private SqlDTO buildConditionClause(JsonNode node, String field, String type,
                                        Map<String, String> rawTemporalKindMap) {

        ArrayNode conditions = (ArrayNode) node.get("conditions");
        ArrayNode logicOps = (ArrayNode) node.get("logicOps");

        if (conditions == null || conditions.size() == 0) return SqlDTO.empty();

        String rawKind = Optional.ofNullable(rawTemporalKindMap)
                .map(m -> m.getOrDefault(field, "timestamp"))
                .orElse("timestamp");

        List<SqlDTO> exprs = new ArrayList<>();
        for (int i = 0; i < conditions.size(); i++) {
            SqlDTO expr = buildSingleCondition(conditions.get(i), field, type, rawKind);
            if (!expr.isBlank()) exprs.add(expr);
        }

        if (exprs.isEmpty()) return SqlDTO.empty();
        return combineWithLogicOps(exprs, logicOps);
    }

    private SqlDTO buildSingleCondition(JsonNode cond, String field, String type, String rawKind) {
        String op = cond.path("op").asText("");

        if ("between".equalsIgnoreCase(op)) {
            return buildBetweenCondition(cond, field, type, rawKind);
        }

        String val = cond.path("val").asText(null);
        if (val == null || val.isBlank()) return SqlDTO.empty();

        return registry.build("t", type, field, op, val, rawKind);
    }

    private SqlDTO buildBetweenCondition(JsonNode cond, String field, String type, String rawKind) {

        // date between
        if ("date".equalsIgnoreCase(type)) {
            String v1 = textOrNull(cond, "val1", "from");
            String v2 = textOrNull(cond, "val2", "to");
            if (v1 == null || v2 == null) return SqlDTO.empty();

            String kst = temporal.toDateExpr("t", field, rawKind);
            return SqlDTO.of(kst + " BETWEEN ?::date AND ?::date", List.of(v1, v2));
        }

        // number between
        if ("number".equalsIgnoreCase(type)) {
            String a = textOrNull(cond, "min", "val1");
            String b = textOrNull(cond, "max", "val2");
            if (a == null || b == null) return SqlDTO.empty();

            String f = SqlIdentifier.quoteWithAlias("t", field);
            return SqlDTO.of(f + " BETWEEN ? AND ?", List.of(parseNum(a), parseNum(b)));
        }

        // string/text/IP/MAC between
        String a = cond.path("val1").asText(null);
        String b = cond.path("val2").asText(null);
        if (a == null || b == null) return SqlDTO.empty();

        String f = SqlIdentifier.quoteWithAlias("t", field);
        return SqlDTO.of(f + "::text BETWEEN ? AND ?", List.of(a, b));
    }

    /* --------------------------------------------------------------------------
     *  LogicOps 결합 (AND/OR)
     * -------------------------------------------------------------------------- */

    private SqlDTO combineWithLogicOps(List<SqlDTO> exprs, ArrayNode logicOps) {

        if (exprs.size() == 1) return SqlDTO.wrap(exprs.get(0));

        List<String> sqlParts = new ArrayList<>();
        List<Object> args = new ArrayList<>();

        for (int i = 0; i < exprs.size(); i++) {

            if (i > 0) {
                String logic = (logicOps != null && logicOps.size() > i - 1)
                        ? logicOps.get(i - 1).asText("AND")
                        : "AND";
                sqlParts.add(logic);
            }

            sqlParts.add(exprs.get(i).getSql());
            args.addAll(exprs.get(i).getArgs());
        }

        return new SqlDTO("(" + String.join(" ", sqlParts) + ")", args);
    }

    /* --------------------------------------------------------------------------
     *  BASE SPEC 처리(time, conditions)
     * -------------------------------------------------------------------------- */

    public SqlDTO fromBaseSpec(String baseSpecJson,
                               Map<String, String> typeMap,
                               Map<String, String> rawTemporalKindMap) {

        if (baseSpecJson == null || baseSpecJson.isBlank()) return SqlDTO.empty();

        try {
            String norm = json.normalize(baseSpecJson);
            JsonNode root = json.parse(norm);

            List<SqlDTO> parts = new ArrayList<>();

            // 1) time range
            SqlDTO timePart = buildTimeClause(root.path("time"), typeMap, rawTemporalKindMap);
            if (!timePart.isBlank()) parts.add(timePart);

            // 2) conditions
            SqlDTO condPart = buildConditionsClause(
                    root.path("conditions"),
                    root.path("not").asBoolean(false),
                    typeMap
            );
            if (!condPart.isBlank()) parts.add(condPart);

            return SqlDTO.join(" AND ", parts);

        } catch (Exception e) {
            log.error("Failed to parse baseSpec: {}", baseSpecJson, e);
            throw new GridException(
                    GridException.ErrorCode.INVALID_BASE_SPEC,
                    "BaseSpec 파싱 실패: " + e.getMessage()
            );
        }
    }

    private SqlDTO buildTimeClause(JsonNode time,
                                   Map<String, String> typeMap,
                                   Map<String, String> rawTemporalKindMap) {

        if (!time.isObject()) return SqlDTO.empty();

        String field = textOrNull(time, "field");
        Long from = time.hasNonNull("fromEpoch") ? time.get("fromEpoch").asLong() : null;
        Long to = time.hasNonNull("toEpoch") ? time.get("toEpoch").asLong() : null;

        if (field == null || from == null || to == null) return SqlDTO.empty();

        boolean inclusive = time.path("inclusive").asBoolean(true);
        String fType = typeMap != null ? typeMap.getOrDefault(field, "") : "";
        String raw = rawTemporalKindMap != null ? rawTemporalKindMap.getOrDefault(field, "") : "";

        return temporal.timeRangeClause("t", field, from, to, inclusive, fType, raw);
    }

    private SqlDTO buildConditionsClause(JsonNode conds, boolean not, Map<String, String> typeMap) {

        if (!conds.isArray() || conds.size() == 0) return SqlDTO.empty();

        List<SqlDTO> cParts = new ArrayList<>();

        for (int i = 0; i < conds.size(); i++) {
            JsonNode c = conds.get(i);

            String field = textOrNull(c, "field");
            String op = textOrNull(c, "op");
            if (field == null || op == null) continue;

            String dataType = resolveDataType(c, field, typeMap);
            List<String> vals = readStringValues(c.path("values"));

            SqlDTO expr = searchMapper.map("t", field, op.toUpperCase(), dataType, vals);
            if (expr.isBlank()) continue;

            if (i > 0) {
                String join = c.path("join").asText("AND").toUpperCase();
                cParts.add(SqlDTO.raw(join));
            }

            cParts.add(SqlDTO.wrap(expr));
        }

        if (cParts.isEmpty()) return SqlDTO.empty();

        SqlDTO merged = SqlDTO.sequence(cParts);
        return not
                ? SqlDTO.of("NOT (" + merged.getSql() + ")", merged.getArgs())
                : SqlDTO.of("(" + merged.getSql() + ")", merged.getArgs());
    }

    private String resolveDataType(JsonNode c, String field, Map<String, String> typeMap) {
        String dt = optUpper(c, "dataType");
        if (dt != null && !dt.isBlank()) return dt;

        if (typeMap != null && typeMap.get(field) != null) {
            return typeMap.get(field).toUpperCase();
        }

        return "TEXT";
    }

    private Number parseNum(String s) {
        if (s == null) return 0;
        try { return s.contains(".") ? Double.valueOf(s) : Long.valueOf(s); }
        catch (Exception e) { return 0; }
    }
}
