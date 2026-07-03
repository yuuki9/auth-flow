package com.auth.practice.domain.token;

import java.util.Optional;

public interface RefreshTokenRepository {
    void save(Long userId, String token, long ttlSeconds);
    Optional<String> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
