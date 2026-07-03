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

// [패턴: memory — Refresh Token 처리]
// [현업패턴] memory 패턴의 보안 특성:
//            - Access Token: JS 변수 저장 → XSS 시 탈취 가능하지만 TTL 짧음(15분).
//            - Refresh Token: HttpOnly 쿠키 → XSS로 탈취 불가.
//            cookie 패턴보다 Access Token 노출 위험은 높지만, Refresh Token은 동일하게 보호.
//            "탭 닫으면 Access Token 소멸" → 공용 PC 등 환경에서 유리.
@Profile("memory")
@Component
public class MemoryRefreshTokenHandler implements RefreshTokenHandler {

    // [왜?] memory 패턴에서도 Refresh Token은 cookie에서 읽는다.
    //        MemoryOAuth2SuccessHandler가 Refresh Token을 HttpOnly 쿠키에 저장하기 때문.
    //        이 점이 memory와 localstorage의 가장 큰 차이: localstorage는 body에서 읽는다.
    @Override
    public String extractRefreshToken(HttpServletRequest request, String bodyToken) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> "refresh_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    // [왜?] Refresh Token은 쿠키에 재설정, Access Token은 body로 반환.
    //        JS가 body의 accessToken을 받아 변수에 저장 → 이후 API 요청 시 Authorization 헤더로 전송.
    //        Silent Refresh: 401 응답 시 /api/auth/refresh를 호출해 새 accessToken을 자동으로 받아 재시도.
    @Override
    public Map<String, Object> buildRefreshResponse(HttpServletResponse response, TokenResponse tokens) {
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", tokens.refreshToken())
                .httpOnly(true).secure(false).sameSite("Strict")
                .path("/api/auth/refresh").maxAge(7 * 24 * 60 * 60).build();
        response.addHeader("Set-Cookie", refreshCookie.toString());

        return Map.of(
                "accessToken", tokens.accessToken(),
                "expiresInMs", tokens.accessTokenExpiresInMs()
        );
    }

    // [왜?] 로그아웃 시 Refresh Token 쿠키만 삭제.
    //        Access Token은 JS 변수에 있으므로 클라이언트가 변수를 null로 초기화하면 소멸.
    @Override
    public void clearOnLogout(HttpServletResponse response) {
        ResponseCookie clearRefresh = ResponseCookie.from("refresh_token", "")
                .httpOnly(true).path("/api/auth/refresh").maxAge(0).build();
        response.addHeader("Set-Cookie", clearRefresh.toString());
    }
}
