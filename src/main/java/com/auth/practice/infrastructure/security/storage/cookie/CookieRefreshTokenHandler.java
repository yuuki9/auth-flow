package com.auth.practice.infrastructure.security.storage.cookie;

import com.auth.practice.infrastructure.security.storage.RefreshTokenHandler;
import com.auth.practice.presentation.dto.TokenResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

// [패턴: cookie]
// Refresh Token을 HttpOnly 쿠키에서 읽고, 갱신된 토큰도 쿠키에 저장한다.
@Profile("cookie")
@Component
public class CookieRefreshTokenHandler implements RefreshTokenHandler {

    @Override
    public String extractRefreshToken(HttpServletRequest request, String bodyToken) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> "refresh_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    @Override
    public Map<String, Object> buildRefreshResponse(HttpServletResponse response, TokenResponse tokens) {
        ResponseCookie accessCookie = ResponseCookie.from("access_token", tokens.accessToken())
                .httpOnly(true).secure(false).sameSite("Strict")
                .path("/").maxAge(tokens.accessTokenExpiresInMs() / 1000).build();
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", tokens.refreshToken())
                .httpOnly(true).secure(false).sameSite("Strict")
                .path("/api/auth/refresh").maxAge(7 * 24 * 60 * 60).build();
        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
        return Map.of("message", "토큰 갱신 성공");
    }

    @Override
    public void clearOnLogout(HttpServletResponse response) {
        ResponseCookie clearAccess = ResponseCookie.from("access_token", "")
                .httpOnly(true).path("/").maxAge(0).build();
        ResponseCookie clearRefresh = ResponseCookie.from("refresh_token", "")
                .httpOnly(true).path("/api/auth/refresh").maxAge(0).build();
        response.addHeader("Set-Cookie", clearAccess.toString());
        response.addHeader("Set-Cookie", clearRefresh.toString());
    }
}
