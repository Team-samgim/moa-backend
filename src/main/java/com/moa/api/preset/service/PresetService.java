package com.moa.api.preset.service;

import com.moa.api.notification.service.NotificationService;
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
 * -----------------------------------------------------------
 * - Preset 저장 기능을 담당 (SEARCH/Grid, PIVOT, CHART)
 * - PresetRepository(JDBC) + Validator 조합
 * - 저장 성공 시 NotificationService로 알림 트리거 가능(Grid 전용)
 * - SecurityContext 기반으로 인증 사용자 ID 추출
 * AUTHOR        : 방대혁
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PresetService {

    private final PresetRepository presetRepository;
    private final PresetValidator validator;
    private final NotificationService notificationService;


    /**
     * Grid Preset 저장 (PresetType.SEARCH)
     * -----------------------------------------------------------
     * Flow:
     *  1) 요청 DTO 검증
     *  2) 인증 사용자 ID 확인
     *  3) PresetRepository.insert() 실행
     *  4) Preset 저장 성공 알림 전송 (실패해도 전체 트랜잭션 영향 없음)
     *  5) presetId 반환
     *
     * Grid의 preset_type은 항상 "SEARCH"
     * origin은 항상 USER
     */
    @Transactional
    public SaveSearchPresetResponseDTO saveGrid(SaveSearchPresetRequestDTO req) {
        log.info("Saving grid preset: presetName={}", req.getPresetName());

        try {
            // 1) 요청값 검증
            validator.validateSaveRequest(req);

            // 2) 인증 사용자 ID 가져오기
            Long memberId = resolveMemberId();

            // 3) Preset 저장
            Integer presetId = presetRepository.insert(
                    memberId,
                    req.getPresetName().trim(),
                    PresetType.SEARCH.name(),  // Grid는 SEARCH 타입로 통일
                    req.getConfig(),
                    Boolean.TRUE.equals(req.getFavorite()),
                    PresetOrigin.USER          // Grid에서 origin은 항상 USER
            );

            // 4) Preset 저장 알림 보내기 (실패 시 경고만)
            sendPresetSavedNotification(memberId, req.getPresetName().trim(), presetId);

            log.info("Grid preset saved successfully: presetId={}, memberId={}",
                    presetId, memberId);

            return new SaveSearchPresetResponseDTO(presetId);

        } catch (PresetException e) {
            throw e;  // 커스텀 예외는 그대로 전달
        } catch (Exception e) {
            // DB/기타 예외 → PRESET_CREATION_FAILED
            log.error("Unexpected error while saving grid preset", e);
            throw new PresetException(PresetException.ErrorCode.PRESET_CREATION_FAILED, e);
        }
    }


    /**
     * Pivot Preset 저장 (PresetType.PIVOT)
     * -----------------------------------------------------------
     * Flow:
     *  1) DTO 검증
     *  2) 인증 사용자 ID 확인
     *  3) origin 값(요청값이 있으면 그것, 없으면 USER)
     *  4) Preset 저장
     *  5) presetId 반환
     *
     * Pivot은 Grid와 다르게 알림을 전송하지 않음.
     */
    @Transactional
    public SaveSearchPresetResponseDTO savePivot(SaveSearchPresetRequestDTO req) {
        log.info("Saving pivot preset: presetName={}", req.getPresetName());

        try {
            // 1) 검증
            validator.validateSaveRequest(req);

            // 2) 인증 사용자 ID 조회
            Long memberId = resolveMemberId();

            // 3) origin 결정 (null → USER)
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
            throw new PresetException(PresetException.ErrorCode.PRESET_CREATION_FAILED, e);
        }
    }


    /**
     * Chart Preset 저장 (PresetType.CHART)
     * -----------------------------------------------------------
     * Flow:
     *  1) DTO 검증
     *  2) 인증 사용자 ID 조회
     *  3) origin 설정
     *  4) Preset 저장
     */
    @Transactional
    public SaveSearchPresetResponseDTO saveChart(SaveSearchPresetRequestDTO req) {
        log.info("Saving chart preset: presetName={}", req.getPresetName());

        try {
            // 1) 검증
            validator.validateSaveRequest(req);

            // 2) 인증 사용자 ID 조회
            Long memberId = resolveMemberId();

            // 3) Origin 설정 (요청값 없으면 USER)
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
            throw new PresetException(PresetException.ErrorCode.PRESET_CREATION_FAILED, e);
        }
    }


    /**
     * 인증된 사용자 ID 조회
     * -----------------------------------------------------------
     * - SecurityContext에서 Authentication 가져와 principal(Long) 확인
     * - 인증 정보 누락 → UNAUTHORIZED
     * - principal 타입 기대값(Long)과 다르면 예외 발생
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

        // principal이 다른 타입이면 지원 불가
        throw new PresetException(
                PresetException.ErrorCode.UNAUTHORIZED,
                "지원하지 않는 principal 타입: " + principal.getClass().getName()
        );
    }


    /**
     * Preset 저장 완료 알림 전송
     * -----------------------------------------------------------
     * - NotificationService.notifyPresetSaved() 호출
     * - 예외 발생해도 전체 트랜잭션을 깨지 않기 위해 catch 후 warn 로그 출력
     * - Grid preset에서만 사용됨
     */
    private void sendPresetSavedNotification(Long memberId, String presetName, Integer presetId) {
        try {
            notificationService.notifyPresetSaved(memberId, presetName, presetId);
        } catch (Exception e) {
            // 알림 실패해도 preset 저장은 성공시켜야 하므로 예외 삼킴
            log.warn("Failed to send preset saved notification: presetId={}", presetId, e);
        }
    }
}
