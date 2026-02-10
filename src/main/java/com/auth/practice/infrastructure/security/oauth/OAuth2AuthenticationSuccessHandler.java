package com.auth.practice.infrastructure.security.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * OAuth 2.0 로그인 성공 핸들러
 * - 로그인 성공 후 사용자 정보 추출
 * - 홈 페이지로 리다이렉트
 */
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        // CustomOAuth2User 정보 추출
        CustomOAuth2User customOAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        
        // 사용자 정보 로깅 (디버깅용)
        log.info("OAuth2 로그인 성공!");
        log.info("Provider: {}", customOAuth2User.getOAuth2UserInfo().getProvider());
        log.info("Provider ID: {}", customOAuth2User.getOAuth2UserInfo().getProviderId());
        log.info("사용자 이름: {}", customOAuth2User.getUserName());
        log.info("사용자 이메일: {}", customOAuth2User.getEmail());
        log.info("프로필 이미지: {}", customOAuth2User.getProfileImageUrl());
        
        // 홈 페이지로 리다이렉트
        getRedirectStrategy().sendRedirect(request, response, "/home");
    }
}
