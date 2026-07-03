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

    // [왜?] 로그아웃 시 서버는 Redis의 Refresh Token만 삭제한다.
    //        Access Token은 Stateless이므로 서버에서 직접 무효화할 수 없다.
    //        클라이언트가 Access Token을 즉시 폐기(메모리/localStorage에서 삭제)해야 한다.
    // [주의] 클라이언트가 Access Token을 폐기하지 않으면 TTL(15분)이 남아있는 동안 계속 유효.
    //        이것이 Access Token TTL을 짧게 유지하는 핵심 이유다.
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal Long userId) {
        authService.logout(userId);
        return ResponseEntity.ok(Map.of("message", "로그아웃 완료"));
    }
}
