package com.moa.api.grid.util;

import com.moa.api.grid.dto.SqlDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * SqlQueryBuilder 테스트
 */
class SqlQueryBuilderTest {

    @Test
    void select_기본_쿼리_생성() {
        // given
        SqlDTO where = SqlDTO.of("age > ?", List.of(20));

        // when
        SqlDTO result = SqlQueryBuilder
                .select("name", "age")
                .from("users")
                .where(where)
                .orderBy("age", "DESC")
                .limit(10)
                .build();

        // then
        assertThat(result.getSql())
                .isEqualTo("SELECT name, age FROM users WHERE age > ? ORDER BY age DESC LIMIT 10");
        assertThat(result.getArgs()).containsExactly(20);
    }

    @Test
    void select_WHERE절_없이_쿼리_생성() {
        // when
        SqlDTO result = SqlQueryBuilder
                .select("*")
                .from("users")
                .orderBy("id", "ASC")
                .build();

        // then
        assertThat(result.getSql())
                .isEqualTo("SELECT * FROM users ORDER BY id ASC");
        assertThat(result.getArgs()).isEmpty();
    }

    @Test
    void selectDistinct_DISTINCT_쿼리_생성() {
        // when
        SqlDTO result = SqlQueryBuilder
                .selectDistinct("city")
                .from("users")
                .build();

        // then
        assertThat(result.getSql())
                .startsWith("SELECT DISTINCT city");
    }

    @Test
    void groupBy_집계_쿼리_생성() {
        // when
        SqlDTO result = SqlQueryBuilder
                .select("city", "COUNT(*) AS cnt")
                .from("users")
                .groupBy("city")
                .orderBy("cnt", "DESC")
                .build();

        // then
        assertThat(result.getSql())
                .contains("GROUP BY city");
    }

    @Test
    void cte_WITH절_쿼리_생성() {
        // given
        SqlDTO baseQuery = SqlDTO.of(
                "SELECT id, name FROM users WHERE age > ?",
                List.of(20)
        );

        SqlQueryBuilder mainQuery = SqlQueryBuilder
                .select("name")
                .from("base")
                .orderBy("name", "ASC");

        // when
        SqlDTO result = SqlQueryBuilder.cte()
                .with("base", baseQuery)
                .mainQuery(mainQuery)
                .build();

        // then
        assertThat(result.getSql())
                .startsWith("WITH base AS")
                .contains("SELECT id, name FROM users WHERE age > ?")
                .contains("SELECT name FROM base");
        assertThat(result.getArgs()).containsExactly(20);
    }

    @Test
    void cte_다중_CTE_생성() {
        // given
        SqlDTO cte1 = SqlDTO.of("SELECT * FROM users", List.of());
        SqlDTO cte2 = SqlDTO.of("SELECT * FROM orders", List.of());

        SqlQueryBuilder mainQuery = SqlQueryBuilder
                .select("*")
                .from("users_cte, orders_cte");

        // when
        SqlDTO result = SqlQueryBuilder.cte()
                .with("users_cte", cte1)
                .with("orders_cte", cte2)
                .mainQuery(mainQuery)
                .build();

        // then
        assertThat(result.getSql())
                .contains("WITH users_cte AS")
                .contains(", orders_cte AS");
    }

    @Test
    void andWhere_WHERE절_추가() {
        // given
        SqlDTO where1 = SqlDTO.of("age > ?", List.of(20));
        SqlDTO where2 = SqlDTO.of("city = ?", List.of("Seoul"));

        // when
        SqlDTO result = SqlQueryBuilder
                .select("*")
                .from("users")
                .where(where1)
                .andWhere(where2)
                .build();

        // then
        assertThat(result.getSql())
                .contains("WHERE age > ? AND city = ?");
        assertThat(result.getArgs()).containsExactly(20, "Seoul");
    }

    @Test
    void build_FROM절_없으면_예외발생() {
        // when & then
        assertThatThrownBy(() ->
                SqlQueryBuilder
                        .select("name")
                        .build()
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FROM clause is required");
    }

    @Test
    void offset_페이징_쿼리_생성() {
        // when
        SqlDTO result = SqlQueryBuilder
                .select("*")
                .from("users")
                .orderBy("id", "ASC")
                .offset(10)
                .limit(5)
                .build();

        // then
        assertThat(result.getSql())
                .endsWith("ORDER BY id ASC LIMIT 5 OFFSET 10");
    }
}