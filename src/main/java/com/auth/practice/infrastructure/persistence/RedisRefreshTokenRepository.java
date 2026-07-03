package com.auth.practice.infrastructure.persistence;

import com.auth.practice.domain.token.RefreshTokenRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

// [왜?] pattern/oauth-* 브랜치에서 JWT Refresh Token 저장에 사용한다.
//       base 브랜치에서는 인프라만 준비하고, 실제 저장은 pattern에서 시작한다.
@Repository
public class RedisRefreshTokenRepository implements RefreshTokenRepository {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    public RedisRefreshTokenRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

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
