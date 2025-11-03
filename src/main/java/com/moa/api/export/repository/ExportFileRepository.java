package com.moa.api.export.repository;

import com.moa.api.export.entity.ExportFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExportFileRepository extends JpaRepository<ExportFile, Long> { }