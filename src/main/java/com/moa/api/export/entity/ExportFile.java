package com.moa.api.export.entity;

/*****************************************************************************
 CLASS NAME    : ExportFile
 DESCRIPTION   : Grid/Pivot/Chart Export 결과 파일 메타데이터 엔티티
 (S3 버킷/오브젝트 키 및 상태 관리)
 AUTHOR        : 방대혁
 ******************************************************************************/

import com.moa.api.member.entity.Member;
import com.moa.api.preset.entity.Preset;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "export_files")
public class ExportFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "export_id")
    private Long exportId;

    /** FK: members.id */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** FK: presets.preset_id */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "preset_id", nullable = false)
    private Preset preset;

    @Column(name = "export_type", length = 10, nullable = false)
    private String exportType;        // GRID | PIVOT | CHART

    @Column(name = "file_format", length = 10, nullable = false)
    private String fileFormat;        // CSV/PNG/XLSX

    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;

    @Column(name = "bucket", length = 100, nullable = false)
    private String bucket;

    @Column(name = "object_key", length = 600, nullable = false)
    private String objectKey;         // s3 key

    @Column(name = "export_status", length = 20, nullable = false)
    private String exportStatus;      // PENDING/RUNNING/SUCCESS/FAILED

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
