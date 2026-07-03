package com.auth.practice.infrastructure.security.storage.memory;

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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

// [패턴: memory]
// OAuth 로그인 성공 후:
//   - Access Token → URL 프래그먼트(#)에 담아 리다이렉트 (JS가 읽고 변수에 저장)
//   - Refresh Token → HttpOnly 쿠키 (JS 접근 불가, 서버에서만 읽음)
//
// [왜 URL 프래그먼트?]
// OAuth는 리다이렉트 플로우이므로 응답 body를 JS가 받을 수 없다.
// 프래그먼트(#)는 서버로 전송되지 않아 서버 로그에 노출되지 않는다.
@Profile("memory")
@Component
public class MemoryOAuth2SuccessHandler extends OAuth2SuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(MemoryOAuth2SuccessHandler.class);
    private final AuthService authService;

    public MemoryOAuth2SuccessHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();
        TokenResponse tokens = authService.issueTokens(principal.getUserId(), "USER");

        // Refresh Token은 HttpOnly 쿠키 (JS 접근 불가)
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", tokens.refreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .path("/api/auth/refresh")
                .maxAge(7 * 24 * 60 * 60)
                .build();
        response.addHeader("Set-Cookie", refreshCookie.toString());

        // Access Token은 URL 프래그먼트로 전달 (JS 변수에 저장)
        String fragment = "access_token=" + URLEncoder.encode(tokens.accessToken(), StandardCharsets.UTF_8)
                + "&expires_in=" + tokens.accessTokenExpiresInMs();
        response.sendRedirect("/index.html#" + fragment);

        log.info("[memory] OAuth2 로그인 성공, access=fragment/refresh=cookie - userId: {}", principal.getUserId());
    }
}
