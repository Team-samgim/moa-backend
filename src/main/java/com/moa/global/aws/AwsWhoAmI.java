package com.moa.global.aws;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;

/**
 * AWS STS Caller Identity 조회 컴포넌트
 * ------------------------------------------------------------
 * - 서버가 기동될 때 @PostConstruct 단계에서 AWS 자격 증명을 확인.
 * - 현재 애플리케이션이 어떤 AWS 계정/역할로 실행 중인지 로그 출력.
 * - IAM Role 정상 적용 여부, EC2/Container 권한 설정 등을 검증할 때 사용.
 *
 * AUTHOR        : 방대혁
 */
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
            log.info("[AWS] Account={}, Arn={}, UserId={}",
                    id.account(), id.arn(), id.userId());

        } catch (Exception e) {
            log.error("[AWS] getCallerIdentity 실패: {}", e.toString(), e);
        }
    }
}
