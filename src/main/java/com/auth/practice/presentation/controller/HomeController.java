package com.auth.practice.presentation.controller;

import com.auth.practice.infrastructure.security.oauth.CustomOAuth2User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 홈 컨트롤러
 * - 메인 페이지, 로그인 페이지, 홈 페이지 처리
 */
@Controller
public class HomeController {

    /**
     * 메인 페이지 (인증 없이 접근 가능)
     */
    @GetMapping("/")
    public String index(Authentication authentication) {
        // 이미 로그인한 경우 홈으로 리다이렉트
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/home";
        }
        return "index";
    }

    /**
     * 로그인 페이지
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * 홈 페이지 (로그인 후 접근 가능)
     */
    @GetMapping("/home")
    public String home(@AuthenticationPrincipal CustomOAuth2User principal, Model model) {
        // 통합된 사용자 정보를 모델에 추가
        model.addAttribute("provider", principal.getOAuth2UserInfo().getProvider().name());
        model.addAttribute("name", principal.getUserName());
        model.addAttribute("email", principal.getEmail());
        model.addAttribute("picture", principal.getProfileImageUrl());
        
        return "home";
    }
}
