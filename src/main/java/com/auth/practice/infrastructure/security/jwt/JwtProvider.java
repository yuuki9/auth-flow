package com.auth.practice.infrastructure.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// [왜?] HMAC-SHA256(대칭키) 선택 이유:
//       단일 서버 또는 내부 신뢰 서비스 간 통신에서는 대칭키가 간단하고 충분히 안전하다.
//       RSA(비대칭키)는 외부 서비스에 공개키를 배포해야 할 때 사용 (예: OpenID Connect IdP 역할).
@Component
public class JwtProvider {

    private final SecretKey key;
    private final JwtProperties props;

    public JwtProvider(JwtProperties props) {
        // [보안] 키는 최소 256비트(32바이트) 이상이어야 HMAC-SHA256에 안전하다.
        //        짧은 키는 brute-force 공격에 취약.
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    // [현업패턴] Access Token에 userId와 role을 claim으로 포함.
    //            매 요청마다 DB 조회 없이 토큰만으로 사용자 식별 + 권한 확인 가능 → Stateless 달성.
    // [주의] 민감 정보(비밀번호, 개인정보)는 절대 claim에 넣지 않는다.
    //        JWT payload는 Base64 인코딩이므로 누구나 디코딩 가능 (암호화 아님).
    public String generateAccessToken(Long userId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .claim("type", "access")
                .issuedAt(now)
                // [왜?] Access Token TTL 15분: 탈취 시 공격자가 사용할 수 있는 시간을 최소화.
                //        너무 짧으면 Refresh 빈도가 늘어 UX 저하 → 15분이 현업 표준.
                .expiration(new Date(now.getTime() + props.getAccessTokenExpiryMs()))
                .signWith(key)
                .compact();
    }

    // [왜?] Refresh Token은 userId만 포함, role은 넣지 않는다.
    //        Refresh Token의 역할은 "새 Access Token 발급 권한 증명"뿐.
    //        role이 변경됐을 때 새 Access Token 발급 시 최신 role을 반영하기 위해서도 분리.
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", "refresh")
                .issuedAt(now)
                // [왜?] Refresh Token TTL 7일: 매일 로그인하지 않아도 되는 UX 제공.
                //        길수록 탈취 시 위험 기간이 길어지므로 RTR(Rotation)로 보완.
                .expiration(new Date(now.getTime() + props.getRefreshTokenExpiryMs()))
                .signWith(key)
                .compact();
    }

    // [보안] 검증 실패 시 false 반환 (예외를 밖으로 던지지 않음).
    //        만료·변조·서명 불일치 모두 동일하게 처리 → 공격자에게 실패 이유를 노출하지 않음.
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
