package com.moa.api.preset.repository;

import com.moa.api.preset.entity.Preset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PresetRepository extends JpaRepository<Preset, Integer> { }