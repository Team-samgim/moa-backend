package com.moa.api.detail.page.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/*****************************************************************************
 CLASS NAME    : HttpPageProperties
 DESCRIPTION   : HTTP Page 설정 프로퍼티 클래스
 - moa.detail.http-page.* 설정 값을 바인딩
 - 테이블/스키마명 및 캐시 옵션 관리
 AUTHOR        : 방대혁
 ******************************************************************************/
/**
 * HTTP Page 설정
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "moa.detail.http-page")
public class HttpPageProperties {

    /**
     * 테이블명 (기본값: http_page_sample)
     */
    private String tableName = "http_page_sample";

    /**
     * 스키마명 (기본값: public)
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

    /**
     * 스키마명을 포함한 전체 테이블명 반환
     * 예) public.http_page_sample
     */
    public String getFullTableName() {
        return schemaName + "." + tableName;
    }
}
