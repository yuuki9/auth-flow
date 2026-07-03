package com.auth.practice.infrastructure.persistence;

import com.auth.practice.domain.oauth.OAuth2Provider;
import com.auth.practice.domain.user.User;
import com.auth.practice.domain.user.UserRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserJpaRepository extends JpaRepository<User, Long>, UserRepository {
    Optional<User> findByProviderAndProviderId(OAuth2Provider provider, String providerId);
}
