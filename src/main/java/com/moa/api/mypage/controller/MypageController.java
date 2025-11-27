package com.moa.api.mypage.controller;

import com.moa.api.mypage.dto.MypageStatsDto;
import com.moa.api.mypage.service.MypageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*****************************************************************************
 CLASS NAME    : MypageController
 DESCRIPTION   : 마이페이지 관련 API Controller.
 - 로그인한 유저의 통계 정보 조회(stats)
 AUTHOR        : 방대혁
 ******************************************************************************/
@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MypageController {

    private final MypageService mypageService;

    /* -------------------------------------------------------------------------
     *  GET /api/mypage/stats
     *  - 로그인된 유저의 통계 정보 조회
     * ------------------------------------------------------------------------- */
    @GetMapping("/stats")
    public ResponseEntity<MypageStatsDto> getMyStats(
            @AuthenticationPrincipal Long memberId) {

        MypageStatsDto stats = mypageService.getMyStats(memberId);
        return ResponseEntity.ok(stats);
    }
}
