package com.auth.practice.infrastructure.security.storage.memory;

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

// [패턴: memory]
// Access Token을 Authorization: Bearer 헤더에서 읽는다.
// 클라이언트는 로그인 후 응답 body의 accessToken을 JS 변수에 저장.
// 탭/창 닫으면 자동 소멸 → 영구 저장 없음.
@Profile("memory")
@Component
public class MemoryJwtFilter extends JwtAuthenticationFilter {

    private final JwtProvider jwtProvider;

    public MemoryJwtFilter(JwtProvider jwtProvider) {
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
