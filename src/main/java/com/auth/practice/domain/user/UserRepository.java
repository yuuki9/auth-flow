package com.auth.practice.domain.user;

import com.auth.practice.domain.oauth.OAuth2Provider;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findByProviderAndProviderId(OAuth2Provider provider, String providerId);
    Optional<User> findById(Long id);
    User save(User user);
}
