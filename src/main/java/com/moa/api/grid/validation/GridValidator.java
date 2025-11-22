package com.moa.api.grid.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moa.api.grid.exception.GridException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Grid API 입력값 검증기
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GridValidator {

    private final ObjectMapper objectMapper;

    /**
     * FilterModel JSON 유효성 검증
     */
    public void validateFilterModel(String filterModel) {
        if (filterModel == null || filterModel.isBlank()) {
            return; // null/blank는 허용
        }

        try {
            JsonNode root = objectMapper.readTree(filterModel);

            if (!root.isObject()) {
                throw new GridException(
                        GridException.ErrorCode.INVALID_FILTER_MODEL,
                        "FilterModel은 JSON 객체여야 합니다"
                );
            }

            // 각 필드별 검증
            root.fields().forEachRemaining(entry -> {
                String field = entry.getKey();
                JsonNode value = entry.getValue();

                validateFilterField(field, value);
            });

        } catch (Exception e) {
            if (e instanceof GridException) {
                throw (GridException) e;
            }
            throw new GridException(
                    GridException.ErrorCode.INVALID_FILTER_MODEL,
                    e
            );
        }
    }

    /**
     * 필드별 필터 검증
     */
    private void validateFilterField(String field, JsonNode value) {
        if (!value.isObject()) {
            throw new GridException(
                    GridException.ErrorCode.INVALID_FILTER_MODEL,
                    "필드 '" + field + "'의 값은 객체여야 합니다"
            );
        }

        String mode = value.path("mode").asText("");

        if (!"checkbox".equals(mode) && !"condition".equals(mode)) {
            throw new GridException(
                    GridException.ErrorCode.INVALID_FILTER_MODEL,
                    "필드 '" + field + "'의 mode는 'checkbox' 또는 'condition'이어야 합니다"
            );
        }
    }

    /**
     * BaseSpec JSON 유효성 검증
     */
    public void validateBaseSpec(String baseSpec) {
        if (baseSpec == null || baseSpec.isBlank()) {
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(baseSpec);

            if (!root.isObject()) {
                throw new GridException(
                        GridException.ErrorCode.INVALID_BASE_SPEC,
                        "BaseSpec은 JSON 객체여야 합니다"
                );
            }

            // time 필드 검증
            JsonNode time = root.path("time");
            if (time.isObject()) {
                validateTimeSpec(time);
            }

        } catch (Exception e) {
            if (e instanceof GridException) {
                throw (GridException) e;
            }
            throw new GridException(
                    GridException.ErrorCode.INVALID_BASE_SPEC,
                    e
            );
        }
    }

    /**
     * Time spec 검증
     */
    private void validateTimeSpec(JsonNode time) {
        if (!time.has("field") || !time.has("fromEpoch") || !time.has("toEpoch")) {
            throw new GridException(
                    GridException.ErrorCode.INVALID_BASE_SPEC,
                    "time 객체는 field, fromEpoch, toEpoch를 포함해야 합니다"
            );
        }

        long from = time.get("fromEpoch").asLong();
        long to = time.get("toEpoch").asLong();

        if (from > to) {
            throw new GridException(
                    GridException.ErrorCode.INVALID_BASE_SPEC,
                    "fromEpoch는 toEpoch보다 작거나 같아야 합니다"
            );
        }
    }

    /**
     * Layer 유효성 검증
     */
    public void validateLayer(String layer) {
        if (layer == null || layer.isBlank()) {
            return;
        }

        String normalized = layer.toUpperCase();
        if (!isValidLayer(normalized)) {
            throw new GridException(
                    GridException.ErrorCode.LAYER_NOT_FOUND,
                    "지원하지 않는 레이어: " + layer
            );
        }
    }

    private boolean isValidLayer(String layer) {
        return "HTTP_PAGE".equals(layer) ||
                "HTTP_URI".equals(layer) ||
                "TCP".equals(layer) ||
                "ETHERNET".equals(layer);
    }
}