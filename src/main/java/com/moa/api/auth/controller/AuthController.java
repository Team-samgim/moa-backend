/**
 * 작성자: 정소영
 * 설명: 회원가입, 로그인, 토큰 재발급, 아이디 중복확인 등 인증 기능을 제공하는 컨트롤러
 */
package com.moa.api.auth.controller;

import com.moa.api.auth.dto.LoginRequestDTO;
import com.moa.api.auth.dto.ReissueRequestDTO;
import com.moa.api.auth.dto.SignupRequestDTO;
import com.moa.api.auth.dto.TokenResponseDTO;
import com.moa.api.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;

@Tag(name = "인증/Auth", description = "로그인·회원가입·토큰 재발급·중복확인 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "회원가입", description = "회원 정보를 받아 신규 회원을 등록")
    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@RequestBody @Valid SignupRequestDTO req) {
        authService.signup(req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "로그인", description = "loginId/비밀번호로 로그인하여 accessToken과 refreshToken을 발급")
    @PostMapping("/login")
    public ResponseEntity<TokenResponseDTO> login(@RequestBody @Valid LoginRequestDTO req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @Operation(summary = "액세스 토큰 재발급", description = "refreshToken을 전달하면 서명·만료를 검증한 뒤 새 accessToken을 발급, refreshToken은 그대로 반환")
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponseDTO> reissue(@RequestBody ReissueRequestDTO req) {
        return ResponseEntity.ok(authService.reissue(req.refreshToken()));
    }

    @Operation(summary = "중복 아이디 확인", description = "회원 가입 시 중복 아이디 확인")
    @GetMapping("/exists/login-id")
    public Map<String, Boolean> existsLoginId(@Parameter(description = "중복 확인 할 로그인 아이디")
                                              @RequestParam("value") String value) {
        return Map.of("exists", authService.existsLoginId(value));
    }
}