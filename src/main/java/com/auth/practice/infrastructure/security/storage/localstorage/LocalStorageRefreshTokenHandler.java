package com.auth.practice.infrastructure.security.storage.localstorage;

import com.auth.practice.infrastructure.security.storage.RefreshTokenHandler;
import com.auth.practice.presentation.dto.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

// [패턴: localstorage] ⚠️ 학습 목적 — 운영 환경에서는 사용하지 마세요.
// Refresh Token이 localStorage에 있으므로 클라이언트가 request body에 담아 보낸다.
// 갱신된 토큰도 response body로 반환 — 클라이언트가 localStorage에 재저장.
@Profile("localstorage")
@Component
public class LocalStorageRefreshTokenHandler implements RefreshTokenHandler {

    @Override
    public String extractRefreshToken(HttpServletRequest request, String bodyToken) {
        // [XSS 위험] localStorage에서 꺼낸 토큰을 body에 담아 전송
        return bodyToken;
    }

    @Override
    public Map<String, Object> buildRefreshResponse(HttpServletResponse response, TokenResponse tokens) {
        // Access + Refresh 모두 body로 반환 (클라이언트가 localStorage에 재저장)
        return Map.of(
                "accessToken", tokens.accessToken(),
                "refreshToken", tokens.refreshToken(),
                "expiresInMs", tokens.accessTokenExpiresInMs()
        );
    }

    @Override
    public void clearOnLogout(HttpServletResponse response) {
        // 쿠키 없음 — 클라이언트가 localStorage.removeItem()으로 직접 삭제
    }
}
