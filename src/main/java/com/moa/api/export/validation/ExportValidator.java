package com.moa.api.export.validation;

/*****************************************************************************
 CLASS NAME    : ExportValidator
 DESCRIPTION   : Export 관련 요청의 입력값(레이어, 컬럼, 파일명 등) 검증
 AUTHOR        : 방대혁
 ******************************************************************************/

import com.moa.api.export.dto.ExportGridRequestDTO;
import com.moa.api.export.exception.ExportException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Export 입력값 검증기
 */
@Slf4j
@Component
public class ExportValidator {

    private static final List<String> VALID_LAYERS = List.of(
            "ethernet",
            "tcp",
            "http_page",
            "http_uri"
    );
    private static final int MAX_FILE_NAME_LENGTH = 120;

    /**
     * Grid Export 요청 검증
     */
    public void validateGridExportRequest(ExportGridRequestDTO req) {
        // Layer 검증
        if (req.getLayer() == null || req.getLayer().isBlank()) {
            throw new ExportException(
                    ExportException.ErrorCode.INVALID_LAYER,
                    "Layer는 필수입니다"
            );
        }

        String layer = req.getLayer().trim().toLowerCase();
        if (!VALID_LAYERS.contains(layer)) {
            throw new ExportException(
                    ExportException.ErrorCode.INVALID_LAYER,
                    "유효하지 않은 레이어: " + layer
            );
        }

        // Columns 검증
        if (req.getColumns() == null || req.getColumns().isEmpty()) {
            throw new ExportException(
                    ExportException.ErrorCode.INVALID_COLUMNS,
                    "최소 1개 이상의 컬럼이 필요합니다"
            );
        }

        // 빈 컬럼명 체크
        boolean hasEmptyColumn = req.getColumns().stream()
                .anyMatch(col -> col == null || col.isBlank());

        if (hasEmptyColumn) {
            throw new ExportException(
                    ExportException.ErrorCode.INVALID_COLUMNS,
                    "빈 컬럼명이 포함되어 있습니다"
            );
        }

        // 파일명 검증 (옵션)
        if (req.getFileName() != null && !req.getFileName().isBlank()) {
            validateFileName(req.getFileName());
        }
    }

    /**
     * 파일명 유효성 검증
     */
    public void validateFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return; // null/blank는 허용 (서버에서 자동 생성)
        }

        String trimmed = fileName.trim();

        // 길이 체크
        if (trimmed.length() > MAX_FILE_NAME_LENGTH) {
            throw new ExportException(
                    ExportException.ErrorCode.INVALID_FILE_NAME,
                    "파일명이 너무 깁니다 (최대 " + MAX_FILE_NAME_LENGTH + "자)"
            );
        }

        // 위험한 문자 체크
        if (containsDangerousCharacters(trimmed)) {
            throw new ExportException(
                    ExportException.ErrorCode.INVALID_FILE_NAME,
                    "파일명에 유효하지 않은 문자가 포함되어 있습니다"
            );
        }
    }

    /**
     * 위험한 문자 포함 여부 체크
     */
    private boolean containsDangerousCharacters(String fileName) {
        // 경로 구분자
        if (fileName.contains("/") || fileName.contains("\\")) {
            return true;
        }

        // Null 바이트
        if (fileName.contains("\u0000")) {
            return true;
        }

        // 제어 문자
        if (fileName.matches(".*[\\x00-\\x1F\\x7F].*")) {
            return true;
        }

        return false;
    }

    /**
     * Layer 정규화
     */
    public String normalizeLayer(String layer) {
        if (layer == null || layer.isBlank()) {
            return "ethernet"; // 기본값
        }
        return layer.trim().toLowerCase();
    }
}
