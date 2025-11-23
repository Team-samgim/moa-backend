package com.moa.api.mypage.service;

import com.moa.api.export.repository.ExportFileRepository;
import com.moa.api.mypage.dto.MypageStatsDto;
import com.moa.api.mypage.repository.MypageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MypageService {

    private final MypageRepository mypageRepository;
    private final ExportFileRepository exportFileRepository;

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