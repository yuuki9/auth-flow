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

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.of(
                registrationId, oAuth2User.getAttributes());

        // [현업패턴] upsert: provider + providerId 기준으로 신규 INSERT / 기존 UPDATE.
        User user = userRepository.findByProviderAndProviderId(
                        oAuth2UserInfo.getProvider(), oAuth2UserInfo.getProviderId())
                .map(existing -> {
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

        return new CustomOAuth2User(user.getId(), oAuth2UserInfo,
                oAuth2User.getAttributes(), userNameAttributeName);
    }
}
