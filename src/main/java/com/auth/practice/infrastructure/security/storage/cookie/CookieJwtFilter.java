package com.auth.practice.infrastructure.security.storage.cookie;

import com.auth.practice.infrastructure.security.jwt.JwtAuthenticationFilter;
import com.auth.practice.infrastructure.security.jwt.JwtProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

// [패턴: cookie]
// Access Token을 HttpOnly 쿠키에서 읽는다.
// JS에서 토큰에 직접 접근 불가 → XSS 방어. CSRF 방어는 SameSite=Strict로 처리.
@Profile("cookie")
@Component
public class CookieJwtFilter extends JwtAuthenticationFilter {

    private final JwtProvider jwtProvider;

    public CookieJwtFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractFromCookie(request, "access_token");

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

    private String extractFromCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
