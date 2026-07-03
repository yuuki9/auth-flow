package com.auth.practice.infrastructure.security.storage.memory;

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

// [패턴: memory]
// Refresh Token은 쿠키에 있으므로 cookie 패턴과 동일하게 쿠키에서 읽는다.
// 갱신 시: 새 Access Token은 body로 반환 (JS가 변수에 저장), Refresh Token은 쿠키에 재설정.
@Profile("memory")
@Component
public class MemoryRefreshTokenHandler implements RefreshTokenHandler {

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
        // Refresh Token은 쿠키에 재설정
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", tokens.refreshToken())
                .httpOnly(true).secure(false).sameSite("Strict")
                .path("/api/auth/refresh").maxAge(7 * 24 * 60 * 60).build();
        response.addHeader("Set-Cookie", refreshCookie.toString());

        // Access Token은 body로 반환 (클라이언트가 JS 변수에 저장)
        return Map.of(
                "accessToken", tokens.accessToken(),
                "expiresInMs", tokens.accessTokenExpiresInMs()
        );
    }

    @Override
    public void clearOnLogout(HttpServletResponse response) {
        ResponseCookie clearRefresh = ResponseCookie.from("refresh_token", "")
                .httpOnly(true).path("/api/auth/refresh").maxAge(0).build();
        response.addHeader("Set-Cookie", clearRefresh.toString());
    }
}
