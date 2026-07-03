package com.auth.practice.presentation.controller;

import com.auth.practice.application.auth.AuthService;
import com.auth.practice.domain.user.User;
import com.auth.practice.domain.user.UserRepository;
import com.auth.practice.infrastructure.security.storage.RefreshTokenHandler;
import com.auth.practice.presentation.dto.TokenResponse;
import com.auth.practice.presentation.dto.UserInfoResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final RefreshTokenHandler refreshTokenHandler;

    public AuthController(AuthService authService, UserRepository userRepository,
                          RefreshTokenHandler refreshTokenHandler) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.refreshTokenHandler = refreshTokenHandler;
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> me(@AuthenticationPrincipal Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
        return ResponseEntity.ok(UserInfoResponse.from(user));
    }

    // [학습] 패턴별로 달라지는 부분은 RefreshTokenHandler가 처리.
    //        cookie   → 쿠키에서 읽고, 새 토큰을 쿠키에 씀.
    //        memory   → 쿠키에서 읽고, 새 accessToken만 body로 반환.
    //        localstorage → body에서 읽고, 두 토큰 모두 body로 반환.
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            HttpServletRequest request,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletResponse response) {
        String bodyToken = body != null ? body.get("refreshToken") : null;
        String refreshToken = refreshTokenHandler.extractRefreshToken(request, bodyToken);
        if (refreshToken == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Refresh Token 없음"));
        }
        try {
            TokenResponse tokens = authService.refresh(refreshToken);
            Map<String, Object> responseBody = refreshTokenHandler.buildRefreshResponse(response, tokens);
            return ResponseEntity.ok(responseBody);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @AuthenticationPrincipal Long userId,
            HttpServletResponse response) {
        authService.logout(userId);
        refreshTokenHandler.clearOnLogout(response);
        return ResponseEntity.ok(Map.of("message", "로그아웃 완료"));
    }
}
