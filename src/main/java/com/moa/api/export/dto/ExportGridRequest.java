package com.moa.api.export.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportGridRequest {
    private Long memberId;             // 서버 인증
    private Integer presetId;          // 프리셋과 연결(없으면 0 대신 null 금지 -> 프리셋 먼저 저장하고 id 사용 추천)

    private String layer;              // "ethernet" | "http_page" ...
    private List<String> columns;      // 내보낼 컬럼 순서
    private String filterModelJson;    // makeFilterModel(...) 결과 JSON 문자열
    private String baseSpecJson;
    private String sortField;          // 정렬 컬럼
    private String sortDirection;      // ASC|DESC

    private String fileName;           // 표기용 파일명(없으면 서버에서 생성)

    private Integer fetchSize;
}