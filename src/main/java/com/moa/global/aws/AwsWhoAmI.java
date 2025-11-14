package com.moa.global.aws;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;

@Slf4j
@Component
public class AwsWhoAmI {
    @PostConstruct
    public void init() {
        try (var sts = StsClient.builder()
                .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "ap-northeast-2")))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build()) {
            var id = sts.getCallerIdentity();
            log.info("[AWS] Account={}, Arn={}, UserId={}", id.account(), id.arn(), id.userId());
        } catch (Exception e) {
            log.error("[AWS] getCallerIdentity 실패: {}", e.toString(), e);
        }
    }
}