package com.auth.practice.application.auth;

import com.auth.practice.domain.token.RefreshTokenRepository;
import com.auth.practice.infrastructure.security.jwt.JwtProperties;
import com.auth.practice.infrastructure.security.jwt.JwtProvider;
import com.auth.practice.presentation.dto.TokenResponse;
import org.springframework.stereotype.Service;

// [왜?] 두 트랙(jwt-only, oauth2-foundation)의 로그인 결과를 동일한 방식으로 처리하는 오케스트레이터.
//       로그인 방식(ID/PW vs OAuth2)이 달라도 "토큰 발급 → Redis 저장" 로직은 완전히 동일하다.
//       git diff base/jwt-only base/oauth2-foundation 에서 이 파일이 동일함을 확인할 수 있다.
@Service
public class AuthService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository tokenRepository;
    private final JwtProperties jwtProperties;

    public AuthService(JwtProvider jwtProvider,
                       RefreshTokenRepository tokenRepository,
                       JwtProperties jwtProperties) {
        this.jwtProvider = jwtProvider;
        this.tokenRepository = tokenRepository;
        this.jwtProperties = jwtProperties;
    }

    // [현업패턴] Access Token(단명) + Refresh Token(장명) 이중 발급.
    //            Access Token은 짧게(15분) → 탈취 피해 최소화.
    //            Refresh Token은 길게(7일) → 자동 재발급으로 UX 유지.
    public TokenResponse issueTokens(Long userId, String role) {
        String accessToken = jwtProvider.generateAccessToken(userId, role);
        String refreshToken = jwtProvider.generateRefreshToken(userId);

        // [왜?] Refresh Token을 Redis에 저장하는 이유:
        //       - TTL 자동 만료: 별도 배치 작업 없이 Refresh Token이 시간이 지나면 자동 삭제.
        //       - 즉시 무효화: 로그아웃/탈취 감지 시 Redis key를 삭제하면 즉시 폐기 가능.
        //       - DB 부하 없음: 고빈도 토큰 조회를 인메모리로 처리.
        long ttlSeconds = jwtProperties.getRefreshTokenExpiryMs() / 1000;
        tokenRepository.save(userId, refreshToken, ttlSeconds);

        return new TokenResponse(accessToken, refreshToken, jwtProperties.getAccessTokenExpiryMs());
    }

    // [보안] Refresh Token Rotation (RTR):
    //        갱신마다 기존 Refresh Token을 폐기하고 새 토큰을 발급한다.
    //        탈취된 토큰이 재사용되면 Redis의 값과 불일치 → 즉시 감지.
    //        감지 시 Redis에서 해당 userId의 토큰을 전부 삭제 → 진짜 사용자도 재로그인 필요.
    //        (피해를 최소화하기 위한 fail-safe 설계)
    public TokenResponse refresh(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token");
        }
        Long userId = jwtProvider.getUserId(refreshToken);
        String stored = tokenRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Refresh Token이 Redis에 없음 (로그아웃 또는 탈취 감지)"));

        // [보안] 저장된 값과 불일치 = 이미 사용된 토큰으로 재시도 → 탈취 의심.
        //        즉시 Redis 삭제로 해당 계정의 모든 Refresh Token을 무효화한다.
        if (!stored.equals(refreshToken)) {
            tokenRepository.deleteByUserId(userId);
            throw new IllegalArgumentException("Refresh Token 재사용 감지 — 보안 위협");
        }

        // [왜?] 검증 후 기존 토큰 먼저 삭제, 그 다음 새 토큰 발급.
        //        순서가 바뀌면 갱신 중 서버 장애 시 두 토큰이 모두 유효한 상태가 될 수 있다.
        tokenRepository.deleteByUserId(userId);
        return issueTokens(userId, "USER");
    }

    // [현업패턴] 로그아웃은 서버에서 Redis의 Refresh Token만 삭제.
    //            Access Token은 Stateless이므로 서버가 직접 무효화할 수 없다.
    //            클라이언트가 Access Token을 즉시 폐기(쿠키/메모리/localStorage에서 삭제)해야 한다.
    public void logout(Long userId) {
        tokenRepository.deleteByUserId(userId);
    }
}
