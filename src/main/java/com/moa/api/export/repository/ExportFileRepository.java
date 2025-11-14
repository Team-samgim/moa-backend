package com.moa.api.export.repository;

import com.moa.api.export.entity.ExportFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExportFileRepository extends JpaRepository<ExportFile, Long> {

    @EntityGraph(attributePaths = {"preset"})
    Page<ExportFile> findByMember_IdAndExportTypeOrderByCreatedAtDesc(Long memberId, String exportType, Pageable pageable);

    @EntityGraph(attributePaths = {"preset"})
    Optional<ExportFile> findByExportIdAndMember_Id(Long exportId, Long memberId);

    long countByPreset_PresetId(Integer presetId);

    Optional<ExportFile> findByBucketAndObjectKey(String bucket, String objectKey);
}