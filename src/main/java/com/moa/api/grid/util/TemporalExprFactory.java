package com.moa.api.grid.util;

import com.moa.api.grid.config.GridProperties;
import com.moa.api.grid.dto.SqlDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/*****************************************************************************
 CLASS NAME    : TemporalExprFactory
 DESCRIPTION   : Grid에서 사용하는 시간 관련 SQL 표현식 생성 유틸리티 클래스.
 - timestamptz / timestamp / date 필드 처리
 - 타임존 보정
 - 날짜 캐스팅
 - 범위 검색(time-range) SqlDTO 생성
 AUTHOR        : 방대혁
 ******************************************************************************/
@Component
@RequiredArgsConstructor
public class TemporalExprFactory {

    private final GridProperties properties;

    /*************************************************************************
     * 타임존 보정 후 date로 캐스팅하는 SQL 표현식 생성
     *
     * @param alias            테이블 별칭
     * @param field            컬럼명
     * @param rawTemporalKind  timestamptz | timestamp | date
     * @return SQL 문자열
     *************************************************************************/
    public String toDateExpr(String alias, String field, String rawTemporalKind) {
        String quotedField = SqlIdentifier.quoteWithAlias(alias, field);
        String timezone = properties.getDefaultTimeZone();

        return switch (rawTemporalKind.toLowerCase()) {

            // timestamptz → 해당 타임존으로 변환 후 date
            case "timestamptz" ->
                    String.format("(%s AT TIME ZONE '%s')::date", quotedField, timezone);

            // timestamp → 시스템 플래그(tsWoTzIsUtc)에 따라 처리
            case "timestamp" -> {
                if (properties.getTsWoTzIsUtc()) {
                    // timestamp를 UTC로 저장했다고 간주 → KST 변환
                    yield String.format(
                            "((%s AT TIME ZONE 'UTC') AT TIME ZONE '%s')::date",
                            quotedField, timezone
                    );
                } else {
                    // timestamp는 local time으로 간주
                    yield quotedField + "::date";
                }
            }

            // 기본: date 필드 그대로 사용
            default -> quotedField + "::date";
        };
    }

    /*************************************************************************
     * 시간 범위 조건 SQL 생성
     *
     * @param alias      테이블 별칭
     * @param field      필드명
     * @param fromEpoch  시작 timestamp(Unix epoch seconds)
     * @param toEpoch    종료 timestamp(Unix epoch seconds)
     * @param inclusive  종료 포함 여부 (true = <=, false = <)
     * @param fType      number | date
     * @param rawKind    timestamptz | timestamp | date
     * @return SqlDTO (where 조건)
     *************************************************************************/
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

        /* ---------------------------------------------
         * ① number 타입 → epoch 그대로 비교 (집계 전처리된 필드)
         * --------------------------------------------- */
        if ("number".equalsIgnoreCase(fType)) {
            String cond = String.format(
                    "%s %s ? AND %s %s ?",
                    quotedField, geOp, quotedField, leOp
            );
            return SqlDTO.of("(" + cond + ")", List.of(fromEpoch, toEpoch));
        }

        /* ---------------------------------------------
         * ② timestamptz → to_timestamp(epoch) 바로 비교
         * --------------------------------------------- */
        if ("timestamptz".equalsIgnoreCase(rawKind)) {
            String cond = String.format(
                    "%s %s to_timestamp(?) AND %s %s to_timestamp(?)",
                    quotedField, geOp, quotedField, leOp
            );
            return SqlDTO.of("(" + cond + ")", List.of(fromEpoch, toEpoch));
        }

        /* ---------------------------------------------
         * ③ timestamp → UTC → KST 변환 후 비교
         * --------------------------------------------- */
        if ("timestamp".equalsIgnoreCase(rawKind)) {
            // UTC로 간주하여 비교해야 하는 경우 존재 → (col AT TIME ZONE 'UTC')
            String utcField = String.format("(%s AT TIME ZONE 'UTC')", quotedField);
            String cond = String.format(
                    "%s %s to_timestamp(?) AND %s %s to_timestamp(?)",
                    utcField, geOp, utcField, leOp
            );
            return SqlDTO.of("(" + cond + ")", List.of(fromEpoch, toEpoch));
        }

        /* ---------------------------------------------
         * ④ date → 날짜 비교용으로 epoch→date 변환
         * --------------------------------------------- */
        if ("date".equalsIgnoreCase(fType)) {
            String cond = String.format(
                    "%s %s to_timestamp(?)::date AND %s %s to_timestamp(?)::date",
                    quotedField, geOp, quotedField, leOp
            );
            return SqlDTO.of("(" + cond + ")", List.of(fromEpoch, toEpoch));
        }

        return SqlDTO.empty();
    }
}
