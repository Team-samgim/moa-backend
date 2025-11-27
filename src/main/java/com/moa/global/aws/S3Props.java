package com.moa.global.aws;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * AWS S3 설정 Properties
 * ------------------------------------------------------------
 * application.yml 예시:
 *
 * aws:
 *   region: ap-northeast-2
 *   s3:
 *     bucket: my-bucket
 *     prefix: app/exports/dev/
 *
 * - 리전 형식 유효성 검증
 * - 버킷 이름 규칙 검증
 * - Prefix 정규화 제공
 *
 * AUTHOR        : 방대혁
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "aws")
public class S3Props {

    /**
     * AWS 리전
     *
     * 예: ap-northeast-2 (서울), us-east-1 (버지니아 북부)
     */
    @NotBlank(message = "AWS 리전이 설정되지 않았습니다. application.yml에 aws.region을 추가하세요.")
    @Pattern(
            regexp = "^[a-z]{2}-[a-z]+-\\d{1}$",
            message = "유효하지 않은 AWS 리전 형식입니다. (예: ap-northeast-2)"
    )
    private String region;

    /**
     * S3 상세 설정
     */
    @Valid
    private S3 s3 = new S3();

    /**
     * S3 세부 설정 클래스
     */
    @Getter
    @Setter
    public static class S3 {

        /**
         * S3 버킷 이름
         *
         * 규칙:
         * - 3~63자
         * - 소문자, 숫자, 하이픈(-)만 허용
         */
        @NotBlank(message = "S3 버킷 이름이 설정되지 않았습니다. application.yml에 aws.s3.bucket을 추가하세요.")
        @Pattern(
                regexp = "^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$",
                message = "유효하지 않은 S3 버킷 이름입니다. (3-63자, 소문자/숫자/하이픈만 허용)"
        )
        private String bucket;

        /**
         * S3 객체 키 접두사 (선택)
         *
         * 예: app/exports/dev/
         * 기본값: ""
         */
        private String prefix = "";

        /**
         * Prefix 정규화
         * - null → ""
         * - 뒤에 "/" 자동 추가
         */
        public String getNormalizedPrefix() {
            if (prefix == null || prefix.isBlank()) {
                return "";
            }
            String trimmed = prefix.trim();
            return trimmed.endsWith("/") ? trimmed : trimmed + "/";
        }
    }
}
