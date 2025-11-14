package com.moa.api.preset.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class SaveSearchPresetRequestDTO {
    private String presetName;          // required
    private String presetType;          // e.g. "SEARCH"
    private Map<String, Object> config; // required (현재 검색 상태)
    private Boolean favorite;           // optional
}
