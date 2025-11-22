package com.moa.api.grid.util;

import com.moa.api.grid.config.GridProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 레이어명 → 실제 테이블명 변환
 */
@Component
@RequiredArgsConstructor
public class LayerTableResolver {

    private final GridProperties properties;

    /**
     * 레이어명으로 데이터 테이블 조회
     */
    public String resolveDataTable(String layer) {
        if (layer == null || layer.isBlank()) {
            return properties.getLayerTableMapping()
                    .getOrDefault("HTTP_PAGE", "public.http_page_sample");
        }

        String key = layer.trim().toUpperCase(Locale.ROOT);
        return properties.getLayerTableMapping()
                .getOrDefault(key, "public.http_page_sample");
    }

    /**
     * 레이어명으로 필드 메타 테이블 조회
     */
    public String resolveFieldMetaTable(String layer) {
        if (layer == null || layer.isBlank()) {
            return "ethernet_fields";
        }

        String key = layer.trim().toLowerCase(Locale.ROOT);
        return properties.getFieldMetaTableMapping()
                .getOrDefault(key, "ethernet_fields");
    }

    /**
     * 전체 테이블명(FQN)에서 스키마와 테이블명 분리
     *
     * @param fqn public.http_page_sample
     * @return [schema, table] 배열
     */
    public String[] splitSchemaAndTable(String fqn) {
        if (fqn == null || fqn.isBlank()) {
            return new String[]{"public", ""};
        }

        String[] parts = fqn.split("\\.", 2);
        if (parts.length == 1) {
            return new String[]{"public", parts[0]};
        }
        return parts;
    }
}