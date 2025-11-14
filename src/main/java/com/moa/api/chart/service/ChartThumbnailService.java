package com.moa.api.chart.service;

import com.moa.api.chart.dto.CreateChartThumbnailRequestDTO;
import com.moa.api.chart.entity.ChartThumbnail;
import com.moa.api.chart.repository.ChartThumbnailRepository;
import com.moa.api.export.entity.ExportFile;
import com.moa.api.export.repository.ExportFileRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChartThumbnailService {

    private final ExportFileRepository exportFileRepository;
    private final ChartThumbnailRepository chartThumbnailRepository;

    @Transactional
    public ChartThumbnail create(CreateChartThumbnailRequestDTO req) {
        if (req.getBucket() == null || req.getObjectKey() == null ||
                req.getThumbnailBucket() == null || req.getThumbnailKey() == null) {
            throw new IllegalArgumentException("bucket/objectKey/thumbnailBucket/thumbnailKey are required");
        }

        ExportFile exportFile = exportFileRepository
                .findByBucketAndObjectKey(req.getBucket(), req.getObjectKey())
                .orElseThrow(() -> new IllegalArgumentException("원본 export 파일을 찾을 수 없습니다."));

        ChartThumbnail thumb = ChartThumbnail.builder()
                .exportFile(exportFile)
                .bucket(req.getThumbnailBucket())
                .objectKey(req.getThumbnailKey())
                .createdAt(LocalDateTime.now())
                .build();

        return chartThumbnailRepository.save(thumb);
    }
}
