package com.moa.api.export.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExportFileItemDTO {
    private Long fileId;          // = exportId
    private String fileName;
    private String exportType;    // GRID/PIVOT/CHART
    private String layer;         // preset.config.layer
    private LocalDateTime createdAt;
    private Integer presetId;
    private JsonNode config;      // (옵션) 프리셋 config 원본
}