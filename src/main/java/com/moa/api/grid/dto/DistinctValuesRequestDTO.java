package com.moa.api.grid.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class DistinctValuesRequestDTO {

    /** 레이어 (기본 ethernet) */
    @NotBlank
    private String layer = "ethernet";

    /** 필수: DISTINCT 대상 필드 */
    @NotBlank
    private String field;

    /** 활성 필터(JSON 문자열) */
    private String filterModel;

    /** prefix 검색 */
    private String search = "";

    /** 페이징 */
    @Min(0)
    private int offset = 0;

    @Min(1)
    @Max(500)
    private int limit = 100;

    /** 정렬 */
    private String orderBy; // null 허용: 서버에서 보정

    @Pattern(regexp = "(?i)ASC|DESC")
    private String order = "DESC";

    /** SearchDTO 형태의 baseSpec JSON */
    private String baseSpec;

    /** 하위호환(무시됨): includeSelf - 서비스에서 자동 판정 */
    private Boolean includeSelf;
}
