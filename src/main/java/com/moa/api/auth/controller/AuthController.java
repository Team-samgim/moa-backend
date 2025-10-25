package com.moa.api.auth.controller;

import com.moa.api.auth.dto.LoginRequest;
import com.moa.api.auth.dto.ReissueRequest;
import com.moa.api.auth.dto.SignupRequest;
import com.moa.api.auth.dto.TokenResponse;
import com.moa.api.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@RequestBody @Valid SignupRequest req) {
        authService.signup(req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(@RequestBody ReissueRequest req) {
        return ResponseEntity.ok(authService.reissue(req.refreshToken()));
    }

    // ★ 로그인아이디 중복확인
    @GetMapping("/exists/login-id")
    public Map<String, Boolean> existsLoginId(@RequestParam("value") String value) {
        return Map.of("exists", authService.existsLoginId(value));
    }
}