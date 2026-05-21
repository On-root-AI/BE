package com.OnRoot.onroot.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @Email String email,
        @NotBlank @Size(min = 6) String password,
        @NotBlank @Size(min = 2, max = 50) String nickname
) {}
