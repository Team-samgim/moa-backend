package com.moa.api.export.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DownloadUrlDTO {
    private String httpUrl;
}
