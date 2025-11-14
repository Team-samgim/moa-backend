package com.moa.api.export.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportChartRequestDTO {

    // 파일 이름 (확장자 제외, 옵션)
    private String fileName;

    // 프리셋 config: { search?: {...}, pivot: { mode, config }, chart: {...} }
    private Map<String, Object> config;

    // dataURL 에서 `data:image/png;base64,` 뗀 순수 base64 문자열
    private String imageBase64;
}
