package com.moa.api.preset.validation;

import com.moa.api.preset.dto.SaveSearchPresetRequestDTO;
import com.moa.api.preset.entity.PresetOrigin;
import com.moa.api.preset.entity.PresetType;
import com.moa.api.preset.exception.PresetException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

/**
 * Preset 입력값 검증기
 * ---------------------------------------------------------
 * - Preset 저장 요청에서 넘어오는 값들을 검증한다.
 * - 잘못된 입력은 PresetException을 통해 명확한 코드로 반환한다.
 * - Client가 보낸 값의 유효성/위험성 검증 책임을 담당한다.
 * AUTHOR        : 방대혁
 */
@Slf4j
@Component
public class PresetValidator {

    // Preset 이름 길이 제한
    private static final int MAX_PRESET_NAME_LENGTH = 100;
    private static final int MIN_PRESET_NAME_LENGTH = 1;

    /**
     * Preset 저장 요청 전체 검증
     * ---------------------------------------------------------
     * - presetName: 필수, 길이, 문자 검증
     * - config: 필수, 크기 제한
     * - presetType: 존재 시 Enum 체크
     * - origin: 존재 시 Enum 체크 (실제 Enum이라 별도 로직은 없음)
     *
     * @param req 요청 DTO
     */
    public void validateSaveRequest(SaveSearchPresetRequestDTO req) {

        // 1) Preset 이름 검증
        validatePresetName(req.getPresetName());

        // 2) config(Map) 검증
        validateConfig(req.getConfig());

        // 3) presetType 검증 (Search/Pivot/Chart)
        if (req.getPresetType() != null && !req.getPresetType().isBlank()) {
            validatePresetType(req.getPresetType());
        }

        // 4) origin 검증 (USER, EXPORT)
        if (req.getOrigin() != null) {
            validateOrigin(req.getOrigin());
        }
    }


    /**
     * Preset 이름 검증
     * ---------------------------------------------------------
     * - null, 공백 불가
     * - 길이 제한 (1 ~ 100자)
     * - 위험 문자 포함 여부 검사
     */
    public void validatePresetName(String presetName) {
        if (presetName == null || presetName.isBlank()) {
            throw new PresetException(
                    PresetException.ErrorCode.INVALID_PRESET_NAME,
                    "프리셋 이름은 필수입니다"
            );
        }

        String trimmed = presetName.trim();

        // 최소 길이
        if (trimmed.length() < MIN_PRESET_NAME_LENGTH) {
            throw new PresetException(
                    PresetException.ErrorCode.INVALID_PRESET_NAME,
                    "프리셋 이름이 너무 짧습니다 (최소 " + MIN_PRESET_NAME_LENGTH + "자)"
            );
        }

        // 최대 길이
        if (trimmed.length() > MAX_PRESET_NAME_LENGTH) {
            throw new PresetException(
                    PresetException.ErrorCode.INVALID_PRESET_NAME,
                    "프리셋 이름이 너무 깁니다 (최대 " + MAX_PRESET_NAME_LENGTH + "자)"
            );
        }

        // 위험 문자 포함 여부 검사
        if (containsDangerousCharacters(trimmed)) {
            throw new PresetException(
                    PresetException.ErrorCode.INVALID_PRESET_NAME,
                    "프리셋 이름에 유효하지 않은 문자가 포함되어 있습니다"
            );
        }
    }


    /**
     * Config(Map) 검증
     * ---------------------------------------------------------
     * - 필수 값이어야 함
     * - 구성 요소 너무 많은 경우 경고 로그 (성능 고려)
     */
    public void validateConfig(Map<String, Object> config) {
        if (config == null || config.isEmpty()) {
            throw new PresetException(
                    PresetException.ErrorCode.INVALID_CONFIG,
                    "설정(config)은 필수입니다"
            );
        }

        // 키가 너무 많으면 성능 고려해 경고
        if (config.size() > 100) {
            log.warn("Config has too many keys: {}", config.size());
        }
    }


    /**
     * Preset Type 검증
     * ---------------------------------------------------------
     * - SEARCH / PIVOT / CHART 의 Enum 값이어야 함
     * - null/blank는 허용 (상위 서비스에서 기본값 세팅)
     */
    public void validatePresetType(String presetType) {
        if (presetType == null || presetType.isBlank()) {
            return; // null은 상위 서비스에서 기본 PresetType 사용
        }

        String normalized = presetType.trim().toUpperCase();

        boolean isValid = Arrays.stream(PresetType.values())
                .anyMatch(type -> type.name().equals(normalized));

        if (!isValid) {
            throw new PresetException(
                    PresetException.ErrorCode.INVALID_PRESET_TYPE,
                    "유효하지 않은 프리셋 타입: " + presetType +
                            " (허용: " + Arrays.toString(PresetType.values()) + ")"
            );
        }
    }


    /**
     * Origin 검증
     * ---------------------------------------------------------
     * - Enum 기반이라 null이면 USER 등 기본값 사용
     * - 별도 로직 필요 없음
     */
    public void validateOrigin(PresetOrigin origin) {
        if (origin == null) {
            return; // null → 기본값(USER) 허용
        }
        // Enum Origin 자체가 유효성 체크를 대신함
    }


    /**
     * Preset ID 검증
     * ---------------------------------------------------------
     * - null 또는 0 이하의 ID는 잘못된 요청
     */
    public void validatePresetId(Integer presetId) {
        if (presetId == null || presetId <= 0) {
            throw new PresetException(
                    PresetException.ErrorCode.INVALID_REQUEST,
                    "유효하지 않은 프리셋 ID: " + presetId
            );
        }
    }


    /**
     * 페이지 번호 정규화
     * ---------------------------------------------------------
     * - 음수 → 0으로 보정
     */
    public int normalizePage(int page) {
        return Math.max(0, page);
    }

    /**
     * 페이지 크기 정규화
     * ---------------------------------------------------------
     * - 최소 1, 최대 100 (프론트 부하 방지)
     */
    public int normalizeSize(int size) {
        return Math.max(1, Math.min(size, 100));
    }


    /**
     * 위험한 문자 포함 여부 체크
     * ---------------------------------------------------------
     * - Null byte('\0'), 제어 문자 등 위험한 문자 탐지
     * - SQL 인젝션 X, XSS X → 스토리지 보호 목적
     */
    private boolean containsDangerousCharacters(String name) {
        // Null 바이트 포함 여부
        if (name.contains("\u0000")) {
            return true;
        }

        // 제어 문자 (tab, newline 제외)
        if (name.matches(".*[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F].*")) {
            return true;
        }

        return false;
    }
}
