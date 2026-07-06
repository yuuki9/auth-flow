package com.auth.practice.infrastructure.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

// [왜?] HMAC-SHA256(대칭키) 선택 이유:
//       단일 서버 또는 내부 신뢰 서비스 간 통신에서는 대칭키가 간단하고 충분히 안전하다.
//       RSA(비대칭키)는 외부 서비스에 공개키를 배포해야 할 때 사용 (예: OpenID Connect IdP 역할).
@Component
public class JwtProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtProvider.class);

    private final SecretKey key;
    private final long accessTokenExpiryMs;
    private final long refreshTokenExpiryMs;

    public JwtProvider(JwtProperties properties) {
        // [보안] 키는 최소 256비트(32바이트) 이상이어야 HMAC-SHA256에 안전하다.
        //        짧은 키는 brute-force 공격에 취약.
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiryMs = properties.getAccessTokenExpiryMs();
        this.refreshTokenExpiryMs = properties.getRefreshTokenExpiryMs();
    }

    // [현업패턴] Access Token에 userId와 role을 claim으로 포함.
    //            매 요청마다 DB 조회 없이 토큰만으로 사용자 식별 + 권한 확인 가능 → Stateless 달성.
    // [주의] 민감 정보(비밀번호, 개인정보)는 절대 claim에 넣지 않는다.
    //        JWT payload는 Base64 인코딩이므로 누구나 디코딩 가능 (암호화 아님).
    public String generateAccessToken(Long userId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(now)
                // [왜?] Access Token TTL 15분: 탈취 시 공격자가 사용할 수 있는 시간을 최소화.
                //        너무 짧으면 Refresh 빈도가 늘어 UX 저하 → 15분이 현업 표준.
                .expiration(new Date(now.getTime() + accessTokenExpiryMs))
                .signWith(key)
                .compact();
    }

    // [왜?] Refresh Token은 userId만 포함, role은 넣지 않는다.
    //        Refresh Token의 역할은 "새 Access Token 발급 권한 증명"뿐.
    //        role이 변경됐을 때 새 Access Token 발급 시 최신 role을 반영하기 위해서도 분리.
    // [멀티세션] .id(UUID): jti(JWT ID) 클레임. 토큰마다 고유 식별자를 부여한다.
    //            같은 userId라도 로그인마다 다른 jti → Redis key를 "refresh:{jti}"로 설계하면
    //            세션(기기)별로 독립적인 Refresh Token 관리가 가능해진다.
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                // [왜?] Refresh Token TTL 7일: 매일 로그인하지 않아도 되는 UX 제공.
                //        길수록 탈취 시 위험 기간이 길어지므로 RTR(Rotation)로 보완.
                .expiration(new Date(now.getTime() + refreshTokenExpiryMs))
                .signWith(key)
                .compact();
    }

    // [멀티세션] Refresh Token payload에서 jti를 꺼낸다.
    //            Redis 조회·삭제 시 userId 대신 jti를 key로 사용하므로 반드시 필요.
    public String getJti(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload().getId();
    }

    // [보안] 검증 실패 시 false 반환 (예외를 밖으로 던지지 않음).
    //        만료·변조·서명 불일치 모두 동일하게 처리 → 공격자에게 실패 이유를 노출하지 않음.
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("만료된 JWT: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("유효하지 않은 JWT: {}", e.getMessage());
        }
        return false;
    }

    public Long getUserId(String token) {
        return Long.parseLong(
                Jwts.parser().verifyWith(key).build()
                        .parseSignedClaims(token).getPayload().getSubject()
        );
    }

    public String getRole(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload().get("role", String.class);
    }
}
