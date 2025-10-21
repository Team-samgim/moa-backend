package com.moa.api.auth.service;

import com.moa.api.auth.dto.LoginRequest;
import com.moa.api.auth.dto.SignupRequest;
import com.moa.api.auth.dto.TokenResponse;
import com.moa.api.member.entity.Member;
import com.moa.api.member.entity.Role;
import com.moa.api.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.moa.global.security.jwt.TokenProvider;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final MemberRepository memberRepo;
    private final PasswordEncoder encoder;
    private final TokenProvider tokenProvider;

    @Transactional
    public void signup(SignupRequest req) {
        if (memberRepo.existsByLoginId(req.loginId()))
            throw new IllegalArgumentException("이미 사용중인 로그인 아이디입니다.");
        if (memberRepo.existsByEmail(req.email()))
            throw new IllegalArgumentException("이미 사용중인 이메일입니다.");

        Member m = new Member();
        m.setLoginId(req.loginId());
        m.setEmail(req.email());
        m.setPassword(encoder.encode(req.password()));
        m.setNickname(req.nickname());
        m.setRole(Role.ROLE_USER);
        m.setEnabled(true);
        memberRepo.save(m);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        Member m = memberRepo.findByLoginId(req.loginId())
                .orElseThrow(() -> new IllegalArgumentException("로그인 아이디 또는 비밀번호가 올바르지 않습니다."));
        if (!encoder.matches(req.password(), m.getPassword()))
            throw new IllegalArgumentException("로그인 아이디 또는 비밀번호가 올바르지 않습니다.");

        String access = tokenProvider.createAccessToken(m.getId(), m.getEmail(), m.getRole());
        return new TokenResponse(access);
    }

    public boolean existsLoginId(String loginId) {
        return memberRepo.existsByLoginId(loginId);
    }
}