package com.auth.practice.infrastructure.security.oauth.userinfo;

import com.auth.practice.domain.oauth.OAuth2Provider;
import com.auth.practice.domain.oauth.OAuth2UserInfo;

import java.util.Map;

/**
 * Google OAuth 2.0 UserInfo 구현체
 * 
 * Google 응답 예시:
 * {
 *   "sub": "1234567890",
 *   "email": "user@gmail.com",
 *   "name": "홍길동",
 *   "picture": "https://lh3.googleusercontent.com/..."
 * }
 */
public class GoogleOAuth2UserInfo implements OAuth2UserInfo {
    
    private final Map<String, Object> attributes;
    
    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
    
    @Override
    public String getProviderId() {
        return (String) attributes.get("sub");
    }
    
    @Override
    public OAuth2Provider getProvider() {
        return OAuth2Provider.GOOGLE;
    }
    
    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }
    
    @Override
    public String getName() {
        return (String) attributes.get("name");
    }
    
    @Override
    public String getProfileImageUrl() {
        return (String) attributes.get("picture");
    }
}
