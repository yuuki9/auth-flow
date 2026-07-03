package com.auth.practice.presentation.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresInMs
) {}
