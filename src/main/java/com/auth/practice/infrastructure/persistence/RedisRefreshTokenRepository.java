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

    // [멀티세션] "refresh:{jti}" 형태의 key 설계.
    //            기존 "refresh:{userId}"는 userId당 1개만 유지되어 새 로그인이 기존 세션을 덮어썼다.
    //            jti(JWT ID, UUID)를 key로 사용하면 같은 userId라도 로그인마다 독립적인 key가 생성된다.
    //            value는 userId를 저장해 갱신 시 누구의 토큰인지 확인한다.
    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    public RedisRefreshTokenRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // [현업패턴] TTL을 Refresh Token 만료 시간과 동일하게 설정.
    //            Redis TTL이 먼저 만료되면 유효한 토큰이 사라지는 문제가 생기므로 일치시킨다.
    @Override
    public void save(String jti, Long userId, long ttlSeconds) {
        redisTemplate.opsForValue().set(KEY_PREFIX + jti, userId.toString(), ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Optional<Long> findUserIdByJti(String jti) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + jti);
        return Optional.ofNullable(value).map(Long::parseLong);
    }

    @Override
    public void deleteByJti(String jti) {
        redisTemplate.delete(KEY_PREFIX + jti);
    }
}
