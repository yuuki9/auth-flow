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

// [왜?] 이 컨트롤러는 jwt-only와 oauth2-foundation 양쪽에서 동일하게 사용된다.
//        패턴별로 달라지는 토큰 처리(쿠키/바디/헤더)는 RefreshTokenHandler 인터페이스로 분리.
//        spring.profiles.active 값에 따라 Spring이 알맞은 구현체를 자동 주입.
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

    // [왜?] @AuthenticationPrincipal Long userId:
    //        JwtAuthenticationFilter(각 패턴별 구현체)가 SecurityContext에 userId(Long)를 설정.
    //        DB 조회나 세션 없이 컨트롤러에서 즉시 사용자 식별 가능 → Stateless 달성.
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> me(@AuthenticationPrincipal Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
        return ResponseEntity.ok(UserInfoResponse.from(user));
    }

    // [학습] 패턴마다 달라지는 부분을 RefreshTokenHandler가 처리:
    //        cookie      → 쿠키에서 읽고, 새 토큰을 쿠키에 쓴다.
    //        memory      → 쿠키에서 읽고, 새 accessToken만 body로 반환한다.
    //        localstorage → body에서 읽고, 두 토큰 모두 body로 반환한다.
    //
    // [보안] AuthService.refresh() 내부에서 RTR(Rotation) 수행.
    //        유효하지 않거나 재사용된 토큰이면 IllegalArgumentException → 401 반환.
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

    // [왜?] 로그아웃 시 서버는 Redis의 Refresh Token만 삭제.
    //        Access Token은 Stateless이므로 서버에서 직접 무효화할 수 없다.
    //        clearOnLogout()이 패턴별로 쿠키 삭제 등 클라이언트 측 정리를 돕는다.
    // [주의] 클라이언트가 Access Token을 폐기하지 않으면 TTL(15분)이 남아있는 동안 계속 유효.
    //        이것이 Access Token TTL을 짧게 유지하는 핵심 이유다.
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @AuthenticationPrincipal Long userId,
            HttpServletResponse response) {
        authService.logout(userId);
        refreshTokenHandler.clearOnLogout(response);
        return ResponseEntity.ok(Map.of("message", "로그아웃 완료"));
    }
}
