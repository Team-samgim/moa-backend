package com.moa.api.detail.page.validation;

import com.moa.api.detail.page.exception.HttpPageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * HTTP Page 입력값 검증기
 */
@Slf4j
@Component
public class HttpPageValidator {

    /**
     * Row Key 유효성 검증
     */
    public void validateRowKey(String rowKey) {
        if (rowKey == null || rowKey.isBlank()) {
            throw new HttpPageException(
                    HttpPageException.ErrorCode.INVALID_ROW_KEY,
                    "Row Key는 필수입니다"
            );
        }

        // 최소 길이 체크
        if (rowKey.length() < 10) {
            throw new HttpPageException(
                    HttpPageException.ErrorCode.INVALID_ROW_KEY,
                    "Row Key가 너무 짧습니다"
            );
        }

        // 최대 길이 체크 (500자)
        if (rowKey.length() > 500) {
            throw new HttpPageException(
                    HttpPageException.ErrorCode.INVALID_ROW_KEY,
                    "Row Key가 너무 깁니다"
            );
        }

        // SQL Injection 방지
        if (containsSqlInjection(rowKey)) {
            throw new HttpPageException(
                    HttpPageException.ErrorCode.INVALID_ROW_KEY,
                    "유효하지 않은 문자가 포함되어 있습니다"
            );
        }
    }

    /**
     * SQL Injection 패턴 검사
     */
    private boolean containsSqlInjection(String input) {
        String lower = input.toLowerCase();

        // SQL 키워드 체크
        String[] sqlKeywords = {
                "select", "insert", "update", "delete", "drop",
                "create", "alter", "truncate", "exec", "union",
                "script", "javascript", "onload", "onerror"
        };

        for (String keyword : sqlKeywords) {
            if (lower.contains(keyword)) {
                return true;
            }
        }

        // 특수 문자 체크 (', ", ;, --, /*, */)
        if (input.contains("'") || input.contains("\"") ||
                input.contains(";") || input.contains("--") ||
                input.contains("/*") || input.contains("*/")) {
            return true;
        }

        return false;
    }
}