package com.moa.api.preset.service;

import com.moa.api.preset.dto.SaveSearchPresetRequestDTO;
import com.moa.api.preset.dto.SaveSearchPresetResponseDTO;
import com.moa.api.preset.entity.PresetOrigin;
import com.moa.api.preset.entity.PresetType;
import com.moa.api.preset.repository.PresetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PresetService {
    private final PresetRepository repo;

    public SaveSearchPresetResponseDTO saveGrid(SaveSearchPresetRequestDTO req) throws Exception {
        if (req.getPresetName() == null || req.getPresetName().isBlank())
            throw new IllegalArgumentException("presetName is required");
        if (req.getConfig() == null)
            throw new IllegalArgumentException("config is required");

        Integer id = repo.insert(
                resolveMemberId(),
                req.getPresetName(),
                "SEARCH",
                req.getConfig(),
                Boolean.TRUE.equals(req.getFavorite()),
                PresetOrigin.USER                 // ★ 항상 USER로 저장
        );
        return new SaveSearchPresetResponseDTO(id);
    }

    public SaveSearchPresetResponseDTO savePivot(SaveSearchPresetRequestDTO req) throws Exception {
        if (req.getPresetName() == null || req.getPresetName().isBlank()) {
            throw new IllegalArgumentException("presetName is required");
        }
        if (req.getConfig() == null) {
            throw new IllegalArgumentException("config is required");
        }

        Integer id = repo.insert(
                resolveMemberId(),
                req.getPresetName(),
                PresetType.PIVOT.name(),              // PIVOT 타입
                req.getConfig(),
                Boolean.TRUE.equals(req.getFavorite()),
                PresetOrigin.USER                     // USER 직접 저장
        );
        return new SaveSearchPresetResponseDTO(id);
    }

    private Long resolveMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            throw new IllegalStateException("인증 정보가 없습니다.");
        Object principal = auth.getPrincipal();
        if (principal instanceof Long l) return l; // JwtAuthenticationFilter가 m.getId()를 principal로 넣음
        throw new IllegalStateException("지원하지 않는 principal 타입: " + principal);
    }
}
