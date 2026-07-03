package com.auth.practice.infrastructure.security.storage.localstorage;

import com.auth.practice.infrastructure.security.jwt.JwtAuthenticationFilter;
import com.auth.practice.infrastructure.security.jwt.JwtProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

// [패턴: localstorage] ⚠️ 학습 목적 — 운영 환경에서는 사용하지 마세요.
// Access Token을 Authorization: Bearer 헤더에서 읽는다.
// 클라이언트는 localStorage에 accessToken을 저장하여 매 요청마다 헤더에 담는다.
//
// [XSS 취약점]
// localStorage는 JS에서 직접 접근 가능하다 (document.cookie와 달리 httpOnly 없음).
// 악성 스크립트가 주입되면 localStorage.getItem('accessToken')으로 토큰 탈취 가능.
// 따라서 이 패턴은 XSS 방어가 없는 사이트에서는 심각한 보안 위협이 된다.
@Profile("localstorage")
@Component
public class LocalStorageJwtFilter extends JwtAuthenticationFilter {

    private final JwtProvider jwtProvider;

    public LocalStorageJwtFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractFromHeader(request);

        if (token != null && jwtProvider.validateToken(token)) {
            Long userId = jwtProvider.getUserId(token);
            String role = jwtProvider.getRole(token);
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            userId, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    )
            );
        }

        filterChain.doFilter(request, response);
    }

    private String extractFromHeader(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
