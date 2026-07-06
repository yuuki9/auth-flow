package com.auth.practice.domain.token;

import java.util.Optional;

// [멀티세션] 기존: save(userId, token, ttl) → "refresh:{userId}" key
//            변경: save(jti, userId, ttl)   → "refresh:{jti}" key
//
//            jti(JWT ID)를 key로 사용하면 같은 userId라도 로그인마다 독립적인 key가 생성되어
//            브라우저 A의 갱신이 브라우저 B 세션에 영향을 주지 않는다.
public interface RefreshTokenRepository {
    void save(String jti, Long userId, long ttlSeconds);
    Optional<Long> findUserIdByJti(String jti);
    void deleteByJti(String jti);
}
