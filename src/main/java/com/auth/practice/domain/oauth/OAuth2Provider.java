package com.auth.practice.domain.oauth;

/**
 * OAuth 2.0 Provider 열거형
 * 지원하는 소셜 로그인 제공자 정의
 */
public enum OAuth2Provider {
    GOOGLE("google"),
    KAKAO("kakao"),
    NAVER("naver");

    private final String registrationId;

    OAuth2Provider(String registrationId) {
        this.registrationId = registrationId;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    /**
     * registrationId로 Provider 찾기
     */
    public static OAuth2Provider fromRegistrationId(String registrationId) {
        for (OAuth2Provider provider : values()) {
            if (provider.registrationId.equalsIgnoreCase(registrationId)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unknown provider: " + registrationId);
    }
}
