package com.moa.api.grid.config;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Grid API 설정 프로퍼티
 */
@Data
public class GridProperties {

    /**
     * 기본 타임존 (KST 변환용)
     */
    private String defaultTimeZone = "Asia/Seoul";

    /**
     * timestamp without timezone를 UTC로 간주할지 여부
     */
    private Boolean tsWoTzIsUtc = true;

    /**
     * DISTINCT 조회 최대 limit
     */
    private Integer maxDistinctLimit = 500;

    /**
     * 기본 페이지 크기
     */
    private Integer defaultPageSize = 100;

    /**
     * DISTINCT 조회 시 기본 정렬 컬럼
     */
    private String defaultOrderBy = "ts_server_nsec";

    /**
     * 레이어 → 테이블 매핑
     */
    private Map<String, String> layerTableMapping = new HashMap<>();

    /**
     * 레이어별 필드 메타 테이블 매핑
     */
    private Map<String, String> fieldMetaTableMapping = new HashMap<>();

    /**
     * 비동기 처리 설정
     */
    private Async async = new Async();

    @Data
    public static class Async {
        private Integer corePoolSize = 10;
        private Integer maxPoolSize = 50;
        private Integer queueCapacity = 100;
        private String threadNamePrefix = "grid-async-";
    }
}