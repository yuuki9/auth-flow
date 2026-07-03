package com.auth.practice.presentation.dto;

import com.auth.practice.domain.user.User;

public record UserInfoResponse(
        Long userId,
        String email,
        String name,
        String profileImageUrl,
        String provider,
        String role
) {
    public static UserInfoResponse from(User user) {
        return new UserInfoResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getProfileImageUrl(),
                user.getProvider().name(),
                user.getRole().name()
        );
    }
}
