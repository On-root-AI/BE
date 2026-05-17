package com.OnRoot.onroot.domain.user.dto;

import java.time.LocalDateTime;

import com.OnRoot.onroot.domain.user.entity.User;

public record UserResponse(
        Long id,
        String email,
        String nickname,
        String provider,
        LocalDateTime createdAt
) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getNickname(), u.getProvider(), u.getCreatedAt());
    }
}
