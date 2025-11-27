package com.moa.api.export.repository;

/*****************************************************************************
 INTERFACE NAME : ExportFileRepository
 DESCRIPTION    : Export 파일 메타데이터 JPA Repository
 - 회원별 Export 파일 목록 조회 (페이지네이션)
 - Export ID로 단건 조회
 - 프리셋/버킷/오브젝트키 기반 조회 및 카운트
 AUTHOR         : 방대혁
 ******************************************************************************/

import com.moa.api.export.entity.ExportFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * ExportFile Repository
 */
public interface ExportFileRepository extends JpaRepository<ExportFile, Long> {

    @EntityGraph(attributePaths = {"preset"})
    Page<ExportFile> findByMember_IdAndExportTypeOrderByCreatedAtDesc(
            Long memberId,
            String exportType,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"preset"})
    Optional<ExportFile> findByExportIdAndMember_Id(Long exportId, Long memberId);

    long countByPreset_PresetId(Integer presetId);

    Optional<ExportFile> findByBucketAndObjectKey(String bucket, String objectKey);

    @Query("SELECT COUNT(e) FROM ExportFile e WHERE e.member.id = :memberId")
    long countByMemberId(@Param("memberId") Long memberId);
}
