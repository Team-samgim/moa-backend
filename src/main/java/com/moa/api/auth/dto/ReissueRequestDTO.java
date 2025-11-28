/****
 * 작성자: 정소영
 * 설명: 액세스 토큰 재발급을 위한 리프레시 토큰 요청 DTO
 */
package com.moa.api.auth.dto;

public record ReissueRequestDTO(String refreshToken) {}
