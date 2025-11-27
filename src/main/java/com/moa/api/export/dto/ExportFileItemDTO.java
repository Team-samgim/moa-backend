package com.moa.api.export.dto;

/*****************************************************************************
 CLASS NAME    : ExportFileItemDTO
 DESCRIPTION   : Export 파일 목록 조회 시 사용하는 단일 파일 항목 DTO
 AUTHOR        : 방대혁
 ******************************************************************************/
/**
 * Export 파일 단건 DTO
 * - Grid/Pivot/Chart Export 이력 조회용
 * - 프리셋/레이어/생성시각 등의 메타 정보 포함
 */

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExportFileItemDTO {

    /** Export 메타데이터 PK (export_id) */
    private Long fileId;          // = exportId

    /** 실제 파일명 (S3 objectKey의 마지막 segment 등) */
    private String fileName;

    /** Export 타입 (GRID / PIVOT / CHART) */
    private String exportType;

    /** Export 대상 레이어 (preset.config.layer) */
    private String layer;

    /** Export 생성 시각 */
    private LocalDateTime createdAt;

    /** 사용된 프리셋 ID (없을 수 있음) */
    private Integer presetId;

    /** (옵션) 프리셋 config 원본 JSON */
    private JsonNode config;
}
