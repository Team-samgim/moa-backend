package com.moa.api.grid.util;

import com.moa.api.grid.dto.SqlDTO;

import java.util.List;

public class TemporalExprFactory {
    private final String appTz;
    private final boolean tsWoTzIsUtc;

    public TemporalExprFactory() {
        this("Asia/Seoul", true);
    }

    public TemporalExprFactory(String appTz, boolean tsWoTzIsUtc) {
        this.appTz = appTz;
        this.tsWoTzIsUtc = tsWoTzIsUtc;
    }

    public String toDateExpr(String alias, String field, String rawTemporalKind) {
        String qf = SqlIdentifier.quoteWithAlias(alias, field);
        if ("timestamptz".equalsIgnoreCase(rawTemporalKind)) {
            return "(" + qf + " AT TIME ZONE '" + appTz + "')::date";
        }
        if ("timestamp".equalsIgnoreCase(rawTemporalKind)) {
            if (tsWoTzIsUtc) {
                return "((" + qf + " AT TIME ZONE 'UTC') AT TIME ZONE '" + appTz + "')::date";
            } else {
                return qf + "::date";
            }
        }
        return qf + "::date"; // date이면 그대로
    }

    public SqlDTO timeRangeClause(String alias, String field,
                                  long fromEpoch, long toEpoch, boolean inclusive,
                                  String fType, String rawKind) {
        String col = SqlIdentifier.quoteWithAlias(alias, field);
        String ge = inclusive ? ">=" : ">";
        String le = inclusive ? "<=" : "<";
        if ("number".equalsIgnoreCase(fType)) {
            return SqlDTO.of("(" + col + " " + ge + " ? AND " + col + " " + le + " ?)", List.of(fromEpoch, toEpoch));
        }
        if ("timestamptz".equalsIgnoreCase(rawKind)) {
            return SqlDTO.of("(" + col + " " + ge + " to_timestamp(?) AND " + col + " " + le + " to_timestamp(?))", List.of(fromEpoch, toEpoch));
        }
        if ("timestamp".equalsIgnoreCase(rawKind)) {
            return SqlDTO.of("((" + col + " AT TIME ZONE 'UTC') " + ge + " to_timestamp(?) AND (" + col + " AT TIME ZONE 'UTC') " + le + " to_timestamp(?))", List.of(fromEpoch, toEpoch));
        }
        if ("date".equalsIgnoreCase(fType)) {
            return SqlDTO.of("(" + col + " " + ge + " to_timestamp(?)::date AND " + col + " " + le + " to_timestamp(?)::date)", List.of(fromEpoch, toEpoch));
        }
        return SqlDTO.empty();
    }
}