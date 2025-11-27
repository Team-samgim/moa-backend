package com.moa.api.grid.config;

/*****************************************************************************
 CLASS NAME    : GridConfig
 DESCRIPTION   : Grid API 기본 설정 및 레이어/필드 메타 매핑 구성
 AUTHOR        : 방대혁
 ******************************************************************************/

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Grid API 설정
 */
@Configuration
public class GridConfig {

    @Bean
    public GridProperties gridProperties() {
        GridProperties props = new GridProperties();

        // 기본 설정
        props.setDefaultTimeZone("Asia/Seoul");
        props.setTsWoTzIsUtc(true);
        props.setMaxDistinctLimit(500);
        props.setDefaultPageSize(100);
        props.setDefaultOrderBy("ts_server_nsec");

        // 레이어 테이블 매핑
        Map<String, String> layerMapping = new HashMap<>();
        layerMapping.put("HTTP_PAGE", "public.http_page_sample");
        layerMapping.put("HTTP_URI", "public.http_uri_sample");
        layerMapping.put("L4_TCP", "public.l4_tcp_sample");
        layerMapping.put("ETHERNET", "public.ethernet_sample");
        props.setLayerTableMapping(layerMapping);

        // 필드 메타 테이블 매핑
        Map<String, String> fieldMetaMapping = new HashMap<>();
        fieldMetaMapping.put("http_page", "http_page_fields");
        fieldMetaMapping.put("http_uri", "http_uri_fields");
        fieldMetaMapping.put("l4_tcp", "l4_tcp_fields");
        fieldMetaMapping.put("ethernet", "ethernet_fields");
        props.setFieldMetaTableMapping(fieldMetaMapping);

        // 비동기 설정
        GridProperties.Async async = new GridProperties.Async();
        async.setCorePoolSize(10);
        async.setMaxPoolSize(50);
        async.setQueueCapacity(100);
        async.setThreadNamePrefix("grid-async-");
        props.setAsync(async);

        return props;
    }
}
