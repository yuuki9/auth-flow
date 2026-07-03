package com.auth.practice.infrastructure.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// [현업패턴] Stateless 인증 필터.
//            세션 없이 매 요청마다 JWT를 검증하여 SecurityContext를 설정한다.
//            서버 재시작·스케일아웃 시에도 토큰만 유효하면 인증 유지 → 수평 확장에 유리.
//
// [왜?] OncePerRequestFilter: 포워드·인클루드 등 내부 디스패치에서 필터가 중복 실행되지 않도록 보장.
// [왜?] UsernamePasswordAuthenticationFilter 앞에 배치: 폼 로그인 처리 전에 JWT 검증을 먼저 수행.
//        JWT가 유효하면 Spring Security가 이미 인증된 상태로 인식 → 이후 필터 불필요.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    public JwtAuthenticationFilter(JwtProvider jwtProvider) {
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

            // [왜?] credentials 자리에 null을 넣는다.
            //        JWT 기반 인증에서는 비밀번호가 필요 없음.
            //        이미 서명 검증으로 신원이 확인됐으므로 credentials는 의미 없다.
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            userId, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    )
            );
        }

        // [보안] 토큰이 없거나 유효하지 않으면 SecurityContext를 설정하지 않고 다음 필터로 넘긴다.
        //        ExceptionTranslationFilter가 인증 없는 요청에 대해 401을 반환.
        filterChain.doFilter(request, response);
    }

    // [왜?] "Bearer " 접두어를 파싱하는 이유:
    //        RFC 6750 표준에서 Bearer 토큰 전달 방식을 "Authorization: Bearer <token>"으로 정의.
    //        접두어 확인 없이 파싱하면 다른 인증 방식(Basic, Digest 등)과 충돌 가능.
    private String extractFromHeader(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
