package com.auth.practice.application.auth;

import com.auth.practice.domain.token.RefreshTokenRepository;
import com.auth.practice.infrastructure.security.jwt.JwtProperties;
import com.auth.practice.infrastructure.security.jwt.JwtProvider;
import com.auth.practice.presentation.dto.TokenResponse;
import org.springframework.stereotype.Service;

// [왜?] 두 트랙(jwt-only, oauth2-foundation)의 로그인 결과를 동일한 방식으로 처리하는 오케스트레이터.
//       로그인 방식(ID/PW vs OAuth2)이 달라도 "토큰 발급 → Redis 저장" 로직은 동일하다.
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
    // [멀티세션] jti를 Redis key로 사용 → 같은 userId로 발급해도 세션마다 독립적인 key.
    //            브라우저 A 로그인: Redis["refresh:jti-aaa"] = userId
    //            브라우저 B 로그인: Redis["refresh:jti-bbb"] = userId  ← 덮어쓰지 않음
    public TokenResponse issueTokens(Long userId, String role) {
        String accessToken = jwtProvider.generateAccessToken(userId, role);
        String refreshToken = jwtProvider.generateRefreshToken(userId);
        String jti = jwtProvider.getJti(refreshToken);

        long ttlSeconds = jwtProperties.getRefreshTokenExpiryMs() / 1000;
        tokenRepository.save(jti, userId, ttlSeconds);

        return new TokenResponse(accessToken, refreshToken, jwtProperties.getAccessTokenExpiryMs());
    }

    // [보안] Refresh Token Rotation (RTR):
    //        갱신마다 기존 Refresh Token을 폐기하고 새 토큰을 발급한다.
    //        탈취된 토큰이 재사용되면 Redis에 해당 jti가 없음 → 즉시 감지.
    // [멀티세션] jti 단위로 삭제하므로 갱신한 세션(브라우저 A)만 새 토큰을 받고,
    //            다른 세션(브라우저 B)의 Redis key는 그대로 유지된다.
    public TokenResponse refresh(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token");
        }

        String jti = jwtProvider.getJti(refreshToken);

        Long userId = tokenRepository.findUserIdByJti(jti)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Refresh Token이 Redis에 없음 (만료·로그아웃·탈취 감지)"));

        // [왜?] 기존 jti 먼저 삭제 후 새 토큰(새 jti) 발급.
        //        순서가 바뀌면 갱신 중 서버 장애 시 두 토큰이 모두 유효한 상태가 될 수 있다.
        tokenRepository.deleteByJti(jti);
        return issueTokens(userId, "USER");
    }

    // [멀티세션] 특정 세션 로그아웃: 클라이언트가 보낸 Refresh Token의 jti만 삭제.
    //            다른 브라우저의 세션은 영향받지 않는다.
    // [현업패턴] Access Token은 Stateless이므로 서버가 직접 무효화할 수 없다.
    //            클라이언트가 Access Token을 즉시 폐기해야 하며, TTL(15분) 내에는 유효하다.
    public void logout(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            return;
        }
        String jti = jwtProvider.getJti(refreshToken);
        tokenRepository.deleteByJti(jti);
    }
}
