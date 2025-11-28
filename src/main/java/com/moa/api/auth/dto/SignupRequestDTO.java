/**
 * 작성자: 정소영
 * 설명: 회원가입 요청 정보를 담는 DTO (로그인 아이디, 이메일, 비밀번호, 닉네임)
 */
package com.moa.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequestDTO(
        @NotBlank @Size(min = 4, max = 32)
        @Pattern(regexp = "^[a-z0-9_\\-]+$", message = "영문 소문자/숫자/언더스코어/하이픈만 허용")
        String loginId,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 64) String password,
        @NotBlank @Size(max = 30) String nickname
) {}