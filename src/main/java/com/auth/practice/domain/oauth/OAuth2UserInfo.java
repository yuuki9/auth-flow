package com.auth.practice.domain.oauth;

/**
 * OAuth 2.0 Provider별 사용자 정보 통합 인터페이스
 * Provider마다 다른 응답 구조를 추상화
 */
public interface OAuth2UserInfo {
    
    /**
     * Provider에서 제공하는 고유 ID
     */
    String getProviderId();
    
    /**
     * Provider 종류
     */
    OAuth2Provider getProvider();
    
    /**
     * 이메일
     */
    String getEmail();
    
    /**
     * 이름
     */
    String getName();
    
    /**
     * 프로필 이미지 URL
     */
    String getProfileImageUrl();
}
