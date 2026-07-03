package com.auth.practice.application.auth;

import com.auth.practice.domain.token.RefreshTokenRepository;
import com.auth.practice.infrastructure.security.jwt.JwtProperties;
import com.auth.practice.infrastructure.security.jwt.JwtProvider;
import com.auth.practice.presentation.dto.TokenResponse;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository tokenRepository;
    private final JwtProperties jwtProperties;

    public AuthService(JwtProvider jwtProvider,
                       RefreshTokenRepository tokenRepository,
                       JwtProperties jwtProperties) {
        this.jwtProvider = jwtProvider;
        this.tokenRepository = tokenRepository;
        this.jwtProperties = jwtProperties;
    }

    public TokenResponse issueTokens(Long userId, String role) {
        String accessToken = jwtProvider.generateAccessToken(userId, role);
        String refreshToken = jwtProvider.generateRefreshToken(userId);

        long ttlSeconds = jwtProperties.getRefreshTokenExpiryMs() / 1000;
        tokenRepository.save(userId, refreshToken, ttlSeconds);

        return new TokenResponse(accessToken, refreshToken,
                jwtProperties.getAccessTokenExpiryMs());
    }

    public TokenResponse refresh(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token");
        }

        Long userId = jwtProvider.getUserId(refreshToken);

        String stored = tokenRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Refresh Token이 Redis에 없음 (로그아웃 또는 탈취 감지)"));

        if (!stored.equals(refreshToken)) {
            tokenRepository.deleteByUserId(userId);
            throw new IllegalArgumentException("Refresh Token 재사용 감지 — 보안 위협");
        }

        tokenRepository.deleteByUserId(userId);
        return issueTokens(userId, "USER");
    }

    public void logout(Long userId) {
        tokenRepository.deleteByUserId(userId);
    }
}
