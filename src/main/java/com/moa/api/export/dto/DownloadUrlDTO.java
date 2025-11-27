package com.moa.api.export.dto;

/*****************************************************************************
 CLASS NAME    : DownloadUrlDTO
 DESCRIPTION   : Export 파일 다운로드용 프리사인드 URL 응답 DTO
 AUTHOR        : 방대혁
 ******************************************************************************/

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 다운로드 URL 응답 DTO
 * - httpUrl: 클라이언트가 직접 접근 가능한 HTTP(S) 다운로드 URL
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DownloadUrlDTO {

    /** 프리사인드 다운로드 URL (HTTP/S) */
    private String httpUrl;
}
