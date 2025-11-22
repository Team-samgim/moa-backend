package com.moa.api.export.dto;

import com.moa.api.pivot.dto.request.PivotQueryRequestDTO;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportPivotRequestDTO {
    private Integer presetId;
    private PivotQueryRequestDTO pivot;
    private String fileName;
}
