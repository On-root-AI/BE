package com.OnRoot.onroot.domain.user.dto;

import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(min = 2, max = 50) String nickname
) {}
