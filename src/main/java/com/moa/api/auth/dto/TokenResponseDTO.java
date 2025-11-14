package com.moa.api.auth.dto;

public record TokenResponseDTO(String accessToken, String refreshToken) {}