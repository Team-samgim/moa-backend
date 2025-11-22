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
 */
@Slf4j
@Component
public class PresetValidator {

    private static final int MAX_PRESET_NAME_LENGTH = 100;
    private static final int MIN_PRESET_NAME_LENGTH = 1;

    /**
     * Preset 저장 요청 검증
     */
    public void validateSaveRequest(SaveSearchPresetRequestDTO req) {
        // Preset 이름 검증
        validatePresetName(req.getPresetName());

        // Config 검증
        validateConfig(req.getConfig());

        // Preset Type 검증 (있는 경우)
        if (req.getPresetType() != null && !req.getPresetType().isBlank()) {
            validatePresetType(req.getPresetType());
        }

        // Origin 검증 (있는 경우)
        if (req.getOrigin() != null) {
            validateOrigin(req.getOrigin());
        }
    }

    /**
     * Preset 이름 검증
     */
    public void validatePresetName(String presetName) {
        if (presetName == null || presetName.isBlank()) {
            throw new PresetException(
                    PresetException.ErrorCode.INVALID_PRESET_NAME,
                    "프리셋 이름은 필수입니다"
            );
        }

        String trimmed = presetName.trim();

        if (trimmed.length() < MIN_PRESET_NAME_LENGTH) {
            throw new PresetException(
                    PresetException.ErrorCode.INVALID_PRESET_NAME,
                    "프리셋 이름이 너무 짧습니다 (최소 " + MIN_PRESET_NAME_LENGTH + "자)"
            );
        }

        if (trimmed.length() > MAX_PRESET_NAME_LENGTH) {
            throw new PresetException(
                    PresetException.ErrorCode.INVALID_PRESET_NAME,
                    "프리셋 이름이 너무 깁니다 (최대 " + MAX_PRESET_NAME_LENGTH + "자)"
            );
        }

        // 특수문자 체크 (선택사항)
        if (containsDangerousCharacters(trimmed)) {
            throw new PresetException(
                    PresetException.ErrorCode.INVALID_PRESET_NAME,
                    "프리셋 이름에 유효하지 않은 문자가 포함되어 있습니다"
            );
        }
    }

    /**
     * Config 검증
     */
    public void validateConfig(Map<String, Object> config) {
        if (config == null || config.isEmpty()) {
            throw new PresetException(
                    PresetException.ErrorCode.INVALID_CONFIG,
                    "설정(config)은 필수입니다"
            );
        }

        // Config 크기 체크 (너무 크면 성능 이슈)
        if (config.size() > 100) {
            log.warn("Config has too many keys: {}", config.size());
        }
    }

    /**
     * Preset Type 검증
     */
    public void validatePresetType(String presetType) {
        if (presetType == null || presetType.isBlank()) {
            return; // null/blank는 허용 (기본값 사용)
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
     */
    public void validateOrigin(PresetOrigin origin) {
        if (origin == null) {
            return; // null은 허용 (기본값 USER)
        }

        // Enum이므로 이미 검증됨, 추가 로직 불필요
    }

    /**
     * Preset ID 검증
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
     * 페이지 파라미터 정규화
     */
    public int normalizePage(int page) {
        return Math.max(0, page);
    }

    /**
     * 사이즈 파라미터 정규화
     */
    public int normalizeSize(int size) {
        return Math.max(1, Math.min(size, 100)); // 최대 100개
    }

    /**
     * 위험한 문자 포함 여부 체크
     */
    private boolean containsDangerousCharacters(String name) {
        // Null 바이트
        if (name.contains("\u0000")) {
            return true;
        }

        // 제어 문자 (탭, 줄바꿈 제외)
        if (name.matches(".*[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F].*")) {
            return true;
        }

        return false;
    }
}