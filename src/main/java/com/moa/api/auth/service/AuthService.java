/**
 * 작성자: 정소영
 * 설명: 회원가입, 로그인, 토큰 재발급 및 중복 체크 등 인증 관련 비즈니스 로직을 처리하는 서비스
 */
package com.moa.api.auth.service;

import com.moa.api.auth.dto.LoginRequestDTO;
import com.moa.api.auth.dto.SignupRequestDTO;
import com.moa.api.auth.dto.TokenResponseDTO;
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
    public void signup(SignupRequestDTO req) {
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
    public TokenResponseDTO login(LoginRequestDTO req) {
        Member m = memberRepo.findByLoginId(req.loginId())
                .orElseThrow(() -> new IllegalArgumentException("로그인 아이디 또는 비밀번호가 올바르지 않습니다."));
        if (!encoder.matches(req.password(), m.getPassword()))
            throw new IllegalArgumentException("로그인 아이디 또는 비밀번호가 올바르지 않습니다.");

        String access = tokenProvider.createAccessToken(m.getId(), m.getLoginId(), m.getRole());
        String refresh = tokenProvider.createRefreshToken(m.getLoginId(), m.getRole());
        return new TokenResponseDTO(access, refresh);
    }

    @Transactional(readOnly = true)
    public TokenResponseDTO reissue(String refreshToken) {
        var jws = tokenProvider.parse(refreshToken);
        var claims = jws.getBody();

        String tokenType = claims.get("tokenType", String.class);
        if (!"refresh".equals(tokenType)) {
            throw new IllegalArgumentException("유효한 리프레시 토큰이 아닙니다.");
        }

        String loginId = claims.getSubject();
        Number uidNum = claims.get("uid", Number.class);

        Long memberId;
        if (uidNum != null) {
            memberId = uidNum.longValue();
        } else {
            memberId = memberRepo.findByLoginId(loginId)
                    .map(Member::getId)
                    .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));
        }

        Member m = memberRepo.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        String newAccess = tokenProvider.createAccessToken(m.getId(), m.getLoginId(), m.getRole());
        return new TokenResponseDTO(newAccess, refreshToken);
    }

    public boolean existsLoginId(String loginId) {
        return memberRepo.existsByLoginId(loginId);
    }
}