package com.auth.practice.infrastructure.security.oauth;

import com.auth.practice.domain.oauth.OAuth2UserInfo;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {

    private final Long userId;
    private final OAuth2UserInfo oAuth2UserInfo;
    private final Map<String, Object> attributes;
    private final String userNameAttributeName;

    public CustomOAuth2User(Long userId, OAuth2UserInfo oAuth2UserInfo,
                            Map<String, Object> attributes, String userNameAttributeName) {
        this.userId = userId;
        this.oAuth2UserInfo = oAuth2UserInfo;
        this.attributes = attributes;
        this.userNameAttributeName = userNameAttributeName;
    }

    @Override
    public Map<String, Object> getAttributes() { return attributes; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getName() { return String.valueOf(attributes.get(userNameAttributeName)); }

    public Long getUserId() { return userId; }
    public OAuth2UserInfo getOAuth2UserInfo() { return oAuth2UserInfo; }
    public String getUserName() { return oAuth2UserInfo.getName(); }
    public String getEmail() { return oAuth2UserInfo.getEmail(); }
    public String getProfileImageUrl() { return oAuth2UserInfo.getProfileImageUrl(); }
}
