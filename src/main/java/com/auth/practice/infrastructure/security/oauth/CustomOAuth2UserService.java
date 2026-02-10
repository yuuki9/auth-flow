package com.auth.practice.infrastructure.security.oauth;

import com.auth.practice.domain.oauth.OAuth2UserInfo;
import com.auth.practice.infrastructure.security.oauth.userinfo.OAuth2UserInfoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * Custom OAuth2UserService
 * OAuth2 로그인 시 Provider별 사용자 정보를 통합 처리
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    
    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);
    
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. 기본 OAuth2UserService로 사용자 정보 로드
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        // 2. registrationId 추출 (google, kakao, naver)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        
        // 3. userNameAttributeName 추출 (Provider별로 다름)
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();
        
        log.info("OAuth2 Login - Provider: {}, UserNameAttribute: {}", 
                registrationId, userNameAttributeName);
        
        // 4. Provider별 사용자 정보 통합
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.of(
                registrationId, 
                oAuth2User.getAttributes()
        );
        
        // 5. 로그 출력 (디버깅용)
        log.info("OAuth2 User Info - Provider: {}, Email: {}, Name: {}", 
                oAuth2UserInfo.getProvider(),
                oAuth2UserInfo.getEmail(),
                oAuth2UserInfo.getName());
        
        // 6. CustomOAuth2User 반환 (Stage 3에서 DB 저장 로직 추가 예정)
        return new CustomOAuth2User(oAuth2UserInfo, oAuth2User.getAttributes(), userNameAttributeName);
    }
}
