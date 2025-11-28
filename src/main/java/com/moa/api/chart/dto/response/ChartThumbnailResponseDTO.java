// 작성자: 최이서
package com.moa.api.chart.dto.response;

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
