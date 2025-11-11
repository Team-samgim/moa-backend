package com.moa.api.preset.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PresetItemDTO {
    private Integer presetId;
    private String presetName;
    private String presetType;
    private JsonNode config;     // jsonb → JsonNode
    private boolean favorite;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}