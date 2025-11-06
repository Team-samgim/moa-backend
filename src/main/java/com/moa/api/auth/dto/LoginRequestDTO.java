package com.moa.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank String loginId,
        @NotBlank String password
) {}