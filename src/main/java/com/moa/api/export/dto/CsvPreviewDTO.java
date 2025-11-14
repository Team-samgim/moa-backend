package com.moa.api.export.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CsvPreviewDTO {
    private List<String> columns;
    private List<Map<String, String>> rows;
}