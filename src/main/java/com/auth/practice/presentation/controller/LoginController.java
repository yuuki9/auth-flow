package com.auth.practice.presentation.controller;

import com.auth.practice.application.auth.AuthService;
import com.auth.practice.domain.user.User;
import com.auth.practice.domain.user.UserRepository;
import com.auth.practice.presentation.dto.LoginRequest;
import com.auth.practice.presentation.dto.TokenResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// [왜?] base/jwt-only 전용 로그인 엔드포인트.
//        base/oauth2-foundation에서는 이 컨트롤러 대신 OAuth2 콜백이 로그인 진입점이 된다.
//        git diff base/jwt-only base/oauth2-foundation 에서 이 파일이 사라지는 것을 확인할 수 있다.
@RestController
@RequestMapping("/api/auth")
public class LoginController {

    private final AuthenticationManager authenticationManager;
    private final AuthService authService;
    private final UserRepository userRepository;

    public LoginController(AuthenticationManager authenticationManager,
                           AuthService authService,
                           UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.authService = authService;
        this.userRepository = userRepository;
    }

    // [현업패턴] AuthenticationManager를 통해 Spring Security 인증 흐름에 위임.
    //            내부적으로 LoginService.loadUserByUsername() → BCrypt 검증 순서로 실행된다.
    //            직접 BCrypt를 호출하지 않아 인증 로직을 표준화하고 테스트하기 쉽다.
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            // [왜?] auth.getName()에 userId(String)가 들어있다.
            //        LoginService.loadUserByUsername()에서 username 자리에 userId를 저장했기 때문.
            Long userId = Long.parseLong(auth.getName());
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalStateException("User not found"));

            TokenResponse tokens = authService.issueTokens(userId, user.getRole().name());
            return ResponseEntity.ok(tokens);

        } catch (AuthenticationException e) {
            // [보안] "이메일이 없다" / "비밀번호가 틀렸다"를 구분하지 않는다.
            //        구분하면 공격자가 유효한 이메일을 열거(User Enumeration)할 수 있다.
            return ResponseEntity.status(401).body(Map.of("error", "이메일 또는 비밀번호가 올바르지 않습니다"));
        }
    }
}
