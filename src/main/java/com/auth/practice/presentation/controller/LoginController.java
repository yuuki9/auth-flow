package com.auth.practice.presentation.controller;

import com.auth.practice.application.auth.AuthService;
import com.auth.practice.presentation.dto.LoginRequest;
import com.auth.practice.presentation.dto.TokenResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// [왜?] base/jwt-only 전용 로그인 엔드포인트.
//        base/oauth2-foundation에서는 이 컨트롤러 대신 OAuth2 콜백이 로그인 진입점이 된다.
@RestController
@RequestMapping("/api/auth")
public class LoginController {

    private final AuthService authService;

    public LoginController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            TokenResponse tokens = authService.login(request.email(), request.password());
            return ResponseEntity.ok(tokens);
        } catch (AuthenticationException e) {
            // [보안] "이메일이 없다" / "비밀번호가 틀렸다"를 구분하지 않는다.
            return ResponseEntity.status(401).body(Map.of("error", "이메일 또는 비밀번호가 올바르지 않습니다"));
        }
    }
}
