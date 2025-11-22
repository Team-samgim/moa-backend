package com.moa.api.grid.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * SqlDTO 테스트
 */
class SqlDTOTest {

    @Test
    void of_SQL과_파라미터_생성() {
        // when
        SqlDTO dto = SqlDTO.of("SELECT * FROM users WHERE age > ?", List.of(20));

        // then
        assertThat(dto.getSql()).isEqualTo("SELECT * FROM users WHERE age > ?");
        assertThat(dto.getArgs()).containsExactly(20);
    }

    @Test
    void empty_빈_SqlDTO_생성() {
        // when
        SqlDTO dto = SqlDTO.empty();

        // then
        assertThat(dto.isBlank()).isTrue();
        assertThat(dto.getSql()).isEmpty();
        assertThat(dto.getArgs()).isEmpty();
    }

    @Test
    void raw_파라미터_없는_SQL() {
        // when
        SqlDTO dto = SqlDTO.raw("SELECT * FROM users");

        // then
        assertThat(dto.getSql()).isEqualTo("SELECT * FROM users");
        assertThat(dto.getArgs()).isEmpty();
    }

    @Test
    void and_두_조건_AND_결합() {
        // given
        SqlDTO dto1 = SqlDTO.of("age > ?", List.of(20));
        SqlDTO dto2 = SqlDTO.of("city = ?", List.of("Seoul"));

        // when
        SqlDTO result = SqlDTO.and(dto1, dto2);

        // then
        assertThat(result.getSql()).isEqualTo("age > ? AND city = ?");
        assertThat(result.getArgs()).containsExactly(20, "Seoul");
    }

    @Test
    void and_한쪽이_빈_경우() {
        // given
        SqlDTO dto1 = SqlDTO.of("age > ?", List.of(20));
        SqlDTO empty = SqlDTO.empty();

        // when
        SqlDTO result = SqlDTO.and(dto1, empty);

        // then
        assertThat(result.getSql()).isEqualTo("age > ?");
        assertThat(result.getArgs()).containsExactly(20);
    }

    @Test
    void and_둘_다_빈_경우() {
        // given
        SqlDTO empty1 = SqlDTO.empty();
        SqlDTO empty2 = SqlDTO.empty();

        // when
        SqlDTO result = SqlDTO.and(empty1, empty2);

        // then
        assertThat(result.isBlank()).isTrue();
    }

    @Test
    void join_여러_조건_구분자로_결합() {
        // given
        SqlDTO dto1 = SqlDTO.of("age > ?", List.of(20));
        SqlDTO dto2 = SqlDTO.of("city = ?", List.of("Seoul"));
        SqlDTO dto3 = SqlDTO.of("name LIKE ?", List.of("John%"));

        // when
        SqlDTO result = SqlDTO.join(" OR ", List.of(dto1, dto2, dto3));

        // then
        assertThat(result.getSql())
                .isEqualTo("age > ? OR city = ? OR name LIKE ?");
        assertThat(result.getArgs())
                .containsExactly(20, "Seoul", "John%");
    }

    @Test
    void join_빈_조건_필터링() {
        // given
        SqlDTO dto1 = SqlDTO.of("age > ?", List.of(20));
        SqlDTO empty = SqlDTO.empty();
        SqlDTO dto2 = SqlDTO.of("city = ?", List.of("Seoul"));

        // when
        SqlDTO result = SqlDTO.join(" AND ", List.of(dto1, empty, dto2));

        // then
        assertThat(result.getSql()).isEqualTo("age > ? AND city = ?");
        assertThat(result.getArgs()).containsExactly(20, "Seoul");
    }

    @Test
    void wrap_괄호_추가() {
        // given
        SqlDTO dto = SqlDTO.of("age > ? AND city = ?", List.of(20, "Seoul"));

        // when
        SqlDTO result = SqlDTO.wrap(dto);

        // then
        assertThat(result.getSql()).isEqualTo("(age > ? AND city = ?)");
        assertThat(result.getArgs()).containsExactly(20, "Seoul");
    }

    @Test
    void sequence_여러_조건_순차_결합() {
        // given
        SqlDTO dto1 = SqlDTO.of("age > ?", List.of(20));
        SqlDTO raw = SqlDTO.raw("AND");
        SqlDTO dto2 = SqlDTO.of("city = ?", List.of("Seoul"));

        // when
        SqlDTO result = SqlDTO.sequence(List.of(dto1, raw, dto2));

        // then
        assertThat(result.getSql()).isEqualTo("age > ? AND city = ?");
        assertThat(result.getArgs()).containsExactly(20, "Seoul");
    }

    @Test
    void isBlank_빈_SQL_판정() {
        // given
        SqlDTO empty = SqlDTO.empty();
        SqlDTO notEmpty = SqlDTO.of("SELECT 1", List.of());

        // then
        assertThat(empty.isBlank()).isTrue();
        assertThat(notEmpty.isBlank()).isFalse();
    }
}