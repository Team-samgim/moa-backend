package com.moa.api.member.dto;

public record MemberDTO(Long id, String loginId, String email, String nickname, String role) {}