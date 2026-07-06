package com.auth.practice.presentation.controller;

import com.auth.practice.application.auth.AuthService;
import com.auth.practice.domain.user.User;
import com.auth.practice.domain.user.UserRepository;
import com.auth.practice.presentation.dto.TokenResponse;
import com.auth.practice.presentation.dto.UserInfoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    // [왜?] @AuthenticationPrincipal Long userId:
    //        JwtAuthenticationFilter가 SecurityContext에 userId(Long)를 principal로 설정했기 때문에
    //        컨트롤러에서 바로 주입받을 수 있다. DB 조회나 세션 없이 즉시 사용자 식별 가능.
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> me(@AuthenticationPrincipal Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
        return ResponseEntity.ok(UserInfoResponse.from(user));
    }

    // [현업패턴] Refresh Token을 request body로 받는다 (jwt-only 패턴).
    //            base/oauth2-foundation의 cookie 패턴에서는 @CookieValue로 읽는다.
    //            두 브랜치의 이 메서드를 비교하면 저장 패턴의 차이가 명확히 보인다.
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "refreshToken 필드 없음"));
        }
        try {
            // [보안] AuthService.refresh() 내부에서 RTR(Rotation) 수행.
            //        유효하지 않거나 재사용된 토큰이면 IllegalArgumentException 발생.
            TokenResponse tokens = authService.refresh(refreshToken);
            return ResponseEntity.ok(tokens);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    // [멀티세션] 로그아웃 시 Refresh Token을 body로 받아 해당 세션(jti)만 삭제한다.
    //            기존 단일 세션에서는 userId로 삭제했으나, jti 기반에서는 특정 세션만 무효화 가능.
    //            다른 브라우저의 세션은 유지된다.
    // [왜?] Access Token은 Stateless이므로 서버에서 직접 무효화할 수 없다.
    //        클라이언트가 Access Token을 즉시 폐기해야 하며, TTL(15분) 내에는 유효하다.
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "refreshToken 필드 없음"));
        }
        authService.logout(refreshToken);
        return ResponseEntity.ok(Map.of("message", "로그아웃 완료"));
    }
}
