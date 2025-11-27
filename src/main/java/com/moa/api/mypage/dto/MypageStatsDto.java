package com.moa.api.mypage.dto;

import lombok.*;

/*****************************************************************************
 CLASS NAME    : MypageStatsDto
 DESCRIPTION   : 마이페이지 통계 정보 DTO
 - 유저의 프리셋/즐겨찾기/내보내기 문서 수를 담는 응답 모델
 AUTHOR        : 방대혁
 ******************************************************************************/
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MypageStatsDto {

    /** 총 프리셋 수 */
    private long totalPresets;

    /** 즐겨찾기된 프리셋 수 */
    private long favoritePresets;

    /** 총 생성(Export) 문서 수 */
    private long totalExports;
}
