package com.moa.api.preset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class PresetListResponseDTO {
    private final List<PresetItemDTO> items;
    private final int page;
    private final int size;
    private final int totalPages;
    private final long totalItems;
}