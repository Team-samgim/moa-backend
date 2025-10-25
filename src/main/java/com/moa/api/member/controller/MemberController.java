package com.moa.api.member.controller;

import com.moa.api.member.dto.MemberDTO;
import com.moa.api.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/me")
    public MemberDTO me(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return memberService.getMyInfo(userId);
    }
}