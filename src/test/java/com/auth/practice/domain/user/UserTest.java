package com.auth.practice.domain.user;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class UserTest {

    @Test
    void create_sets_role_to_USER() {
        User user = User.create("test@example.com", "hashedPw", "홍길동");

        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getCreatedAt()).isNotNull();
    }

    @Test
    void passwordHash_is_stored_not_plain_text() {
        User user = User.create("test@example.com", "$2a$10$mockHashValue", "홍길동");

        assertThat(user.getPasswordHash()).startsWith("$2a$");
    }
}
