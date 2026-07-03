package com.auth.practice.infrastructure.security.storage.localstorage;

import com.auth.practice.infrastructure.security.storage.RefreshTokenHandler;
import com.auth.practice.presentation.dto.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

// [패턴: localstorage — Refresh Token 처리] ⚠️ 학습 목적 — 운영 환경에서는 사용하지 마세요.
//
// [주의] XSS 취약점 시나리오:
//        공격자가 악성 스크립트를 삽입하면:
//        1. localStorage.getItem('refreshToken') → Refresh Token 탈취
//        2. 탈취한 토큰으로 /api/auth/refresh 호출 → 새 토큰 무한 발급 가능
//        3. Redis TTL(7일) 동안 피해 지속
//        cookie 패턴의 HttpOnly 쿠키는 이 공격을 원천 차단한다.
//
// [왜 학습하는가?] 이 패턴의 위험성을 직접 경험해 cookie 패턴의 필요성을 이해하기 위함.
@Profile("localstorage")
@Component
public class LocalStorageRefreshTokenHandler implements RefreshTokenHandler {

    // [왜?] localstorage 패턴은 Refresh Token이 localStorage에 있다.
    //        쿠키가 아니므로 브라우저가 자동 전송하지 않음 → 클라이언트가 body에 직접 담아 전송.
    //        이것이 cookie/memory 패턴과의 핵심 차이: 서버가 쿠키를 읽지 않는다.
    @Override
    public String extractRefreshToken(HttpServletRequest request, String bodyToken) {
        return bodyToken;
    }

    // [왜?] 두 토큰 모두 body로 반환.
    //        클라이언트가 localStorage.setItem()으로 재저장.
    //        쿠키를 설정하지 않음 → 완전히 JS에서 관리하는 구조.
    @Override
    public Map<String, Object> buildRefreshResponse(HttpServletResponse response, TokenResponse tokens) {
        return Map.of(
                "accessToken", tokens.accessToken(),
                "refreshToken", tokens.refreshToken(),
                "expiresInMs", tokens.accessTokenExpiresInMs()
        );
    }

    // [왜?] 서버가 삭제할 쿠키가 없다.
    //        localStorage는 클라이언트 JS 영역이므로 서버에서 직접 삭제 불가.
    //        클라이언트가 localStorage.removeItem()을 호출해야 한다.
    @Override
    public void clearOnLogout(HttpServletResponse response) {
        // 클라이언트 측 처리: localStorage.removeItem('accessToken'), removeItem('refreshToken')
    }
}
