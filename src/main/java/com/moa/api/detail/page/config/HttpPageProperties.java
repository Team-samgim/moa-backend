package com.moa.api.detail.page.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * HTTP Page 설정
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "moa.detail.http-page")
public class HttpPageProperties {

    /**
     * 테이블명
     */
    private String tableName = "http_page_sample";

    /**
     * 스키마명
     */
    private String schemaName = "public";

    /**
     * 캐시 설정
     */
    private Cache cache = new Cache();

    @Getter
    @Setter
    public static class Cache {
        /**
         * 캐시 활성화 여부
         */
        private boolean enabled = true;

        /**
         * 캐시 TTL (초)
         */
        private int ttl = 300; // 5분

        /**
         * 최대 캐시 크기
         */
        private int maxSize = 1000;
    }

    public String getFullTableName() {
        return schemaName + "." + tableName;
    }
}