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

// [패턴: cookie — Refresh Token 처리]
// [현업패턴] Refresh Token을 HttpOnly 쿠키로 관리하면:
//            - JS에서 접근 불가 → XSS로 Refresh Token 탈취 불가.
//            - 브라우저가 해당 경로(/api/auth/refresh)로만 자동 전송 → 불필요한 노출 차단.
//            - SameSite=Strict → 외부 사이트에서 시작한 요청에는 쿠키 전송 안 함 → CSRF 방어.
@Profile("cookie")
@Component
public class CookieRefreshTokenHandler implements RefreshTokenHandler {

    // [왜?] bodyToken 파라미터는 사용하지 않는다.
    //        cookie 패턴에서 Refresh Token은 항상 HttpOnly 쿠키에 있으므로 body에서 읽을 필요 없음.
    //        localstorage 패턴과 인터페이스를 통일하기 위해 파라미터만 유지.
    @Override
    public String extractRefreshToken(HttpServletRequest request, String bodyToken) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> "refresh_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    // [현업패턴] 갱신된 토큰을 쿠키에 덮어쓴다 (Rotation).
    //            응답 body에는 토큰이 포함되지 않음 → JS에서 접근 불가 상태 유지.
    @Override
    public Map<String, Object> buildRefreshResponse(HttpServletResponse response, TokenResponse tokens) {
        ResponseCookie accessCookie = ResponseCookie.from("access_token", tokens.accessToken())
                .httpOnly(true).secure(false).sameSite("Strict")
                .path("/").maxAge(tokens.accessTokenExpiresInMs() / 1000).build();

        // [보안] Refresh Token 쿠키의 path를 /api/auth/refresh로 제한.
        //        브라우저가 이 경로로 요청할 때만 쿠키를 전송 → 불필요한 노출 방지.
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", tokens.refreshToken())
                .httpOnly(true).secure(false).sameSite("Strict")
                .path("/api/auth/refresh").maxAge(7 * 24 * 60 * 60).build();
        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
        return Map.of("message", "토큰 갱신 성공");
    }

    // [왜?] 로그아웃 시 쿠키를 maxAge=0으로 덮어쓰면 브라우저가 즉시 삭제.
    //        JS에서 document.cookie로 삭제할 수 없으므로(HttpOnly) 서버가 직접 처리.
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
