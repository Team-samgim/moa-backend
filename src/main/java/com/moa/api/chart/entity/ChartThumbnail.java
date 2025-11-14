package com.moa.api.chart.entity;

import com.moa.api.export.entity.ExportFile;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chart_thumbnails")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChartThumbnail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "thumbnail_id")
    private Long thumbnailId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "export_id", nullable = false)
    private ExportFile exportFile;

    @Column(name = "bucket", length = 100, nullable = false)
    private String bucket;

    @Column(name = "object_key", length = 600, nullable = false)
    private String objectKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
