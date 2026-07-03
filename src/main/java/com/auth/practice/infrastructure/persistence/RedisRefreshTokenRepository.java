package com.auth.practice.infrastructure.persistence;

import com.auth.practice.domain.token.RefreshTokenRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

// [왜?] Refresh Token 저장소로 Redis를 선택한 이유:
//        1. TTL 자동 관리: Redis key에 만료 시간을 설정하면 자동 삭제 → 별도 배치 작업 불필요.
//        2. 빠른 읽기: 토큰 갱신 요청마다 조회가 발생하므로 인메모리 DB가 적합.
//        3. 즉시 무효화: 로그아웃·탈취 감지 시 key 삭제 한 번으로 토큰 즉시 폐기 가능.
@Repository
public class RedisRefreshTokenRepository implements RefreshTokenRepository {

    // [왜?] "refresh:{userId}" 형태의 key 설계:
    //        userId를 key로 사용하면 사용자당 하나의 Refresh Token만 유지.
    //        여러 기기 지원이 필요하면 "refresh:{userId}:{deviceId}" 형태로 확장 가능.
    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    public RedisRefreshTokenRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // [현업패턴] TTL을 Refresh Token 만료 시간과 동일하게 설정.
    //            Redis TTL이 먼저 만료되면 유효한 토큰이 사라지는 문제가 생기므로 일치시킨다.
    @Override
    public void save(Long userId, String token, long ttlSeconds) {
        redisTemplate.opsForValue().set(KEY_PREFIX + userId, token, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Optional<String> findByUserId(Long userId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + userId));
    }

    @Override
    public void deleteByUserId(Long userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }
}
