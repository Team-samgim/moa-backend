package com.moa.api.grid.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QueryBuilder {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String buildSelectSQL(String layer, String sortField, String sortDirection, String filterModel) {
        // layer → 실제 테이블명 매핑
        String tableName = switch (layer.toLowerCase()) {
            case "http_page" -> "page_sample";
            default -> "ethernet_sample";
        };

        StringBuilder sql = new StringBuilder("SELECT * FROM " + tableName);

        // 필터 처리
        if (filterModel != null && !filterModel.isBlank()) {
            try {
                Map<String, Map<String, Object>> filters = mapper.readValue(filterModel, Map.class);
                List<String> conditions = new ArrayList<>();

                for (String col : filters.keySet()) {
                    Map<String, Object> filter = filters.get(col);
                    String type = (String) filter.get("type");

                    if ("set".equalsIgnoreCase(type)) {
                        List<?> values = (List<?>) filter.get("values");
                        if (values != null && !values.isEmpty()) {
                            String inClause = values.stream()
                                    .map(Object::toString)
                                    .map(v -> "'" + v + "'")
                                    .collect(Collectors.joining(","));
                            conditions.add("\"" + col + "\" IN (" + inClause + ")");
                        }
                    }
                }
                if (!conditions.isEmpty()) {
                    sql.append(" WHERE ").append(String.join(" AND ", conditions));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 정렬 처리
        if (sortField != null && !sortField.isBlank()) {
            String direction = "DESC".equalsIgnoreCase(sortDirection) ? "DESC" : "ASC";
            sql.append(" ORDER BY \"").append(sortField).append("\" ").append(direction);
        }

        return sql.toString();
    }
}
