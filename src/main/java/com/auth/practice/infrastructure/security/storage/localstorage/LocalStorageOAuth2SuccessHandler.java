package com.auth.practice.infrastructure.security.storage.localstorage;

import com.auth.practice.application.auth.AuthService;
import com.auth.practice.infrastructure.security.oauth.CustomOAuth2User;
import com.auth.practice.infrastructure.security.oauth.OAuth2SuccessHandler;
import com.auth.practice.presentation.dto.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

// [패턴: localstorage] ⚠️ 학습 목적 — 운영 환경에서는 사용하지 마세요.
// OAuth 로그인 성공 후 Access Token과 Refresh Token을 모두 URL 프래그먼트로 전달한다.
// 클라이언트 JS가 둘 다 localStorage에 저장한다.
//
// [XSS 취약점]
// localStorage에 저장된 토큰은 JS로 언제든지 읽을 수 있다.
// 악성 스크립트 주입 시: localStorage.getItem('refreshToken') → 탈취 → 무제한 재발급 가능.
// cookie 패턴의 HttpOnly 쿠키와 달리 JS 차단이 없다.
@Profile("localstorage")
@Component
public class LocalStorageOAuth2SuccessHandler extends OAuth2SuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageOAuth2SuccessHandler.class);
    private final AuthService authService;

    public LocalStorageOAuth2SuccessHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();
        TokenResponse tokens = authService.issueTokens(principal.getUserId(), "USER");

        // Access Token + Refresh Token 모두 URL 프래그먼트로 전달
        // 클라이언트 JS: localStorage.setItem('accessToken', ...)
        //                localStorage.setItem('refreshToken', ...)
        String fragment = "access_token=" + URLEncoder.encode(tokens.accessToken(), StandardCharsets.UTF_8)
                + "&refresh_token=" + URLEncoder.encode(tokens.refreshToken(), StandardCharsets.UTF_8)
                + "&expires_in=" + tokens.accessTokenExpiresInMs();
        response.sendRedirect("/index.html#" + fragment);

        log.warn("[localstorage] OAuth2 로그인 성공, 학습용 패턴 — 운영 환경에서 사용 금지. userId: {}", principal.getUserId());
    }
}
