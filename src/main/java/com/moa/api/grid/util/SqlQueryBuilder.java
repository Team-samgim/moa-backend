package com.moa.api.grid.util;

import com.moa.api.grid.dto.SqlDTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * SQL 쿼리를 Fluent API로 조립하는 Builder
 *
 * 사용 예시:
 * SqlDTO query = SqlQueryBuilder
 *     .select("col1", "col2")
 *     .from("table t")
 *     .where(whereClause)
 *     .orderBy("col1", "DESC")
 *     .limit(100)
 *     .build();
 */
public class SqlQueryBuilder {

    private final List<String> selectColumns = new ArrayList<>();
    private String fromClause;
    private SqlDTO whereClause = SqlDTO.empty();
    private final List<String> orderByColumns = new ArrayList<>();
    private Integer limit;
    private Integer offset;
    private final List<String> groupByColumns = new ArrayList<>();
    private boolean distinct = false;

    private SqlQueryBuilder() {}

    public static SqlQueryBuilder select(String... columns) {
        SqlQueryBuilder builder = new SqlQueryBuilder();
        builder.selectColumns.addAll(Arrays.asList(columns));
        return builder;
    }

    public static SqlQueryBuilder selectDistinct(String... columns) {
        SqlQueryBuilder builder = new SqlQueryBuilder();
        builder.distinct = true;
        builder.selectColumns.addAll(Arrays.asList(columns));
        return builder;
    }

    public SqlQueryBuilder from(String table) {
        this.fromClause = table;
        return this;
    }

    public SqlQueryBuilder where(SqlDTO clause) {
        if (clause != null && !clause.isBlank()) {
            this.whereClause = clause;
        }
        return this;
    }

    public SqlQueryBuilder andWhere(SqlDTO additionalClause) {
        if (additionalClause != null && !additionalClause.isBlank()) {
            this.whereClause = SqlDTO.and(this.whereClause, additionalClause);
        }
        return this;
    }

    public SqlQueryBuilder orderBy(String column, String direction) {
        String dir = "DESC".equalsIgnoreCase(direction) ? "DESC" : "ASC";
        this.orderByColumns.add(column + " " + dir);
        return this;
    }

    public SqlQueryBuilder orderBy(String orderByClause) {
        this.orderByColumns.add(orderByClause);
        return this;
    }

    public SqlQueryBuilder groupBy(String... columns) {
        this.groupByColumns.addAll(Arrays.asList(columns));
        return this;
    }

    public SqlQueryBuilder limit(int limit) {
        this.limit = Math.max(1, limit);
        return this;
    }

    public SqlQueryBuilder offset(int offset) {
        this.offset = Math.max(0, offset);
        return this;
    }

    public SqlDTO build() {
        if (fromClause == null || fromClause.isBlank()) {
            throw new IllegalStateException("FROM clause is required");
        }

        StringBuilder sql = new StringBuilder();
        List<Object> args = new ArrayList<>();

        // SELECT
        sql.append("SELECT ");
        if (distinct) {
            sql.append("DISTINCT ");
        }
        if (selectColumns.isEmpty()) {
            sql.append("*");
        } else {
            sql.append(String.join(", ", selectColumns));
        }

        // FROM
        sql.append(" FROM ").append(fromClause);

        // WHERE
        if (!whereClause.isBlank()) {
            sql.append(" WHERE ").append(whereClause.getSql());
            args.addAll(whereClause.getArgs());
        }

        // GROUP BY
        if (!groupByColumns.isEmpty()) {
            sql.append(" GROUP BY ").append(String.join(", ", groupByColumns));
        }

        // ORDER BY
        if (!orderByColumns.isEmpty()) {
            sql.append(" ORDER BY ").append(String.join(", ", orderByColumns));
        }

        // LIMIT
        if (limit != null) {
            sql.append(" LIMIT ").append(limit);
        }

        // OFFSET
        if (offset != null) {
            sql.append(" OFFSET ").append(offset);
        }

        return new SqlDTO(sql.toString(), args);
    }

    /**
     * CTE(Common Table Expression) 빌더
     */
    public static class CteBuilder {
        private final List<Cte> ctes = new ArrayList<>();
        private SqlQueryBuilder mainQuery;

        public CteBuilder with(String name, SqlDTO query) {
            ctes.add(new Cte(name, query));
            return this;
        }

        public CteBuilder with(String name, SqlQueryBuilder builder) {
            ctes.add(new Cte(name, builder.build()));
            return this;
        }

        public CteBuilder mainQuery(SqlQueryBuilder builder) {
            this.mainQuery = builder;
            return this;
        }

        public SqlDTO build() {
            if (ctes.isEmpty()) {
                return mainQuery.build();
            }

            StringBuilder sql = new StringBuilder("WITH ");
            List<Object> args = new ArrayList<>();

            for (int i = 0; i < ctes.size(); i++) {
                if (i > 0) sql.append(", ");
                Cte cte = ctes.get(i);
                sql.append(cte.name).append(" AS (\n");
                sql.append(cte.query.getSql());
                sql.append("\n)");
                args.addAll(cte.query.getArgs());
            }

            sql.append("\n");
            SqlDTO main = mainQuery.build();
            sql.append(main.getSql());
            args.addAll(main.getArgs());

            return new SqlDTO(sql.toString(), args);
        }

        private record Cte(String name, SqlDTO query) {}
    }

    public static CteBuilder cte() {
        return new CteBuilder();
    }
}