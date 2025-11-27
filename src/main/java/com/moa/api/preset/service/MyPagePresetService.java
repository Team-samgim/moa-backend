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
 * -----------------------------------------------------------
 * - Preset 목록 조회 / 삭제 / 즐겨찾기 변경 담당
 * - Repository(JDBC)와 Validator를 조합하여 비즈니스 흐름 구성
 * - Exception은 PresetException으로 통일되어 ControllerAdvice에서 처리됨
 * AUTHOR        : 방대혁
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MyPagePresetService {

    private final PresetRepository presetRepository;
    private final PresetValidator validator;


    /**
     * 내 Preset 목록 조회
     * -----------------------------------------------------------
     * 1) page, size 정규화
     * 2) repository 조회
     * 3) 총 개수 조회하여 totalPages 계산
     * 4) PresetListResponseDTO로 래핑 후 반환
     *
     * @param userId 사용자 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @param type PRESET 타입 (SEARCH, PIVOT, CHART)
     */
    @Transactional(readOnly = true)
    public PresetListResponseDTO findMyPresets(Long userId, int page, int size, String type) {
        log.debug("Finding user presets: userId={}, page={}, size={}, type={}",
                userId, page, size, type);

        try {
            // 1) page/size 범위 정규화
            int normalizedPage = validator.normalizePage(page);
            int normalizedSize = validator.normalizeSize(size);

            // 2) Preset 리스트 조회
            List<PresetItemDTO> items = presetRepository.findByMember(
                    userId,
                    type,
                    normalizedPage,
                    normalizedSize
            );

            // 3) 전체 Preset 개수 조회
            long totalItems = presetRepository.countByMember(userId, type);

            // 4) 총 페이지 수 계산
            int totalPages = calculateTotalPages(totalItems, normalizedSize);

            log.debug("Found {} presets for userId={} (page {}/{})",
                    items.size(), userId, normalizedPage, totalPages);

            // 5) DTO 래핑
            return PresetListResponseDTO.builder()
                    .items(items)
                    .page(normalizedPage)
                    .size(normalizedSize)
                    .totalPages(totalPages)
                    .totalItems(totalItems)
                    .build();

        } catch (PresetException e) {
            throw e; // 그대로 전파 → ControllerAdvice에서 처리
        } catch (Exception e) {
            log.error("Unexpected error while finding presets: userId={}", userId, e);
            throw new PresetException(PresetException.ErrorCode.DATABASE_ERROR, e);
        }
    }


    /**
     * 내 Preset 목록 조회 (origin 필터 포함)
     * -----------------------------------------------------------
     * origin(USER / EXPORT)을 추가로 필터링하는 버전
     * 나머지 로직은 동일
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

            // 2) Preset 리스트 조회(origin 포함)
            List<PresetItemDTO> items = presetRepository.findByMember(
                    userId,
                    type,
                    origin,
                    normalizedPage,
                    normalizedSize
            );

            // 3) 전체 개수 조회(origin 포함)
            long totalItems = presetRepository.countByMember(userId, type, origin);

            // 4) 페이지 계산
            int totalPages = calculateTotalPages(totalItems, normalizedSize);

            log.debug("Found {} presets for userId={} with origin filter (page {}/{})",
                    items.size(), userId, normalizedPage, totalPages);

            // 5) DTO 반환
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
            throw new PresetException(PresetException.ErrorCode.DATABASE_ERROR, e);
        }
    }


    /**
     * 즐겨찾기(Favorite) 상태 변경
     * -----------------------------------------------------------
     * 1) presetId 유효성 검사
     * 2) repository.updateFavorite() 실행
     * 3) 영향받은 row가 없으면 '권한 없음 or 존재하지 않음'
     * 4) 업데이트된 preset을 재조회하여 반환
     */
    @Transactional
    public PresetItemDTO setFavorite(Long userId, Integer presetId, boolean favorite) {
        log.info("Setting favorite: userId={}, presetId={}, favorite={}",
                userId, presetId, favorite);

        try {
            // 1) ID 검증
            validator.validatePresetId(presetId);

            // 2) 업데이트
            int affected = presetRepository.updateFavorite(userId, presetId, favorite);

            // 3) 업데이트된 row 없음 → 사용자 소유 아님 or 존재하지 않음
            if (affected == 0) {
                throw new PresetException(
                        PresetException.ErrorCode.PRESET_NOT_FOUND,
                        "프리셋을 찾을 수 없거나 권한이 없습니다: presetId=" + presetId
                );
            }

            // 4) 다시 Preset 조회해서 최신 상태 반환
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
            throw new PresetException(PresetException.ErrorCode.PRESET_UPDATE_FAILED, e);
        }
    }


    /**
     * Preset 삭제
     * -----------------------------------------------------------
     * 1) presetId 유효성 검사
     * 2) repository.deleteOne() 실행
     * 3) 영향받은 row = 0 → 존재하지 않음 또는 권한 없음
     */
    @Transactional
    public void delete(Long userId, Integer presetId) {
        log.info("Deleting preset: userId={}, presetId={}", userId, presetId);

        try {
            // 1) ID 검증
            validator.validatePresetId(presetId);

            // 2) 삭제 실행
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
            throw new PresetException(PresetException.ErrorCode.PRESET_DELETE_FAILED, e);
        }
    }


    /**
     * 전체 페이지 수 계산
     * -----------------------------------------------------------
     * totalItems: 전체 개수
     * size: 페이지당 개수
     * size <= 0 예외 케이스 방어
     */
    private int calculateTotalPages(long totalItems, int size) {
        if (totalItems == 0 || size <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalItems / (double) size);
    }
}
