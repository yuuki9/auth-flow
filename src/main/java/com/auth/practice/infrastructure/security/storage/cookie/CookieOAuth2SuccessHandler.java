package com.auth.practice.infrastructure.security.storage.cookie;

import com.auth.practice.application.auth.AuthService;
import com.auth.practice.infrastructure.security.oauth.CustomOAuth2User;
import com.auth.practice.infrastructure.security.oauth.OAuth2SuccessHandler;
import com.auth.practice.presentation.dto.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.IOException;

// [패턴: cookie]
// OAuth 로그인 성공 후 Access + Refresh Token을 모두 HttpOnly 쿠키에 담는다.
// JS에서 토큰을 읽을 수 없으므로 XSS 공격으로부터 보호된다.
// CSRF 방어: SameSite=Strict
@Profile("cookie")
@Component
public class CookieOAuth2SuccessHandler extends OAuth2SuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(CookieOAuth2SuccessHandler.class);
    private final AuthService authService;

    public CookieOAuth2SuccessHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();
        TokenResponse tokens = authService.issueTokens(principal.getUserId(), "USER");

        ResponseCookie accessCookie = ResponseCookie.from("access_token", tokens.accessToken())
                .httpOnly(true)
                .secure(false) // 운영 환경에서는 true (HTTPS 필수)
                .sameSite("Strict")
                .path("/")
                .maxAge(tokens.accessTokenExpiresInMs() / 1000)
                .build();

        // [보안] Refresh Token은 /api/auth/refresh 경로에만 전송되도록 path 제한
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", tokens.refreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .path("/api/auth/refresh")
                .maxAge(7 * 24 * 60 * 60)
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
        response.sendRedirect("/index.html");

        log.info("[cookie] OAuth2 로그인 성공, 쿠키 발급 - userId: {}", principal.getUserId());
    }
}
