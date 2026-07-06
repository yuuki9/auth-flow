package com.auth.practice.infrastructure.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-must-be-at-least-256-bits-long-for-hs256");
        props.setAccessTokenExpiryMs(900000L);
        props.setRefreshTokenExpiryMs(604800000L);
        jwtProvider = new JwtProvider(props);
    }

    @Test
    void generateAccessToken_returns_valid_token() {
        String token = jwtProvider.generateAccessToken(1L, "USER");
        assertThat(jwtProvider.validateToken(token)).isTrue();
        assertThat(jwtProvider.getUserId(token)).isEqualTo(1L);
        assertThat(jwtProvider.getRole(token)).isEqualTo("USER");
    }

    @Test
    void generateRefreshToken_returns_valid_token_with_jti() {
        String token = jwtProvider.generateRefreshToken(1L);
        assertThat(jwtProvider.validateToken(token)).isTrue();
        assertThat(jwtProvider.getUserId(token)).isEqualTo(1L);
        assertThat(jwtProvider.getJti(token)).isNotEmpty();
    }

    @Test
    void each_refreshToken_has_unique_jti() {
        String token1 = jwtProvider.generateRefreshToken(1L);
        String token2 = jwtProvider.generateRefreshToken(1L);
        assertThat(jwtProvider.getJti(token1)).isNotEqualTo(jwtProvider.getJti(token2));
    }

    @Test
    void validateToken_returns_false_for_tampered_token() {
        String token = jwtProvider.generateAccessToken(1L, "USER");
        assertThat(jwtProvider.validateToken(token + "tampered")).isFalse();
    }

    @Test
    void validateToken_returns_false_for_expired_token() {
        JwtProperties shortProps = new JwtProperties();
        shortProps.setSecret("test-secret-key-must-be-at-least-256-bits-long-for-hs256");
        shortProps.setAccessTokenExpiryMs(-1000L);
        shortProps.setRefreshTokenExpiryMs(-1000L);
        JwtProvider shortJwt = new JwtProvider(shortProps);

        String token = shortJwt.generateAccessToken(1L, "USER");
        assertThat(shortJwt.validateToken(token)).isFalse();
    }
}
