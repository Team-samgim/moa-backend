package com.moa.api.export.repository;

import com.moa.api.export.entity.ExportFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ExportFileRepository extends JpaRepository<ExportFile, Long> {

    @EntityGraph(attributePaths = {"preset"})
    Page<ExportFile> findByMember_IdAndExportTypeOrderByCreatedAtDesc(Long memberId, String exportType, Pageable pageable);

    @EntityGraph(attributePaths = {"preset"})
    Optional<ExportFile> findByExportIdAndMember_Id(Long exportId, Long memberId);

    long countByPreset_PresetId(Integer presetId);

    Optional<ExportFile> findByBucketAndObjectKey(String bucket, String objectKey);

    @Query("SELECT COUNT(e) FROM ExportFile e WHERE e.member.id = :memberId")
    long countByMemberId(@Param("memberId") Long memberId);
}