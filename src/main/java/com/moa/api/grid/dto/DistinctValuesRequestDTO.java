package com.moa.api.grid.dto;

/*****************************************************************************
 CLASS NAME    : DistinctValuesRequestDTO
 DESCRIPTION   : DISTINCT 값 조회 요청 DTO
 AUTHOR        : 방대혁
 ******************************************************************************/

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DISTINCT 값 조회 요청 DTO
 */
@Data
public class DistinctValuesRequestDTO {

    /**
     * 레이어 (기본 ethernet)
     */
    @NotBlank(message = "레이어는 필수입니다")
    @Pattern(
            regexp = "^(http_page|http_uri|l4_tcp|ethernet|HTTP_PAGE|HTTP_URI|L4_TCP|ETHERNET)$",
            message = "지원하지 않는 레이어입니다"
    )
    private String layer = "ethernet";

    /**
     * DISTINCT 대상 필드
     */
    @NotBlank(message = "필드명은 필수입니다")
    @Size(max = 100, message = "필드명은 100자를 초과할 수 없습니다")
    private String field;

    /**
     * 활성 필터(JSON 문자열)
     */
    @Size(max = 10000, message = "필터 모델은 10000자를 초과할 수 없습니다")
    private String filterModel;

    /**
     * Prefix 검색
     */
    @Size(max = 200, message = "검색어는 200자를 초과할 수 없습니다")
    private String search = "";

    /**
     * 페이징 offset
     */
    @Min(value = 0, message = "offset은 0 이상이어야 합니다")
    private int offset = 0;

    /**
     * 페이징 limit
     */
    @Min(value = 1, message = "limit은 1 이상이어야 합니다")
    @Max(value = 1000, message = "limit은 1000 이하여야 합니다")
    private int limit = 100;

    /**
     * 정렬 컬럼
     */
    @Size(max = 100, message = "정렬 컬럼명은 100자를 초과할 수 없습니다")
    private String orderBy;

    /**
     * 정렬 방향
     */
    @Pattern(
            regexp = "^(ASC|DESC|asc|desc)$",
            message = "정렬 방향은 ASC 또는 DESC여야 합니다"
    )
    private String order = "DESC";

    /**
     * SearchDTO 형태의 baseSpec JSON
     */
    @Size(max = 10000, message = "baseSpec은 10000자를 초과할 수 없습니다")
    private String baseSpec;

    /**
     * 하위호환(무시됨): includeSelf
     */
    private Boolean includeSelf;
}
