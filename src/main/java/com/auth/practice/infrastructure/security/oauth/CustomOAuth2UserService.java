package com.auth.practice.infrastructure.security.oauth;

import com.auth.practice.domain.oauth.OAuth2UserInfo;
import com.auth.practice.domain.user.User;
import com.auth.practice.domain.user.UserRepository;
import com.auth.practice.infrastructure.security.oauth.userinfo.OAuth2UserInfoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

// [왜?] DefaultOAuth2UserService를 extends하는 이유:
//        부모 클래스가 OAuth2 Provider(Google/Kakao/Naver)에서 사용자 정보를 가져오는 HTTP 통신을 담당.
//        loadUser()를 오버라이드해서 "Provider 응답 → DB upsert → CustomOAuth2User 반환" 로직만 추가.
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // [왜?] super.loadUser()가 Provider의 userinfo 엔드포인트를 호출해 사용자 속성을 가져온다.
        //        이 시점에서 OAuth2 인증은 완료된 상태 — 이후는 우리 DB와의 연동만 남음.
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        // [왜?] OAuth2UserInfoFactory: Provider마다 응답 구조가 다르다.
        //        Google은 "sub", Kakao는 "id", Naver는 "response.id"로 사용자 ID를 반환.
        //        Factory가 Provider별 파싱 차이를 추상화해 동일한 인터페이스로 접근 가능.
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.of(
                registrationId, oAuth2User.getAttributes());

        // [현업패턴] upsert: provider + providerId 기준으로 신규 INSERT / 기존 UPDATE.
        //            이메일 대신 providerId를 식별자로 사용하는 이유:
        //            사용자가 Provider에서 이메일을 변경해도 providerId는 불변 → 계정 연속성 보장.
        User user = userRepository.findByProviderAndProviderId(
                        oAuth2UserInfo.getProvider(), oAuth2UserInfo.getProviderId())
                .map(existing -> {
                    // [왜?] 로그인마다 이름·프로필 이미지를 Provider에서 최신값으로 동기화.
                    //        사용자가 Provider에서 프로필을 변경하면 다음 로그인 시 반영됨.
                    existing.update(oAuth2UserInfo.getName(), oAuth2UserInfo.getProfileImageUrl());
                    return userRepository.save(existing);
                })
                .orElseGet(() -> userRepository.save(User.create(
                        oAuth2UserInfo.getEmail(),
                        oAuth2UserInfo.getName(),
                        oAuth2UserInfo.getProvider(),
                        oAuth2UserInfo.getProviderId(),
                        oAuth2UserInfo.getProfileImageUrl()
                )));

        log.info("OAuth2 로그인 성공 - provider: {}, userId: {}, email: {}",
                oAuth2UserInfo.getProvider(), user.getId(), user.getEmail());

        // [왜?] CustomOAuth2User를 반환하는 이유:
        //        Spring Security가 OAuth2User를 Authentication의 principal로 저장한다.
        //        CustomOAuth2User에 userId를 포함해 SuccessHandler에서 바로 issueTokens()를 호출할 수 있도록.
        return new CustomOAuth2User(user.getId(), oAuth2UserInfo,
                oAuth2User.getAttributes(), userNameAttributeName);
    }
}
