package com.auth.practice.infrastructure.security.jwt;

import org.springframework.web.filter.OncePerRequestFilter;

// [학습] storage 패턴별 구현체(Cookie/Memory/LocalStorage)가 이 추상 클래스를 extends한다.
//        SecurityConfig는 이 타입으로 주입받으므로 패턴이 바뀌어도 SecurityConfig는 수정 불필요.
public abstract class JwtAuthenticationFilter extends OncePerRequestFilter {
}
