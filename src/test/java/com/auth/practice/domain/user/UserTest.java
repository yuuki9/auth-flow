package com.auth.practice.domain.user;

import com.auth.practice.domain.oauth.OAuth2Provider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class UserTest {

    @Test
    void create_sets_default_role_to_USER() {
        User user = User.create("test@gmail.com", "홍길동",
                OAuth2Provider.GOOGLE, "google-123", "https://img.url");

        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getCreatedAt()).isNotNull();
    }

    @Test
    void update_changes_name_and_profileImage() {
        User user = User.create("test@gmail.com", "홍길동",
                OAuth2Provider.GOOGLE, "google-123", "https://old-img.url");

        user.update("김철수", "https://new-img.url");

        assertThat(user.getName()).isEqualTo("김철수");
        assertThat(user.getProfileImageUrl()).isEqualTo("https://new-img.url");
    }
}
