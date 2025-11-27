package com.moa.api.grid.util;

/*****************************************************************************
 CLASS NAME    : SearchConditionMapper
 DESCRIPTION   : SearchDTO(SearchSpec 기반 그리드 조회)의 단일 조건을
 SQL WHERE 조건(SqlDTO)으로 변환하는 매퍼 클래스.
 - TEXT/IP/MAC
 - NUMBER
 - DATETIME (epoch -> to_timestamp 변환)
 - BOOLEAN
 각각의 연산자(EQ, LIKE, BETWEEN, IN, IS_NULL 등)를 지원한다.
 AUTHOR        : 방대혁
 ******************************************************************************/

import com.moa.api.grid.dto.SqlDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.moa.api.grid.util.InClauseBuilder.placeholders;

public class SearchConditionMapper {

    /**
     * SearchSpec 단일 조건 → SqlDTO 변환
     *
     * @param alias     테이블 alias (보통 t)
     * @param field     컬럼명
     * @param op        연산자(EQ, LIKE, BETWEEN 등)
     * @param dataType  TEXT, NUMBER, DATETIME, BOOLEAN
     * @param values    값 목록
     */
    public SqlDTO map(String alias, String field, String op, String dataType,
                      java.util.List<String> values) {

        String f = SqlIdentifier.quoteWithAlias(alias, field);

        switch (dataType) {

            /*************************************************************************
             * TEXT / IP / MAC
             *************************************************************************/
            case "TEXT":
            case "IP":
            case "MAC": {
                String val = values.isEmpty() ? "" : values.get(0);

                return switch (op) {
                    case "LIKE" -> SqlDTO.of(f + "::text ILIKE ?", List.of("%" + val + "%"));
                    case "STARTS_WITH" -> SqlDTO.of(f + "::text ILIKE ?", List.of(val + "%"));
                    case "ENDS_WITH" -> SqlDTO.of(f + "::text ILIKE ?", List.of("%" + val));
                    case "EQ" -> SqlDTO.of(f + " = ?", List.of(val));
                    case "NE" -> SqlDTO.of(f + " <> ?", List.of(val));
                    case "IN" ->
                            SqlDTO.of(
                                    f + " IN (" + placeholders(values.size(), "?") + ")",
                                    new ArrayList<>(values)
                            );
                    case "IS_NULL" -> SqlDTO.raw(f + " IS NULL");
                    case "IS_NOT_NULL" -> SqlDTO.raw(f + " IS NOT NULL");
                    default -> SqlDTO.empty();
                };
            }

            /*************************************************************************
             * NUMBER
             *************************************************************************/
            case "NUMBER": {
                String v1 = values.isEmpty() ? null : values.get(0);
                String v2 = (values.size() > 1) ? values.get(1) : null;

                return switch (op) {
                    case "EQ" -> SqlDTO.of(f + " = ?", List.of(parseNum(v1)));
                    case "NE" -> SqlDTO.of(f + " <> ?", List.of(parseNum(v1)));
                    case "GT" -> SqlDTO.of(f + " > ?", List.of(parseNum(v1)));
                    case "GTE" -> SqlDTO.of(f + " >= ?", List.of(parseNum(v1)));
                    case "LT" -> SqlDTO.of(f + " < ?", List.of(parseNum(v1)));
                    case "LTE" -> SqlDTO.of(f + " <= ?", List.of(parseNum(v1)));
                    case "BETWEEN" ->
                            SqlDTO.of(f + " BETWEEN ? AND ?", List.of(parseNum(v1), parseNum(v2)));
                    case "IN" -> SqlDTO.of(
                            f + " IN (" + placeholders(values.size(), "?") + ")",
                            values.stream().map(this::parseNum).collect(Collectors.toList())
                    );
                    case "IS_NULL" -> SqlDTO.raw(f + " IS NULL");
                    case "IS_NOT_NULL" -> SqlDTO.raw(f + " IS NOT NULL");
                    default -> SqlDTO.empty();
                };
            }

            /*************************************************************************
             * DATETIME (epoch → to_timestamp)
             *************************************************************************/
            case "DATETIME": {
                String cast = "to_timestamp";
                List<Object> epochArgs =
                        values.stream().map(Long::valueOf).collect(Collectors.toList());

                return switch (op) {
                    case "GTE" ->
                            SqlDTO.of(f + " >= " + cast + "(?)", epochArgs.subList(0, 1));
                    case "LT" ->
                            SqlDTO.of(f + " < " + cast + "(?)", epochArgs.subList(0, 1));
                    case "BETWEEN" ->
                            SqlDTO.of(
                                    f + " BETWEEN " + cast + "(?) AND " + cast + "(?)",
                                    epochArgs.subList(0, Math.min(2, epochArgs.size()))
                            );
                    case "IN" ->
                            SqlDTO.of(
                                    f + " IN (" +
                                            values.stream()
                                                    .map(v -> cast + "(?)")
                                                    .collect(Collectors.joining(", "))
                                            + ")",
                                    epochArgs
                            );
                    case "IS_NULL" -> SqlDTO.raw(f + " IS NULL");
                    case "IS_NOT_NULL" -> SqlDTO.raw(f + " IS NOT NULL");
                    default -> SqlDTO.empty();
                };
            }

            /*************************************************************************
             * BOOLEAN
             *************************************************************************/
            case "BOOLEAN": {
                String bool = values.isEmpty() ? "false" : values.get(0);

                return switch (op) {
                    case "IS_NULL" -> SqlDTO.raw(f + " IS NULL");
                    case "IS_NOT_NULL" -> SqlDTO.raw(f + " IS NOT NULL");
                    case "EQ" -> SqlDTO.of(f + " = ?", List.of(Boolean.valueOf(bool)));
                    default -> SqlDTO.empty();
                };
            }

            /*************************************************************************
             * 지원하지 않는 타입
             *************************************************************************/
            default:
                return SqlDTO.empty();
        }
    }

    /**
     * 문자열 숫자 파싱
     */
    private Number parseNum(String s) {
        if (s == null) return 0;
        try {
            return s.contains(".") ? Double.valueOf(s) : Long.valueOf(s);
        } catch (Exception e) {
            return 0;
        }
    }
}
