package com.auth.practice.infrastructure.security.oauth.userinfo;

import com.auth.practice.domain.oauth.OAuth2Provider;
import com.auth.practice.domain.oauth.OAuth2UserInfo;

import java.util.Map;

/**
 * Naver OAuth 2.0 UserInfo 구현체
 * 
 * Naver 응답 예시:
 * {
 *   "resultcode": "00",
 *   "message": "success",
 *   "response": {
 *     "id": "1234567890",
 *     "email": "user@naver.com",
 *     "name": "홍길동",
 *     "profile_image": "https://phinf.pstatic.net/..."
 *   }
 * }
 */
public class NaverOAuth2UserInfo implements OAuth2UserInfo {
    
    private final Map<String, Object> attributes;
    
    public NaverOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
    
    @Override
    public String getProviderId() {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        if (response == null) {
            return null;
        }
        return (String) response.get("id");
    }
    
    @Override
    public OAuth2Provider getProvider() {
        return OAuth2Provider.NAVER;
    }
    
    @Override
    public String getEmail() {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        if (response == null) {
            return null;
        }
        return (String) response.get("email");
    }
    
    @Override
    public String getName() {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        if (response == null) {
            return null;
        }
        return (String) response.get("name");
    }
    
    @Override
    public String getProfileImageUrl() {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        if (response == null) {
            return null;
        }
        return (String) response.get("profile_image");
    }
}
