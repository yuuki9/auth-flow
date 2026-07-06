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
    void issueTokens_saves_with_jti_key() {
        TokenResponse tokens = authService.issueTokens(1L, "USER");

        assertThat(tokens.accessToken()).isNotEmpty();
        assertThat(tokens.refreshToken()).isNotEmpty();
        // jti(String)와 userId(Long)로 저장되는지 검증
        verify(tokenRepository, times(1)).save(anyString(), eq(1L), anyLong());
    }

    @Test
    void each_session_gets_independent_jti() {
        authService.issueTokens(1L, "USER");
        authService.issueTokens(1L, "USER");

        // 같은 userId로 두 번 로그인해도 서로 다른 jti로 각각 저장
        verify(tokenRepository, times(2)).save(anyString(), eq(1L), anyLong());
    }

    @Test
    void refresh_rotates_token_by_jti() {
        TokenResponse initial = authService.issueTokens(1L, "USER");
        String jti = jwtProvider.getJti(initial.refreshToken());
        when(tokenRepository.findUserIdByJti(jti)).thenReturn(Optional.of(1L));

        TokenResponse rotated = authService.refresh(initial.refreshToken());

        assertThat(rotated.accessToken()).isNotEmpty();
        verify(tokenRepository).deleteByJti(jti);
        // 최초 발급 1회 + 갱신 후 재발급 1회 = 총 2회
        verify(tokenRepository, times(2)).save(anyString(), eq(1L), anyLong());
    }

    @Test
    void refresh_throws_when_jti_not_in_redis() {
        TokenResponse initial = authService.issueTokens(1L, "USER");
        String jti = jwtProvider.getJti(initial.refreshToken());
        when(tokenRepository.findUserIdByJti(jti)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(initial.refreshToken()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void logout_deletes_only_given_session_jti() {
        TokenResponse tokens = authService.issueTokens(1L, "USER");
        String jti = jwtProvider.getJti(tokens.refreshToken());

        authService.logout(tokens.refreshToken());

        verify(tokenRepository).deleteByJti(jti);
        // userId 기반 전체 삭제는 호출되지 않음
        verify(tokenRepository, never()).deleteByJti(argThat(id -> !id.equals(jti)));
    }
}
