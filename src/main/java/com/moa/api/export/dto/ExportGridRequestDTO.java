package com.moa.api.export.dto;

/*****************************************************************************
 CLASS NAME    : ExportGridRequestDTO
 DESCRIPTION   : Grid CSV Export 요청 DTO
 AUTHOR        : 방대혁
 ******************************************************************************/
/**
 * Grid CSV Export 요청 DTO
 * - Grid 레이어, 컬럼, 필터, 정렬, 파일명, fetchSize 정보를 포함
 */

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportGridRequestDTO {

    /** 요청 사용자 식별자 (서버 인증 기준) */
    private Long memberId;

    /** 프리셋 ID (null 허용, 0 사용 금지 - 프리셋 먼저 저장 후 ID 사용하는 방식 권장) */
    private Integer presetId;

    /** Export 대상 레이어 ("ethernet", "http_page" 등) */
    private String layer;

    /** 내보낼 컬럼 이름 리스트 (출력 순서와 동일) */
    private List<String> columns;

    /** makeFilterModel(...) 결과 JSON 문자열 */
    private String filterModelJson;

    /** 기본 검색 스펙 JSON (선택) */
    private String baseSpecJson;

    /** 정렬 컬럼명 */
    private String sortField;

    /** 정렬 방향 (ASC | DESC) */
    private String sortDirection;

    /** 표기용 파일명 (미지정 시 서버에서 자동 생성) */
    private String fileName;

    /** Fetch Size (null 시 서버 기본값 사용) */
    private Integer fetchSize;
}
