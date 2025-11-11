package com.moa.api.export.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportFileListResponseDTO {
    private List<ExportFileItemDTO> items;
    private int page;
    private int size;
    private int totalPages;
    private long totalItems;
}