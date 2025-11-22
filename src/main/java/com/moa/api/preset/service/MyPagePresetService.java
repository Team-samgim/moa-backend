package com.moa.api.preset.service;

import com.moa.api.preset.dto.PresetItemDTO;
import com.moa.api.preset.dto.PresetListResponseDTO;
import com.moa.api.preset.exception.PresetException;
import com.moa.api.preset.repository.PresetRepository;
import com.moa.api.preset.validation.PresetValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * MyPage Preset Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MyPagePresetService {

    private final PresetRepository presetRepository;
    private final PresetValidator validator;

    /**
     * 내 Preset 목록 조회
     *
     * @param userId 사용자 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @param type Preset 타입 (SEARCH, PIVOT, CHART)
     * @return Preset 목록
     */
    @Transactional(readOnly = true)
    public PresetListResponseDTO findMyPresets(Long userId, int page, int size, String type) {
        log.debug("Finding user presets: userId={}, page={}, size={}, type={}",
                userId, page, size, type);

        try {
            // 1) 파라미터 정규화
            int normalizedPage = validator.normalizePage(page);
            int normalizedSize = validator.normalizeSize(size);

            // 2) Preset 조회
            List<PresetItemDTO> items = presetRepository.findByMember(
                    userId,
                    type,
                    normalizedPage,
                    normalizedSize
            );

            // 3) 전체 개수 조회
            long totalItems = presetRepository.countByMember(userId, type);

            // 4) 전체 페이지 수 계산
            int totalPages = calculateTotalPages(totalItems, normalizedSize);

            log.debug("Found {} presets for userId={} (page {}/{})",
                    items.size(), userId, normalizedPage, totalPages);

            return PresetListResponseDTO.builder()
                    .items(items)
                    .page(normalizedPage)
                    .size(normalizedSize)
                    .totalPages(totalPages)
                    .totalItems(totalItems)
                    .build();

        } catch (PresetException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while finding presets: userId={}", userId, e);
            throw new PresetException(
                    PresetException.ErrorCode.DATABASE_ERROR,
                    e
            );
        }
    }

    /**
     * 내 Preset 목록 조회 (Origin 필터 포함)
     *
     * @param userId 사용자 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param type Preset 타입
     * @param origin Preset Origin (USER, EXPORT)
     * @return Preset 목록
     */
    @Transactional(readOnly = true)
    public PresetListResponseDTO findMyPresets(
            Long userId,
            int page,
            int size,
            String type,
            String origin
    ) {
        log.debug("Finding user presets with origin: userId={}, page={}, size={}, type={}, origin={}",
                userId, page, size, type, origin);

        try {
            // 1) 파라미터 정규화
            int normalizedPage = validator.normalizePage(page);
            int normalizedSize = validator.normalizeSize(size);

            // 2) Preset 조회
            List<PresetItemDTO> items = presetRepository.findByMember(
                    userId,
                    type,
                    origin,
                    normalizedPage,
                    normalizedSize
            );

            // 3) 전체 개수 조회
            long totalItems = presetRepository.countByMember(userId, type, origin);

            // 4) 전체 페이지 수 계산
            int totalPages = calculateTotalPages(totalItems, normalizedSize);

            log.debug("Found {} presets for userId={} with origin filter (page {}/{})",
                    items.size(), userId, normalizedPage, totalPages);

            return PresetListResponseDTO.builder()
                    .items(items)
                    .page(normalizedPage)
                    .size(normalizedSize)
                    .totalPages(totalPages)
                    .totalItems(totalItems)
                    .build();

        } catch (PresetException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while finding presets with origin: userId={}", userId, e);
            throw new PresetException(
                    PresetException.ErrorCode.DATABASE_ERROR,
                    e
            );
        }
    }

    /**
     * Favorite 상태 변경
     *
     * @param userId 사용자 ID
     * @param presetId Preset ID
     * @param favorite 즐겨찾기 여부
     * @return 수정된 Preset
     */
    @Transactional
    public PresetItemDTO setFavorite(Long userId, Integer presetId, boolean favorite) {
        log.info("Setting favorite: userId={}, presetId={}, favorite={}",
                userId, presetId, favorite);

        try {
            // 1) Preset ID 검증
            validator.validatePresetId(presetId);

            // 2) Favorite 업데이트
            int affected = presetRepository.updateFavorite(userId, presetId, favorite);

            if (affected == 0) {
                throw new PresetException(
                        PresetException.ErrorCode.PRESET_NOT_FOUND,
                        "프리셋을 찾을 수 없거나 권한이 없습니다: presetId=" + presetId
                );
            }

            // 3) 수정된 Preset 조회
            PresetItemDTO preset = presetRepository.findOneForOwner(userId, presetId)
                    .orElseThrow(() -> new PresetException(
                            PresetException.ErrorCode.PRESET_NOT_FOUND,
                            "프리셋 수정 후 조회 실패: presetId=" + presetId
                    ));

            log.info("Favorite updated successfully: presetId={}, favorite={}",
                    presetId, favorite);

            return preset;

        } catch (PresetException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while setting favorite: presetId={}", presetId, e);
            throw new PresetException(
                    PresetException.ErrorCode.PRESET_UPDATE_FAILED,
                    e
            );
        }
    }

    /**
     * Preset 삭제
     *
     * @param userId 사용자 ID
     * @param presetId Preset ID
     */
    @Transactional
    public void delete(Long userId, Integer presetId) {
        log.info("Deleting preset: userId={}, presetId={}", userId, presetId);

        try {
            // 1) Preset ID 검증
            validator.validatePresetId(presetId);

            // 2) Preset 삭제
            int affected = presetRepository.deleteOne(userId, presetId);

            if (affected == 0) {
                throw new PresetException(
                        PresetException.ErrorCode.PRESET_NOT_FOUND,
                        "프리셋을 찾을 수 없거나 권한이 없습니다: presetId=" + presetId
                );
            }

            log.info("Preset deleted successfully: presetId={}, userId={}", presetId, userId);

        } catch (PresetException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while deleting preset: presetId={}", presetId, e);
            throw new PresetException(
                    PresetException.ErrorCode.PRESET_DELETE_FAILED,
                    e
            );
        }
    }

    /**
     * 전체 페이지 수 계산
     */
    private int calculateTotalPages(long totalItems, int size) {
        if (totalItems == 0 || size <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalItems / (double) size);
    }
}