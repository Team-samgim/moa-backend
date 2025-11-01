package com.moa.global.aws;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableConfigurationProperties(S3Props.class)
public class S3Config {

    @Bean
    public S3Client s3Client(S3Props p) {
        return S3Client.builder()
                .region(Region.of(p.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}