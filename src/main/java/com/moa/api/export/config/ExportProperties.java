package com.moa.api.export.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Export 설정
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "moa.export")
public class ExportProperties {

    /**
     * CSV 기본 설정
     */
    private Csv csv = new Csv();

    /**
     * S3 업로드 설정
     */
    private S3Upload s3Upload = new S3Upload();

    /**
     * 프리사인드 URL 설정
     */
    private Presign presign = new Presign();

    @Getter
    @Setter
    public static class Csv {
        /**
         * 기본 fetch size
         */
        private int defaultFetchSize = 5000;

        /**
         * 최대 fetch size
         */
        private int maxFetchSize = 10000;

        /**
         * 파일명 최대 길이
         */
        private int maxFileNameLength = 120;

        /**
         * CSV 인코딩
         */
        private String encoding = "UTF-8";
    }

    @Getter
    @Setter
    public static class S3Upload {
        /**
         * 임시 파일 접두사
         */
        private String tmpPrefix = "grid-export-";

        /**
         * Content-Type
         */
        private String contentType = "text/csv; charset=utf-8";
    }

    @Getter
    @Setter
    public static class Presign {
        /**
         * 프리사인드 URL 만료 시간 (분)
         */
        private int expirationMinutes = 10;

        /**
         * 다운로드 URL 만료 시간 (분)
         */
        private int downloadExpirationMinutes = 5;
    }
}