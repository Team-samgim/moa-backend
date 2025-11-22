package com.moa.api.preset.service;

import com.moa.api.preset.dto.SaveSearchPresetRequestDTO;
import com.moa.api.preset.dto.SaveSearchPresetResponseDTO;
import com.moa.api.preset.entity.PresetOrigin;
import com.moa.api.preset.entity.PresetType;
import com.moa.api.preset.exception.PresetException;
import com.moa.api.preset.repository.PresetRepository;
import com.moa.api.preset.validation.PresetValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Preset Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PresetService {

    private final PresetRepository presetRepository;
    private final PresetValidator validator;

    /**
     * Grid Preset 저장
     *
     * @param req Preset 저장 요청
     * @return 생성된 Preset ID
     */
    @Transactional
    public SaveSearchPresetResponseDTO saveGrid(SaveSearchPresetRequestDTO req) {
        log.info("Saving grid preset: presetName={}", req.getPresetName());

        try {
            // 1) 검증
            validator.validateSaveRequest(req);

            // 2) 인증된 사용자 ID 조회
            Long memberId = resolveMemberId();

            // 3) Preset 저장
            Integer presetId = presetRepository.insert(
                    memberId,
                    req.getPresetName().trim(),
                    PresetType.SEARCH.name(),  // Grid는 SEARCH 타입
                    req.getConfig(),
                    Boolean.TRUE.equals(req.getFavorite()),
                    PresetOrigin.USER  // Grid Preset은 항상 USER
            );

            log.info("Grid preset saved successfully: presetId={}, memberId={}",
                    presetId, memberId);

            return new SaveSearchPresetResponseDTO(presetId);

        } catch (PresetException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while saving grid preset", e);
            throw new PresetException(
                    PresetException.ErrorCode.PRESET_CREATION_FAILED,
                    e
            );
        }
    }

    /**
     * Pivot Preset 저장
     *
     * @param req Preset 저장 요청
     * @return 생성된 Preset ID
     */
    @Transactional
    public SaveSearchPresetResponseDTO savePivot(SaveSearchPresetRequestDTO req) {
        log.info("Saving pivot preset: presetName={}", req.getPresetName());

        try {
            // 1) 검증
            validator.validateSaveRequest(req);

            // 2) 인증된 사용자 ID 조회
            Long memberId = resolveMemberId();

            // 3) Origin 결정 (요청에 있으면 사용, 없으면 USER)
            PresetOrigin origin = req.getOrigin() != null
                    ? req.getOrigin()
                    : PresetOrigin.USER;

            // 4) Preset 저장
            Integer presetId = presetRepository.insert(
                    memberId,
                    req.getPresetName().trim(),
                    PresetType.PIVOT.name(),
                    req.getConfig(),
                    Boolean.TRUE.equals(req.getFavorite()),
                    origin
            );

            log.info("Pivot preset saved successfully: presetId={}, memberId={}, origin={}",
                    presetId, memberId, origin);

            return new SaveSearchPresetResponseDTO(presetId);

        } catch (PresetException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while saving pivot preset", e);
            throw new PresetException(
                    PresetException.ErrorCode.PRESET_CREATION_FAILED,
                    e
            );
        }
    }

    /**
     * Chart Preset 저장
     *
     * @param req Preset 저장 요청
     * @return 생성된 Preset ID
     */
    @Transactional
    public SaveSearchPresetResponseDTO saveChart(SaveSearchPresetRequestDTO req) {
        log.info("Saving chart preset: presetName={}", req.getPresetName());

        try {
            // 1) 검증
            validator.validateSaveRequest(req);

            // 2) 인증된 사용자 ID 조회
            Long memberId = resolveMemberId();

            // 3) Origin 결정
            PresetOrigin origin = req.getOrigin() != null
                    ? req.getOrigin()
                    : PresetOrigin.USER;

            // 4) Preset 저장
            Integer presetId = presetRepository.insert(
                    memberId,
                    req.getPresetName().trim(),
                    PresetType.CHART.name(),
                    req.getConfig(),
                    Boolean.TRUE.equals(req.getFavorite()),
                    origin
            );

            log.info("Chart preset saved successfully: presetId={}, memberId={}, origin={}",
                    presetId, memberId, origin);

            return new SaveSearchPresetResponseDTO(presetId);

        } catch (PresetException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while saving chart preset", e);
            throw new PresetException(
                    PresetException.ErrorCode.PRESET_CREATION_FAILED,
                    e
            );
        }
    }

    /**
     * 인증된 사용자 ID 조회
     */
    private Long resolveMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new PresetException(PresetException.ErrorCode.UNAUTHORIZED);
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof Long memberId) {
            return memberId;
        }

        throw new PresetException(
                PresetException.ErrorCode.UNAUTHORIZED,
                "지원하지 않는 principal 타입: " + principal.getClass().getName()
        );
    }
}