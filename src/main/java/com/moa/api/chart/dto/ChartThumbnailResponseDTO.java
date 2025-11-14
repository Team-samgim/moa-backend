package com.moa.api.chart.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChartThumbnailResponseDTO {
    private Long thumbnailId;
    private Long exportId;
    private String bucket;
    private String objectKey;
}
