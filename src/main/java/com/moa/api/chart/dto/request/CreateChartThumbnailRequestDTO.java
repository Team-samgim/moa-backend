// 작성자: 최이서
package com.moa.api.chart.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateChartThumbnailRequestDTO {

    // 원본(ChartExportService 가 올린 PNG)
    private String bucket;       // 예: moa-export
    private String objectKey;    // 예: chart/chart_2025....

    // 썸네일(람다가 만든 PNG)
    private String thumbnailBucket; // 보통 같은 버킷이면 bucket 재사용
    private String thumbnailKey;    // 예: thumbnails/chart/chart_2025....
}
