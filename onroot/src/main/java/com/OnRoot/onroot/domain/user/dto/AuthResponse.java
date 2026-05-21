package com.OnRoot.onroot.domain.user.dto;

public record AuthResponse(
        String accessToken,
        String tokenType
) {
    public static AuthResponse of(String token) {
        return new AuthResponse(token, "Bearer");
    }
}
