package com.moa.api.mypage.service;

import com.moa.api.export.repository.ExportFileRepository;
import com.moa.api.mypage.dto.MypageStatsDto;
import com.moa.api.mypage.repository.MypageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*****************************************************************************
 CLASS NAME    : MypageService
 DESCRIPTION   : 마이페이지 통계 조회 비즈니스 로직
 - 총 프리셋 수
 - 즐겨찾기 프리셋 수
 - 총 생성 문서(export) 수
 AUTHOR        : 방대혁
 ******************************************************************************/
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MypageService {

    private final MypageRepository mypageRepository;
    private final ExportFileRepository exportFileRepository;

    /**
     * 회원의 마이페이지 통계 조회
     *
     * @param memberId 로그인한 회원 ID
     * @return MypageStatsDto (총 프리셋/즐겨찾기/Export 개수)
     */
    public MypageStatsDto getMyStats(Long memberId) {
        long totalPresets = mypageRepository.countByMemberId(memberId);
        long favoritePresets = mypageRepository.countFavoriteByMemberId(memberId);
        long totalExports = exportFileRepository.countByMemberId(memberId);

        return MypageStatsDto.builder()
                .totalPresets(totalPresets)
                .favoritePresets(favoritePresets)
                .totalExports(totalExports)
                .build();
    }
}
