package com.moa.api.preset.service;

import com.moa.api.preset.dto.PresetItemDTO;
import com.moa.api.preset.dto.PresetListResponseDTO;
import com.moa.api.preset.repository.PresetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPagePresetService {

    private final PresetRepository repo;

    public PresetListResponseDTO findMyPresets(Long userId, int page, int size, String type) {
        int p = Math.max(0, page);
        int s = Math.max(1, size);
        List<PresetItemDTO> items = repo.findByMember(userId, type, p, s);
        long total = repo.countByMember(userId, type);
        int totalPages = (int) Math.ceil((double) total / (double) s);
        return PresetListResponseDTO.builder()
                .items(items)
                .page(p)
                .size(s)
                .totalPages(totalPages)
                .totalItems(total)
                .build();
    }

    @Transactional
    public PresetItemDTO setFavorite(Long userId, Integer presetId, boolean favorite) {
        int n = repo.updateFavorite(userId, presetId, favorite);
        if (n == 0) throw new IllegalArgumentException("프리셋을 찾을 수 없습니다.");
        return repo.findOneForOwner(userId, presetId).orElseThrow();
    }

    @Transactional
    public void delete(Long userId, Integer presetId) {
        int n = repo.deleteOne(userId, presetId);
        if (n == 0) throw new IllegalArgumentException("프리셋을 찾을 수 없습니다.");
    }
}