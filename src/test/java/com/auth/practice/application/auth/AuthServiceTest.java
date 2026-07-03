package com.auth.practice.application.auth;

import com.auth.practice.domain.token.RefreshTokenRepository;
import com.auth.practice.infrastructure.security.jwt.JwtProperties;
import com.auth.practice.infrastructure.security.jwt.JwtProvider;
import com.auth.practice.presentation.dto.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private RefreshTokenRepository tokenRepository;

    private AuthService authService;
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-must-be-at-least-256-bits-long-for-hs256");
        props.setAccessTokenExpiryMs(900000L);
        props.setRefreshTokenExpiryMs(604800000L);
        jwtProvider = new JwtProvider(props);
        authService = new AuthService(jwtProvider, tokenRepository, props);
    }

    @Test
    void issueTokens_saves_refreshToken_to_redis() {
        TokenResponse tokens = authService.issueTokens(1L, "USER");

        assertThat(tokens.accessToken()).isNotEmpty();
        assertThat(tokens.refreshToken()).isNotEmpty();
        verify(tokenRepository, times(1)).save(eq(1L), anyString(), anyLong());
    }

    @Test
    void refresh_rotates_refreshToken() {
        TokenResponse initial = authService.issueTokens(1L, "USER");
        when(tokenRepository.findByUserId(1L)).thenReturn(Optional.of(initial.refreshToken()));

        TokenResponse rotated = authService.refresh(initial.refreshToken());

        assertThat(rotated.accessToken()).isNotEmpty();
        verify(tokenRepository).deleteByUserId(1L);
        verify(tokenRepository, times(2)).save(eq(1L), anyString(), anyLong());
    }

    @Test
    void refresh_throws_when_token_not_in_redis() {
        TokenResponse initial = authService.issueTokens(1L, "USER");
        when(tokenRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(initial.refreshToken()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void logout_deletes_refreshToken_from_redis() {
        authService.logout(1L);
        verify(tokenRepository).deleteByUserId(1L);
    }
}
