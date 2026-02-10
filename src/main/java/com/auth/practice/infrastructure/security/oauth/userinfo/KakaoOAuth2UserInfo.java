package com.auth.practice.infrastructure.security.oauth.userinfo;

import com.auth.practice.domain.oauth.OAuth2Provider;
import com.auth.practice.domain.oauth.OAuth2UserInfo;

import java.util.Map;

/**
 * Kakao OAuth 2.0 UserInfo 구현체
 * 
 * Kakao 응답 예시:
 * {
 *   "id": 1234567890,
 *   "kakao_account": {
 *     "email": "user@kakao.com",
 *     "profile": {
 *       "nickname": "홍길동",
 *       "profile_image_url": "https://k.kakaocdn.net/..."
 *     }
 *   }
 * }
 */
public class KakaoOAuth2UserInfo implements OAuth2UserInfo {
    
    private final Map<String, Object> attributes;
    
    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
    
    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
    }
    
    @Override
    public OAuth2Provider getProvider() {
        return OAuth2Provider.KAKAO;
    }
    
    @Override
    public String getEmail() {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        if (kakaoAccount == null) {
            return null;
        }
        return (String) kakaoAccount.get("email");
    }
    
    @Override
    public String getName() {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        if (kakaoAccount == null) {
            return null;
        }
        
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        if (profile == null) {
            return null;
        }
        
        return (String) profile.get("nickname");
    }
    
    @Override
    public String getProfileImageUrl() {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        if (kakaoAccount == null) {
            return null;
        }
        
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        if (profile == null) {
            return null;
        }
        
        return (String) profile.get("profile_image_url");
    }
}
