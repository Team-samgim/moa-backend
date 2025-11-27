package com.moa.api.grid.util;

import com.moa.api.grid.dto.SqlDTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*****************************************************************************
 CLASS NAME    : SqlQueryBuilder
 DESCRIPTION   : Fluent API 스타일로 SQL 쿼리를 안전하게 조립하기 위한 빌더 클래스.
 - SELECT / FROM / WHERE / GROUP BY / ORDER BY / LIMIT / OFFSET
 - DISTINCT 지원
 - CTE(Common Table Expression) 조립 지원
 AUTHOR        : 방대혁
 ******************************************************************************/
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

    /*************************************************************************
     * SELECT 절 생성
     *************************************************************************/
    public static SqlQueryBuilder select(String... columns) {
        SqlQueryBuilder builder = new SqlQueryBuilder();
        builder.selectColumns.addAll(Arrays.asList(columns));
        return builder;
    }

    /*************************************************************************
     * SELECT DISTINCT 절 생성
     *************************************************************************/
    public static SqlQueryBuilder selectDistinct(String... columns) {
        SqlQueryBuilder builder = new SqlQueryBuilder();
        builder.distinct = true;
        builder.selectColumns.addAll(Arrays.asList(columns));
        return builder;
    }

    /*************************************************************************
     * FROM 절 설정
     *************************************************************************/
    public SqlQueryBuilder from(String table) {
        this.fromClause = table;
        return this;
    }

    /*************************************************************************
     * WHERE 절 설정
     *************************************************************************/
    public SqlQueryBuilder where(SqlDTO clause) {
        if (clause != null && !clause.isBlank()) {
            this.whereClause = clause;
        }
        return this;
    }

    /*************************************************************************
     * WHERE 절 추가 (AND)
     *************************************************************************/
    public SqlQueryBuilder andWhere(SqlDTO additionalClause) {
        if (additionalClause != null && !additionalClause.isBlank()) {
            this.whereClause = SqlDTO.and(this.whereClause, additionalClause);
        }
        return this;
    }

    /*************************************************************************
     * ORDER BY 설정 (column + direction)
     *************************************************************************/
    public SqlQueryBuilder orderBy(String column, String direction) {
        String dir = "DESC".equalsIgnoreCase(direction) ? "DESC" : "ASC";
        this.orderByColumns.add(column + " " + dir);
        return this;
    }

    /*************************************************************************
     * ORDER BY 전체 구문 직접 전달
     *************************************************************************/
    public SqlQueryBuilder orderBy(String orderByClause) {
        this.orderByColumns.add(orderByClause);
        return this;
    }

    /*************************************************************************
     * GROUP BY 설정
     *************************************************************************/
    public SqlQueryBuilder groupBy(String... columns) {
        this.groupByColumns.addAll(Arrays.asList(columns));
        return this;
    }

    /*************************************************************************
     * LIMIT 설정
     *************************************************************************/
    public SqlQueryBuilder limit(int limit) {
        this.limit = Math.max(1, limit);
        return this;
    }

    /*************************************************************************
     * OFFSET 설정
     *************************************************************************/
    public SqlQueryBuilder offset(int offset) {
        this.offset = Math.max(0, offset);
        return this;
    }

    /*************************************************************************
     * SQLDTO 빌드
     *************************************************************************/
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

    /*************************************************************************
     * CTE(Common Table Expression) Builder
     *************************************************************************/
    public static class CteBuilder {
        private final List<Cte> ctes = new ArrayList<>();
        private SqlQueryBuilder mainQuery;

        /*********************************************************************
         * WITH name AS (raw SqlDTO)
         *********************************************************************/
        public CteBuilder with(String name, SqlDTO query) {
            ctes.add(new Cte(name, query));
            return this;
        }

        /*********************************************************************
         * WITH name AS (builder.build())
         *********************************************************************/
        public CteBuilder with(String name, SqlQueryBuilder builder) {
            ctes.add(new Cte(name, builder.build()));
            return this;
        }

        /*********************************************************************
         * 메인 쿼리 설정
         *********************************************************************/
        public CteBuilder mainQuery(SqlQueryBuilder builder) {
            this.mainQuery = builder;
            return this;
        }

        /*********************************************************************
         * 최종 SQLDTO 구성
         *********************************************************************/
        public SqlDTO build() {
            if (ctes.isEmpty()) {
                return mainQuery.build();
            }

            StringBuilder sql = new StringBuilder("WITH ");
            List<Object> args = new ArrayList<>();

            // CTE 목록 조립
            for (int i = 0; i < ctes.size(); i++) {
                if (i > 0) sql.append(", ");
                Cte cte = ctes.get(i);

                sql.append(cte.name).append(" AS (\n");
                sql.append(cte.query.getSql());
                sql.append("\n)");

                args.addAll(cte.query.getArgs());
            }

            sql.append("\n");

            // Main Query 병합
            SqlDTO main = mainQuery.build();
            sql.append(main.getSql());
            args.addAll(main.getArgs());

            return new SqlDTO(sql.toString(), args);
        }

        private record Cte(String name, SqlDTO query) {}
    }

    /*************************************************************************
     * CTE 시작 메서드
     *************************************************************************/
    public static CteBuilder cte() {
        return new CteBuilder();
    }
}
