package com.moa.global.aws;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "aws")
public class S3Props {

    @NotBlank(message = "aws.region이 비어있습니다.")
    private String region;

    @Valid
    private S3 s3 = new S3();

    @Getter @Setter
    public static class S3 {
        @NotBlank(message = "aws.s3.bucket이 비어있습니다.")
        private String bucket;
        // 선택 항목이면 기본값 비워둬도 됨
        private String prefix = "";
    }
}