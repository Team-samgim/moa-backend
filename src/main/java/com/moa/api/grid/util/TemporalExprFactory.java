package com.moa.api.grid.util;

import com.moa.api.grid.config.GridProperties;
import com.moa.api.grid.dto.SqlDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 시간 관련 SQL 표현식 생성
 *
 * 변경사항:
 * 1. GridProperties 주입 (하드코딩된 타임존 제거)
 */
@Component
@RequiredArgsConstructor
public class TemporalExprFactory {

    private final GridProperties properties;

    /**
     * 타임존 변환 후 date로 캐스팅하는 표현식 생성
     *
     * @param alias 테이블 별칭 (예: t)
     * @param field 컬럼명
     * @param rawTemporalKind timestamptz | timestamp | date
     * @return SQL 표현식
     */
    public String toDateExpr(String alias, String field, String rawTemporalKind) {
        String quotedField = SqlIdentifier.quoteWithAlias(alias, field);
        String timezone = properties.getDefaultTimeZone();

        return switch (rawTemporalKind.toLowerCase()) {
            case "timestamptz" ->
                    String.format("(%s AT TIME ZONE '%s')::date", quotedField, timezone);

            case "timestamp" -> {
                if (properties.getTsWoTzIsUtc()) {
                    // UTC로 간주하고 변환
                    yield String.format("((%s AT TIME ZONE 'UTC') AT TIME ZONE '%s')::date",
                            quotedField, timezone);
                } else {
                    // 로컬 타임으로 간주
                    yield quotedField + "::date";
                }
            }

            default -> quotedField + "::date";
        };
    }

    /**
     * 시간 범위 조건 생성
     *
     * @param alias 테이블 별칭
     * @param field 컬럼명
     * @param fromEpoch 시작 Unix timestamp (초)
     * @param toEpoch 종료 Unix timestamp (초)
     * @param inclusive true면 <=, false면 <
     * @param fType 필드 타입 (number | date)
     * @param rawKind timestamptz | timestamp | date
     * @return SqlDTO (쿼리 + 파라미터)
     */
    public SqlDTO timeRangeClause(
            String alias,
            String field,
            long fromEpoch,
            long toEpoch,
            boolean inclusive,
            String fType,
            String rawKind) {

        String quotedField = SqlIdentifier.quoteWithAlias(alias, field);
        String geOp = inclusive ? ">=" : ">";
        String leOp = inclusive ? "<=" : "<";

        // number 타입은 그대로 비교
        if ("number".equalsIgnoreCase(fType)) {
            String condition = String.format("%s %s ? AND %s %s ?",
                    quotedField, geOp, quotedField, leOp);
            return SqlDTO.of("(" + condition + ")", List.of(fromEpoch, toEpoch));
        }

        // timestamptz: to_timestamp() 사용
        if ("timestamptz".equalsIgnoreCase(rawKind)) {
            String condition = String.format("%s %s to_timestamp(?) AND %s %s to_timestamp(?)",
                    quotedField, geOp, quotedField, leOp);
            return SqlDTO.of("(" + condition + ")", List.of(fromEpoch, toEpoch));
        }

        // timestamp: UTC 변환 후 비교
        if ("timestamp".equalsIgnoreCase(rawKind)) {
            String utcField = String.format("(%s AT TIME ZONE 'UTC')", quotedField);
            String condition = String.format("%s %s to_timestamp(?) AND %s %s to_timestamp(?)",
                    utcField, geOp, utcField, leOp);
            return SqlDTO.of("(" + condition + ")", List.of(fromEpoch, toEpoch));
        }

        // date: to_timestamp()::date로 변환
        if ("date".equalsIgnoreCase(fType)) {
            String condition = String.format("%s %s to_timestamp(?)::date AND %s %s to_timestamp(?)::date",
                    quotedField, geOp, quotedField, leOp);
            return SqlDTO.of("(" + condition + ")", List.of(fromEpoch, toEpoch));
        }

        return SqlDTO.empty();
    }
}