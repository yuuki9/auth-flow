package com.auth.practice.infrastructure.security.oauth.userinfo;

import com.auth.practice.domain.oauth.OAuth2Provider;
import com.auth.practice.domain.oauth.OAuth2UserInfo;

import java.util.Map;

/**
 * OAuth2UserInfo Factory
 * Provider별로 적절한 OAuth2UserInfo 구현체를 생성
 */
public class OAuth2UserInfoFactory {
    
    /**
     * registrationId에 따라 적절한 OAuth2UserInfo 생성
     * 
     * @param registrationId OAuth2 Provider ID (google, kakao, naver)
     * @param attributes Provider로부터 받은 사용자 정보
     * @return OAuth2UserInfo 구현체
     */
    public static OAuth2UserInfo of(String registrationId, Map<String, Object> attributes) {
        OAuth2Provider provider = OAuth2Provider.fromRegistrationId(registrationId);
        
        return switch (provider) {
            case GOOGLE -> new GoogleOAuth2UserInfo(attributes);
            case KAKAO -> new KakaoOAuth2UserInfo(attributes);
            case NAVER -> new NaverOAuth2UserInfo(attributes);
        };
    }
}
