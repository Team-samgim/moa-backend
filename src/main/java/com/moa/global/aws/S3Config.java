package com.moa.global.aws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * S3 설정
 * ------------------------------------------------------------
 * - S3Client / S3Presigner 빈 생성
 * - AWS SDK v2 사용
 * - Region 및 CredentialsProvider 설정 주입
 *
 * AUTHOR        : 방대혁
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(S3Props.class)
public class S3Config {

    /**
     * S3 Client 빈 생성
     *
     * @param props S3 설정 (application.yml에서 주입)
     * @return S3Client
     */
    @Bean
    public S3Client s3Client(S3Props props) {
        log.info("Creating S3Client bean: region={}, bucket={}",
                props.getRegion(), props.getS3().getBucket());

        S3Client client = S3Client.builder()
                .region(Region.of(props.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        log.info("S3Client bean created successfully");

        return client;
    }

    /**
     * S3 Presigner 빈 생성
     * ------------------------------------------------------------
     * - Presigned URL(다운로드/업로드) 생성에 사용
     *
     * @param props S3 설정
     * @return S3Presigner
     */
    @Bean
    public S3Presigner s3Presigner(S3Props props) {
        log.info("Creating S3Presigner bean: region={}", props.getRegion());

        S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(props.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        log.info("S3Presigner bean created successfully");

        return presigner;
    }
}
