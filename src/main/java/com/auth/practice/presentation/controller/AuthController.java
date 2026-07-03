package com.auth.practice.presentation.controller;

import com.auth.practice.domain.user.User;
import com.auth.practice.domain.user.UserRepository;
import com.auth.practice.infrastructure.security.oauth.CustomOAuth2User;
import com.auth.practice.presentation.dto.UserInfoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // [왜?] base 브랜치: JWT 필터 없이 OAuth 세션 기반 /me (pattern 추가 전 참고용).
    //        pattern/oauth-* 에서 JwtAuthenticationFilter 추가 후 Long userId로 교체된다.
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> me(
            @AuthenticationPrincipal CustomOAuth2User principal) {
        User user = userRepository.findByProviderAndProviderId(
                        principal.getOAuth2UserInfo().getProvider(),
                        principal.getOAuth2UserInfo().getProviderId())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        return ResponseEntity.ok(UserInfoResponse.from(user));
    }
}
