package com.auth.practice.infrastructure.config;

import com.auth.practice.domain.user.User;
import com.auth.practice.domain.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.findByEmail("test@example.com").isEmpty()) {
            User user = User.create(
                    "test@example.com",
                    passwordEncoder.encode("password123"),
                    "테스트 사용자"
            );
            userRepository.save(user);
            log.info("테스트 사용자 생성: test@example.com / password123");
        }
    }
}
