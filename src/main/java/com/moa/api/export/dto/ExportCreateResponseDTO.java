package com.moa.api.export.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportCreateResponseDTO {
    private Long exportId;
    private String bucket;
    private String objectKey;
    private String s3Url;         // s3://bucket/key
    private String httpUrl;
    private String status;        // SUCCESS 등
}
