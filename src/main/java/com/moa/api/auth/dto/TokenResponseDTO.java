/****
 * 작성자: 정소영
 * 설명: 액세스 토큰과 리프레시 토큰을 담는 응답 DTO
 */
package com.moa.api.auth.dto;

public record TokenResponseDTO(String accessToken, String refreshToken) {}