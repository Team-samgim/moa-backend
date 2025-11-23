package com.moa.api.mypage.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MypageStatsDto {
    private long totalPresets;      // 총 프리셋 수
    private long favoritePresets;   // 즐겨찾기 프리셋 수
    private long totalExports;      // 총 생성 문서 수
}