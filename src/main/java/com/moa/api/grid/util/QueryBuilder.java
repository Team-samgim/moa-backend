package com.moa.api.grid.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;

@Slf4j
@Component
public class QueryBuilder {

    private final ObjectMapper mapper = new ObjectMapper();

    public String buildSelectSQL(String layer, String sortField, String sortDirection,
                                 String filterModel, int offset, int limit) {

        String tableName = switch (layer.toLowerCase()) {
            case "http_page" -> "page_sample";
            default -> "ethernet_sample";
        };

        StringBuilder sql = new StringBuilder("SELECT * FROM " + tableName);

        // ✅ WHERE 절 생성
        String where = buildWhereClause(filterModel);
        if (!where.isEmpty()) {
            sql.append(" WHERE ").append(where);
        }

        // ✅ 정렬 조건
        if (sortField != null && sortDirection != null) {
            sql.append(" ORDER BY \"").append(sortField).append("\" ").append(sortDirection);
        }

        // ✅ 페이징
        if (limit > 0) {
            sql.append(" OFFSET ").append(offset).append(" LIMIT ").append(limit);
        }

        return sql.toString();
    }

    private String buildWhereClause(String filterModel) {
        if (filterModel == null || filterModel.isEmpty() || filterModel.equals("{}")) return "";

        try {
            JsonNode root = mapper.readTree(filterModel);
            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            StringBuilder where = new StringBuilder();
            boolean first = true;

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String field = entry.getKey();
                JsonNode filter = entry.getValue();

                if (!first) where.append(" AND ");
                first = false;

                if (filter.has("values")) {
                    JsonNode values = filter.get("values");
                    where.append("\"").append(field).append("\" IN ").append(buildInClause(values));
                }
            }

            log.info("[QueryBuilder] WHERE clause built: {}", where);
            return where.toString();

        } catch (Exception e) {
            log.error("[QueryBuilder] Failed to parse filterModel: {}", filterModel, e);
            return "";
        }
    }

    private String buildInClause(JsonNode values) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("'").append(values.get(i).asText()).append("'");
        }
        sb.append(")");
        return sb.toString();
    }
}
