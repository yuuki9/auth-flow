package com.auth.practice.domain.user;

import com.auth.practice.domain.oauth.OAuth2Provider;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OAuth2Provider provider;

    // [왜?] providerId: Provider가 부여한 고유 ID.
    //       이메일은 사용자가 변경할 수 있으므로 Provider ID가 더 신뢰할 수 있는 식별자.
    @Column(nullable = false)
    private String providerId;

    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected User() {}

    public static User create(String email, String name, OAuth2Provider provider,
                               String providerId, String profileImageUrl) {
        User user = new User();
        user.email = email;
        user.name = name;
        user.provider = provider;
        user.providerId = providerId;
        user.profileImageUrl = profileImageUrl;
        user.role = UserRole.USER;
        user.createdAt = LocalDateTime.now();
        user.updatedAt = LocalDateTime.now();
        return user;
    }

    public void update(String name, String profileImageUrl) {
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public OAuth2Provider getProvider() { return provider; }
    public String getProviderId() { return providerId; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public UserRole getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
