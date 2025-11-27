package com.moa.api.export.dto;

/*****************************************************************************
 CLASS NAME    : ExportCreateResponseDTO
 DESCRIPTION   : Export 생성 요청 처리 후 S3 업로드/메타 정보 응답 DTO
 AUTHOR        : 방대혁
 ******************************************************************************/

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Export 생성 결과 DTO
 * - exportId : Export 메타 데이터 PK
 * - bucket   : 업로드된 S3 버킷명
 * - objectKey: 업로드된 S3 오브젝트 키
 * - s3Url    : s3://bucket/key 형태의 내부 식별 URL
 * - httpUrl  : 프리사인드/다운로드용 HTTP URL
 * - status   : 처리 상태 (예: SUCCESS, FAILED 등)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportCreateResponseDTO {

    /** Export 메타 데이터 PK */
    private Long exportId;

    /** S3 버킷명 */
    private String bucket;

    /** S3 오브젝트 키 */
    private String objectKey;

    /** s3://bucket/key 형태의 URL */
    private String s3Url;

    /** HTTP(S) 다운로드 URL */
    private String httpUrl;

    /** 처리 상태 (예: SUCCESS, FAILED 등) */
    private String status;
}
