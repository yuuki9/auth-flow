package com.auth.practice.infrastructure.security.oauth;

import com.auth.practice.domain.oauth.OAuth2UserInfo;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Custom OAuth2User
 * Spring Security의 OAuth2User를 확장하여 통합된 사용자 정보 제공
 */
public class CustomOAuth2User implements OAuth2User {
    
    private final OAuth2UserInfo oAuth2UserInfo;
    private final Map<String, Object> attributes;
    private final String nameAttributeKey;
    
    public CustomOAuth2User(
            OAuth2UserInfo oAuth2UserInfo,
            Map<String, Object> attributes,
            String nameAttributeKey) {
        this.oAuth2UserInfo = oAuth2UserInfo;
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
    }
    
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 기본 권한: ROLE_USER
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }
    
    @Override
    public String getName() {
        // Provider별 고유 ID 반환
        return oAuth2UserInfo.getProviderId();
    }
    
    /**
     * OAuth2UserInfo 반환 (통합된 사용자 정보)
     */
    public OAuth2UserInfo getOAuth2UserInfo() {
        return oAuth2UserInfo;
    }
    
    /**
     * 이메일 반환
     */
    public String getEmail() {
        return oAuth2UserInfo.getEmail();
    }
    
    /**
     * 사용자 이름 반환
     */
    public String getUserName() {
        return oAuth2UserInfo.getName();
    }
    
    /**
     * 프로필 이미지 URL 반환
     */
    public String getProfileImageUrl() {
        return oAuth2UserInfo.getProfileImageUrl();
    }
}
