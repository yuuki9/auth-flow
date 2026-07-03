package com.auth.practice.infrastructure.security.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

// [현업패턴] base 브랜치: OAuth 로그인 성공 후 userId만 JSON 반환 (JWT 미포함).
//           JWT 발급은 pattern/oauth-* 브랜치에서 AuthService 호출로 추가된다.
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();

        log.info("OAuth2 로그인 성공 - userId: {}, provider: {}",
                principal.getUserId(),
                principal.getOAuth2UserInfo().getProvider());

        // [TODO] pattern/oauth-* 브랜치에서 JWT 발급 후 토큰 전달 방식 추가
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "userId", principal.getUserId(),
                "name", principal.getUserName(),
                "email", principal.getEmail()
        )));
    }
}
