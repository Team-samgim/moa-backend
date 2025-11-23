package com.moa.api.mypage.controller;

import com.moa.api.mypage.dto.MypageStatsDto;
import com.moa.api.mypage.service.MypageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MypageController {

    private final MypageService mypageService;

    @GetMapping("/stats")
    public ResponseEntity<MypageStatsDto> getMyStats(
            @AuthenticationPrincipal Long memberId) {
        MypageStatsDto stats = mypageService.getMyStats(memberId);
        return ResponseEntity.ok(stats);
    }
}