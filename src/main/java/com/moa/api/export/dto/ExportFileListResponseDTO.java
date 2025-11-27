package com.moa.api.export.dto;

/*****************************************************************************
 CLASS NAME    : ExportFileListResponseDTO
 DESCRIPTION   : Export 파일 목록 + 페이징 정보를 담는 응답 DTO
 AUTHOR        : 방대혁
 ******************************************************************************/
/**
 * Export 파일 목록 응답 DTO
 * - Export 파일 리스트와 페이지 정보를 함께 전달
 */

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportFileListResponseDTO {

    /** Export 파일 항목 리스트 */
    private List<ExportFileItemDTO> items;

    /** 현재 페이지 번호(0-base) */
    private int page;

    /** 페이지 크기 */
    private int size;

    /** 전체 페이지 수 */
    private int totalPages;

    /** 전체 항목 수 */
    private long totalItems;
}
