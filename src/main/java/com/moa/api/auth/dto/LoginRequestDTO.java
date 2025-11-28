/****
 * 작성자: 정소영
 * 설명: 로그인 요청 정보를 담는 DTO (로그인 아이디와 비밀번호)
 */
package com.moa.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank String loginId,
        @NotBlank String password
) {}