# OAuth2 + JWT 현업 수준 인증 시스템 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 학습용 STAGE 구조를 OAuth2 + JWT(3가지 저장 패턴) 현업 수준 인증 시스템으로 전환

**Architecture:** `base/oauth2-foundation` 브랜치에서 공통 기반(PostgreSQL, OAuth2, REST API)을 구축하고, 3개 패턴 브랜치(`pattern/cookie-only`, `pattern/memory-access`, `pattern/localstorage`)로 분기. 각 패턴 브랜치는 JWT 발급/저장/검증 방식만 다르며 나머지는 동일.

**Tech Stack:** Java 17, Spring Boot 3.x, PostgreSQL, Redis, jjwt 0.12.x, Gradle Kotlin DSL

---

## Phase 1: main 브랜치 정리

### Task 1: STAGE 파일 삭제 및 Thymeleaf 제거

**Files:**
- Delete: `STAGE1_COMPLETION.md`
- Delete: `STAGE2_COMPLETION.md`
- Delete: `src/main/resources/templates/home.html`
- Delete: `src/main/resources/templates/index.html`
- Delete: `src/main/resources/templates/login.html`
- Modify: `build.gradle.kts`
- Modify: `src/main/resources/application.yaml`
- Modify: `src/main/java/com/auth/practice/presentation/controller/HomeController.java`

- [ ] **Step 1: STAGE 파일 삭제**

```bash
git rm STAGE1_COMPLETION.md STAGE2_COMPLETION.md
git rm src/main/resources/templates/home.html
git rm src/main/resources/templates/index.html
git rm src/main/resources/templates/login.html
```

- [ ] **Step 2: build.gradle.kts에서 Thymeleaf 제거**

`build.gradle.kts`를 아래와 같이 수정:

```kotlin
plugins {
    java
    id("org.springframework.boot") version "3.5.11-SNAPSHOT"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.auth.practice"
version = "0.0.1-SNAPSHOT"
description = "OAuth 2.0, JWT, OpenID Connect 현업 수준 학습 프로젝트"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/snapshot") }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("me.paulschwarz:spring-dotenv:4.0.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

- [ ] **Step 3: application.yaml을 OAuth2만 남기도록 정리**

`src/main/resources/application.yaml`을 아래로 교체:

```yaml
spring:
  application:
    name: auth-guide
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope:
              - email
              - profile
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
          kakao:
            client-id: ${KAKAO_CLIENT_ID}
            client-secret: ${KAKAO_CLIENT_SECRET}
            client-authentication-method: client_secret_post
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope:
              - profile_nickname
              - profile_image
              - account_email
            client-name: Kakao
          naver:
            client-id: ${NAVER_CLIENT_ID}
            client-secret: ${NAVER_CLIENT_SECRET}
            client-authentication-method: client_secret_post
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope:
              - name
              - email
              - profile_image
            client-name: Naver
        provider:
          kakao:
            authorization-uri: https://kauth.kakao.com/oauth/authorize
            token-uri: https://kauth.kakao.com/oauth/token
            user-info-uri: https://kapi.kakao.com/v2/user/me
            user-name-attribute: id
          naver:
            authorization-uri: https://nid.naver.com/oauth2.0/authorize
            token-uri: https://nid.naver.com/oauth2.0/token
            user-info-uri: https://openapi.naver.com/v1/nid/me
            user-name-attribute: response

logging:
  level:
    org.springframework.security: DEBUG
    org.springframework.security.oauth2: DEBUG
```

- [ ] **Step 4: HomeController 삭제**

Thymeleaf 뷰를 반환하는 `HomeController`는 base 브랜치에서 `AuthController`로 대체. 지금은 파일만 삭제.

```bash
git rm src/main/java/com/auth/practice/presentation/controller/HomeController.java
```

- [ ] **Step 5: SecurityConfig 임시 정리 — 로그인 페이지 커스텀 제거**

`SecurityConfig.java`에서 Thymeleaf 로그인 페이지 참조 제거:

```java
package com.auth.practice.infrastructure.security.config;

import com.auth.practice.infrastructure.security.oauth.CustomOAuth2UserService;
import com.auth.practice.infrastructure.security.oauth.OAuth2AuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final CustomOAuth2UserService customOAuth2UserService;

    public SecurityConfig(
            OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
            CustomOAuth2UserService customOAuth2UserService) {
        this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/error", "/oauth2/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2AuthenticationSuccessHandler)
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }
}
```

- [ ] **Step 6: 커밋**

```bash
git add -A
git commit -m "chore: remove STAGE files and Thymeleaf, clean up for REST API transition"
```

---

## Phase 2: base/oauth2-foundation 브랜치

### Task 2: base 브랜치 생성 및 인프라 의존성 추가

**Files:**
- Create: `docker-compose.yml`
- Create: `src/main/resources/application-local.yaml.example`
- Modify: `build.gradle.kts`
- Modify: `src/main/resources/application.yaml`

- [ ] **Step 1: base 브랜치 생성**

```bash
git checkout -b base/oauth2-foundation
```

- [ ] **Step 2: build.gradle.kts에 PostgreSQL, JPA, Redis 추가**

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("me.paulschwarz:spring-dotenv:4.0.0")

    // PostgreSQL + JPA
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
```

- [ ] **Step 3: docker-compose.yml 생성**

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: authguide
      POSTGRES_USER: authuser
      POSTGRES_PASSWORD: authpass
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7
    ports:
      - "6379:6379"

volumes:
  postgres_data:
```

- [ ] **Step 4: application.yaml에 datasource + redis 추가**

기존 OAuth2 설정 아래에 추가:

```yaml
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/authguide}
    username: ${DB_USERNAME:authuser}
    password: ${DB_PASSWORD:authpass}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        format_sql: true
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
```

- [ ] **Step 5: application-local.yaml.example 생성**

`src/main/resources/application-local.yaml.example`:

```yaml
# 이 파일을 application-local.yaml로 복사 후 실제 값으로 채우세요.
# application-local.yaml은 .gitignore에 의해 추적되지 않습니다.

spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: your-google-client-id
            client-secret: your-google-client-secret
          kakao:
            client-id: your-kakao-client-id
            client-secret: your-kakao-client-secret
          naver:
            client-id: your-naver-client-id
            client-secret: your-naver-client-secret
  datasource:
    url: jdbc:postgresql://localhost:5432/authguide
    username: authuser
    password: authpass
  data:
    redis:
      host: localhost
      port: 6379

# JWT 설정은 pattern 브랜치에서 추가됩니다
# jwt:
#   secret: your-256-bit-secret-key-here
#   access-token-expiry-ms: 900000    # 15분
#   refresh-token-expiry-ms: 604800000 # 7일
```

- [ ] **Step 6: 커밋**

```bash
git add -A
git commit -m "chore: add PostgreSQL, Redis dependencies and docker-compose"
```

---

### Task 3: User 도메인 (엔티티 + 리포지토리)

**Files:**
- Create: `src/main/java/com/auth/practice/domain/user/UserRole.java`
- Create: `src/main/java/com/auth/practice/domain/user/User.java`
- Create: `src/main/java/com/auth/practice/domain/user/UserRepository.java`
- Create: `src/main/java/com/auth/practice/infrastructure/persistence/UserJpaRepository.java`
- Create: `src/test/java/com/auth/practice/domain/user/UserTest.java`

- [ ] **Step 1: UserRole 열거형 생성**

```java
package com.auth.practice.domain.user;

public enum UserRole {
    USER, ADMIN
}
```

- [ ] **Step 2: User 엔티티 생성**

```java
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

    // [왜?] email + provider 조합으로 유니크 보장.
    //       같은 이메일이라도 Google과 Kakao는 별개 계정으로 관리.
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
```

- [ ] **Step 3: UserRepository 인터페이스 (도메인 계층)**

```java
package com.auth.practice.domain.user;

import com.auth.practice.domain.oauth.OAuth2Provider;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findByProviderAndProviderId(OAuth2Provider provider, String providerId);
    User save(User user);
}
```

- [ ] **Step 4: UserJpaRepository (인프라 계층 구현체)**

```java
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
```

- [ ] **Step 5: User 단위 테스트 작성**

```java
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
```

- [ ] **Step 6: 테스트 실행 확인**

```bash
./gradlew test --tests "com.auth.practice.domain.user.UserTest"
```

Expected: 2 tests PASS

- [ ] **Step 7: 커밋**

```bash
git add -A
git commit -m "feat: add User domain entity and repository"
```

---

### Task 4: CustomOAuth2UserService — DB upsert 추가

**Files:**
- Modify: `src/main/java/com/auth/practice/infrastructure/security/oauth/CustomOAuth2UserService.java`
- Modify: `src/main/java/com/auth/practice/infrastructure/security/oauth/CustomOAuth2User.java`

- [ ] **Step 1: CustomOAuth2User에 userId 추가**

```java
package com.auth.practice.infrastructure.security.oauth;

import com.auth.practice.domain.oauth.OAuth2UserInfo;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {

    private final Long userId;
    private final OAuth2UserInfo oAuth2UserInfo;
    private final Map<String, Object> attributes;
    private final String userNameAttributeName;

    public CustomOAuth2User(Long userId, OAuth2UserInfo oAuth2UserInfo,
                            Map<String, Object> attributes, String userNameAttributeName) {
        this.userId = userId;
        this.oAuth2UserInfo = oAuth2UserInfo;
        this.attributes = attributes;
        this.userNameAttributeName = userNameAttributeName;
    }

    @Override
    public Map<String, Object> getAttributes() { return attributes; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getName() { return String.valueOf(attributes.get(userNameAttributeName)); }

    public Long getUserId() { return userId; }
    public OAuth2UserInfo getOAuth2UserInfo() { return oAuth2UserInfo; }
    public String getUserName() { return oAuth2UserInfo.getName(); }
    public String getEmail() { return oAuth2UserInfo.getEmail(); }
    public String getProfileImageUrl() { return oAuth2UserInfo.getProfileImageUrl(); }
}
```

- [ ] **Step 2: CustomOAuth2UserService에 DB upsert 추가**

```java
package com.auth.practice.infrastructure.security.oauth;

import com.auth.practice.domain.oauth.OAuth2UserInfo;
import com.auth.practice.domain.user.User;
import com.auth.practice.domain.user.UserRepository;
import com.auth.practice.infrastructure.security.oauth.userinfo.OAuth2UserInfoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.of(
                registrationId, oAuth2User.getAttributes());

        // [현업패턴] upsert 패턴: 신규 사용자면 INSERT, 기존 사용자면 이름/프로필만 UPDATE.
        //            providerId를 기준으로 조회하는 이유: 이메일은 사용자가 변경 가능하기 때문.
        User user = userRepository.findByProviderAndProviderId(
                        oAuth2UserInfo.getProvider(), oAuth2UserInfo.getProviderId())
                .map(existing -> {
                    existing.update(oAuth2UserInfo.getName(), oAuth2UserInfo.getProfileImageUrl());
                    return userRepository.save(existing);
                })
                .orElseGet(() -> userRepository.save(User.create(
                        oAuth2UserInfo.getEmail(),
                        oAuth2UserInfo.getName(),
                        oAuth2UserInfo.getProvider(),
                        oAuth2UserInfo.getProviderId(),
                        oAuth2UserInfo.getProfileImageUrl()
                )));

        log.info("OAuth2 로그인 성공 - provider: {}, userId: {}, email: {}",
                oAuth2UserInfo.getProvider(), user.getId(), user.getEmail());

        return new CustomOAuth2User(user.getId(), oAuth2UserInfo,
                oAuth2User.getAttributes(), userNameAttributeName);
    }
}
```

- [ ] **Step 3: 커밋**

```bash
git add -A
git commit -m "feat: add DB upsert to CustomOAuth2UserService"
```

---

### Task 5: REST API 기반 SecurityConfig + AuthController

**Files:**
- Modify: `src/main/java/com/auth/practice/infrastructure/security/config/SecurityConfig.java`
- Modify: `src/main/java/com/auth/practice/infrastructure/security/oauth/OAuth2AuthenticationSuccessHandler.java`
- Create: `src/main/java/com/auth/practice/presentation/controller/AuthController.java`
- Create: `src/main/java/com/auth/practice/presentation/dto/UserInfoResponse.java`
- Create: `src/main/resources/static/index.html`

- [ ] **Step 1: SecurityConfig를 Stateless REST API 기반으로 교체**

```java
package com.auth.practice.infrastructure.security.config;

import com.auth.practice.infrastructure.security.oauth.CustomOAuth2UserService;
import com.auth.practice.infrastructure.security.oauth.OAuth2AuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final CustomOAuth2UserService customOAuth2UserService;

    public SecurityConfig(
            OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
            CustomOAuth2UserService customOAuth2UserService) {
        this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // [왜?] REST API는 세션이 필요 없음. 매 요청을 토큰으로 인증.
            //       STATELESS 설정으로 서버가 세션 상태를 유지하지 않음.
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // [보안] CSRF: 세션 기반 인증에서 필요. JWT + Authorization 헤더 방식은 CSRF 불필요.
            //       (단, Cookie 기반 JWT에서는 반드시 CSRF 방어 필요 — pattern/cookie-only 참고)
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/error", "/oauth2/**",
                                 "/login/oauth2/**", "/index.html").permitAll()
                .requestMatchers("/api/auth/refresh", "/api/auth/logout").permitAll()
                .anyRequest().authenticated()
            )

            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2AuthenticationSuccessHandler)
            );

        return http.build();
    }
}
```

- [ ] **Step 2: OAuth2AuthenticationSuccessHandler — base 브랜치용 (JSON 응답 뼈대)**

```java
package com.auth.practice.infrastructure.security.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

// [현업패턴] OAuth2 로그인 성공 후 JWT를 발급해 클라이언트로 전달하는 진입점.
//           이 base 브랜치에서는 userId만 JSON으로 반환 (JWT 미포함).
//           실제 토큰 발급은 pattern 브랜치에서 추가됨.
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();

        log.info("OAuth2 로그인 성공 - userId: {}, provider: {}",
                principal.getUserId(),
                principal.getOAuth2UserInfo().getProvider());

        // [TODO] pattern 브랜치에서 JWT 발급 후 토큰을 응답에 포함
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "userId", principal.getUserId(),
                "name", principal.getUserName(),
                "email", principal.getEmail()
        )));
    }
}
```

- [ ] **Step 3: UserInfoResponse DTO 생성**

```java
package com.auth.practice.presentation.dto;

import com.auth.practice.domain.user.User;

public record UserInfoResponse(
        Long userId,
        String email,
        String name,
        String profileImageUrl,
        String provider,
        String role
) {
    public static UserInfoResponse from(User user) {
        return new UserInfoResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getProfileImageUrl(),
                user.getProvider().name(),
                user.getRole().name()
        );
    }
}
```

- [ ] **Step 4: AuthController 생성**

```java
package com.auth.practice.presentation.controller;

import com.auth.practice.domain.user.User;
import com.auth.practice.domain.user.UserRepository;
import com.auth.practice.infrastructure.security.oauth.CustomOAuth2User;
import com.auth.practice.presentation.dto.UserInfoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // [현업패턴] /api/auth/me: 현재 로그인한 사용자 정보 조회.
    //           토큰에서 userId를 꺼내 DB에서 최신 정보를 조회.
    //           토큰에 모든 정보를 넣지 않는 이유: 정보 변경 시 토큰 재발급 없이 반영 가능.
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> me(
            @AuthenticationPrincipal CustomOAuth2User principal) {
        User user = userRepository.findByProviderAndProviderId(
                        principal.getOAuth2UserInfo().getProvider(),
                        principal.getOAuth2UserInfo().getProviderId())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        return ResponseEntity.ok(UserInfoResponse.from(user));
    }
}
```

- [ ] **Step 5: 테스트용 index.html 생성**

`src/main/resources/static/index.html`:

```html
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>Auth Guide - Base (OAuth2 Only)</title>
    <style>
        body { font-family: sans-serif; max-width: 600px; margin: 50px auto; padding: 0 20px; }
        button { margin: 8px 0; padding: 10px 20px; cursor: pointer; }
        pre { background: #f4f4f4; padding: 16px; border-radius: 4px; overflow-x: auto; }
    </style>
</head>
<body>
    <h1>Auth Guide — base/oauth2-foundation</h1>
    <p>현재 브랜치: OAuth2 로그인 + PostgreSQL 사용자 저장 (JWT 없음)</p>

    <h2>소셜 로그인</h2>
    <button onclick="location.href='/oauth2/authorization/google'">Google 로그인</button>
    <button onclick="location.href='/oauth2/authorization/kakao'">Kakao 로그인</button>
    <button onclick="location.href='/oauth2/authorization/naver'">Naver 로그인</button>

    <h2>내 정보</h2>
    <button onclick="getMe()">GET /api/auth/me</button>
    <pre id="result">결과가 여기에 표시됩니다</pre>

    <script>
        async function getMe() {
            const res = await fetch('/api/auth/me', { credentials: 'include' });
            const data = await res.json();
            document.getElementById('result').textContent = JSON.stringify(data, null, 2);
        }
    </script>
</body>
</html>
```

- [ ] **Step 6: 커밋**

```bash
git add -A
git commit -m "feat: add REST API foundation - SecurityConfig stateless, AuthController, test HTML"
```

---

## Phase 3: pattern/cookie-only 브랜치

### Task 6: pattern/cookie-only 브랜치 생성 및 JWT 의존성

**Files:**
- Modify: `build.gradle.kts`
- Modify: `src/main/resources/application.yaml`
- Create: `src/main/java/com/auth/practice/infrastructure/security/jwt/JwtProperties.java`

- [ ] **Step 1: base에서 패턴 브랜치 생성**

```bash
git checkout base/oauth2-foundation
git checkout -b pattern/cookie-only
```

- [ ] **Step 2: build.gradle.kts에 jjwt 추가**

dependencies 블록에 추가:

```kotlin
// JWT
implementation("io.jsonwebtoken:jjwt-api:0.12.6")
runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
```

- [ ] **Step 3: application.yaml에 JWT 설정 추가**

```yaml
jwt:
  secret: ${JWT_SECRET}
  # [왜?] 15분: 탈취 시 피해 최소화. Refresh Token으로 자동 갱신.
  access-token-expiry-ms: 900000
  # [왜?] 7일: 매일 재로그인 없이 사용 가능한 UX 밸런스.
  refresh-token-expiry-ms: 604800000
```

- [ ] **Step 4: JwtProperties 설정 클래스 생성**

```java
package com.auth.practice.infrastructure.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private long accessTokenExpiryMs;
    private long refreshTokenExpiryMs;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public long getAccessTokenExpiryMs() { return accessTokenExpiryMs; }
    public void setAccessTokenExpiryMs(long ms) { this.accessTokenExpiryMs = ms; }
    public long getRefreshTokenExpiryMs() { return refreshTokenExpiryMs; }
    public void setRefreshTokenExpiryMs(long ms) { this.refreshTokenExpiryMs = ms; }
}
```

- [ ] **Step 5: 커밋**

```bash
git add -A
git commit -m "chore: add jjwt dependency and JWT properties config"
```

---

### Task 7: JwtProvider — 토큰 생성/검증

**Files:**
- Create: `src/main/java/com/auth/practice/infrastructure/security/jwt/JwtProvider.java`
- Create: `src/test/java/com/auth/practice/infrastructure/security/jwt/JwtProviderTest.java`

- [ ] **Step 1: JwtProvider 테스트 먼저 작성**

```java
package com.auth.practice.infrastructure.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-must-be-at-least-256-bits-long-for-hs256");
        props.setAccessTokenExpiryMs(900000L);
        props.setRefreshTokenExpiryMs(604800000L);
        jwtProvider = new JwtProvider(props);
    }

    @Test
    void generateAccessToken_returns_valid_token() {
        String token = jwtProvider.generateAccessToken(1L, "USER");
        assertThat(jwtProvider.validateToken(token)).isTrue();
        assertThat(jwtProvider.getUserId(token)).isEqualTo(1L);
    }

    @Test
    void generateRefreshToken_returns_valid_token() {
        String token = jwtProvider.generateRefreshToken(1L);
        assertThat(jwtProvider.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_returns_false_for_tampered_token() {
        String token = jwtProvider.generateAccessToken(1L, "USER");
        assertThat(jwtProvider.validateToken(token + "tampered")).isFalse();
    }

    @Test
    void validateToken_returns_false_for_expired_token() {
        JwtProperties shortProps = new JwtProperties();
        shortProps.setSecret("test-secret-key-must-be-at-least-256-bits-long-for-hs256");
        shortProps.setAccessTokenExpiryMs(-1000L); // 이미 만료
        shortProps.setRefreshTokenExpiryMs(-1000L);
        JwtProvider shortJwt = new JwtProvider(shortProps);

        String token = shortJwt.generateAccessToken(1L, "USER");
        assertThat(shortJwt.validateToken(token)).isFalse();
    }
}
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
./gradlew test --tests "com.auth.practice.infrastructure.security.jwt.JwtProviderTest"
```

Expected: FAIL (JwtProvider 미존재)

- [ ] **Step 3: JwtProvider 구현**

```java
package com.auth.practice.infrastructure.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtProvider.class);

    private final SecretKey key;
    private final long accessTokenExpiryMs;
    private final long refreshTokenExpiryMs;

    public JwtProvider(JwtProperties properties) {
        // [보안] HS256은 최소 256비트(32바이트) 키 필요.
        //       짧은 키는 브루트포스 공격에 취약. Keys.hmacShaKeyFor()가 자동 검증.
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiryMs = properties.getAccessTokenExpiryMs();
        this.refreshTokenExpiryMs = properties.getRefreshTokenExpiryMs();
    }

    public String generateAccessToken(Long userId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                // [왜?] Access Token에 role을 포함: API 요청마다 DB 조회 없이 권한 확인 가능.
                //       단, role 변경 시 즉시 반영되지 않으므로 민감한 권한은 DB 재확인 필요.
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpiryMs))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                // [왜?] Refresh Token에는 role 클레임 없음.
                //       Refresh Token은 새 Access Token 발급 용도만. 최소 정보 원칙.
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTokenExpiryMs))
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("만료된 JWT: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("유효하지 않은 JWT: {}", e.getMessage());
        }
        return false;
    }

    public Long getUserId(String token) {
        return Long.parseLong(
                Jwts.parser().verifyWith(key).build()
                        .parseSignedClaims(token).getPayload().getSubject()
        );
    }

    public String getRole(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload().get("role", String.class);
    }
}
```

- [ ] **Step 4: 테스트 재실행 — 통과 확인**

```bash
./gradlew test --tests "com.auth.practice.infrastructure.security.jwt.JwtProviderTest"
```

Expected: 4 tests PASS

- [ ] **Step 5: 커밋**

```bash
git add -A
git commit -m "feat: add JwtProvider with token generation and validation"
```

---

### Task 8: Redis Refresh Token 저장소

**Files:**
- Create: `src/main/java/com/auth/practice/domain/token/RefreshTokenRepository.java`
- Create: `src/main/java/com/auth/practice/infrastructure/persistence/RedisRefreshTokenRepository.java`
- Create: `src/main/java/com/auth/practice/infrastructure/config/RedisConfig.java`

- [ ] **Step 1: RefreshTokenRepository 인터페이스 (도메인 계층)**

```java
package com.auth.practice.domain.token;

import java.util.Optional;

public interface RefreshTokenRepository {
    void save(Long userId, String token, long ttlSeconds);
    Optional<String> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
```

- [ ] **Step 2: RedisConfig 설정**

```java
package com.auth.practice.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
```

- [ ] **Step 3: RedisRefreshTokenRepository 구현**

```java
package com.auth.practice.infrastructure.persistence;

import com.auth.practice.domain.token.RefreshTokenRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
public class RedisRefreshTokenRepository implements RefreshTokenRepository {

    // [왜?] Redis key 형식: "refresh:{userId}".
    //       prefix를 붙이는 이유: Redis는 단일 네임스페이스.
    //       다른 데이터(캐시 등)와 키 충돌 방지.
    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    public RedisRefreshTokenRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(Long userId, String token, long ttlSeconds) {
        // [왜?] TTL을 Redis에서 직접 관리: 만료된 Refresh Token이 자동 삭제됨.
        //       별도 배치 작업 없이 Redis가 스스로 정리.
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
```

- [ ] **Step 4: 커밋**

```bash
git add -A
git commit -m "feat: add Redis-backed RefreshTokenRepository"
```

---

### Task 9: AuthService — 토큰 라이프사이클

**Files:**
- Create: `src/main/java/com/auth/practice/application/auth/AuthService.java`
- Create: `src/main/java/com/auth/practice/presentation/dto/TokenResponse.java`
- Create: `src/test/java/com/auth/practice/application/auth/AuthServiceTest.java`

- [ ] **Step 1: TokenResponse DTO 생성**

```java
package com.auth.practice.presentation.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresInMs
) {}
```

- [ ] **Step 2: AuthService 테스트 작성**

```java
package com.auth.practice.application.auth;

import com.auth.practice.domain.token.RefreshTokenRepository;
import com.auth.practice.domain.user.UserRepository;
import com.auth.practice.infrastructure.security.jwt.JwtProperties;
import com.auth.practice.infrastructure.security.jwt.JwtProvider;
import com.auth.practice.presentation.dto.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private RefreshTokenRepository tokenRepository;
    @Mock private UserRepository userRepository;

    private AuthService authService;
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-must-be-at-least-256-bits-long-for-hs256");
        props.setAccessTokenExpiryMs(900000L);
        props.setRefreshTokenExpiryMs(604800000L);
        jwtProvider = new JwtProvider(props);
        authService = new AuthService(jwtProvider, tokenRepository, props);
    }

    @Test
    void issueTokens_saves_refreshToken_to_redis() {
        TokenResponse tokens = authService.issueTokens(1L, "USER");

        assertThat(tokens.accessToken()).isNotEmpty();
        assertThat(tokens.refreshToken()).isNotEmpty();
        verify(tokenRepository, times(1)).save(eq(1L), anyString(), anyLong());
    }

    @Test
    void refresh_rotates_refreshToken() {
        TokenResponse initial = authService.issueTokens(1L, "USER");
        when(tokenRepository.findByUserId(1L)).thenReturn(Optional.of(initial.refreshToken()));

        TokenResponse rotated = authService.refresh(initial.refreshToken());

        assertThat(rotated.accessToken()).isNotEqualTo(initial.accessToken());
        // [보안] Rotation 확인: 기존 토큰 삭제 + 새 토큰 저장
        verify(tokenRepository).deleteByUserId(1L);
        verify(tokenRepository, times(2)).save(eq(1L), anyString(), anyLong());
    }

    @Test
    void refresh_throws_when_token_not_in_redis() {
        TokenResponse initial = authService.issueTokens(1L, "USER");
        when(tokenRepository.findByUserId(1L)).thenReturn(Optional.empty());

        // [보안] Redis에 없는 토큰: 이미 로그아웃됐거나 탈취 후 재사용 시도.
        assertThatThrownBy(() -> authService.refresh(initial.refreshToken()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 3: 테스트 실행 — 실패 확인**

```bash
./gradlew test --tests "com.auth.practice.application.auth.AuthServiceTest"
```

Expected: FAIL (AuthService 미존재)

- [ ] **Step 4: AuthService 구현**

```java
package com.auth.practice.application.auth;

import com.auth.practice.domain.token.RefreshTokenRepository;
import com.auth.practice.infrastructure.security.jwt.JwtProperties;
import com.auth.practice.infrastructure.security.jwt.JwtProvider;
import com.auth.practice.presentation.dto.TokenResponse;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository tokenRepository;
    private final JwtProperties jwtProperties;

    public AuthService(JwtProvider jwtProvider,
                       RefreshTokenRepository tokenRepository,
                       JwtProperties jwtProperties) {
        this.jwtProvider = jwtProvider;
        this.tokenRepository = tokenRepository;
        this.jwtProperties = jwtProperties;
    }

    public TokenResponse issueTokens(Long userId, String role) {
        String accessToken = jwtProvider.generateAccessToken(userId, role);
        String refreshToken = jwtProvider.generateRefreshToken(userId);

        long ttlSeconds = jwtProperties.getRefreshTokenExpiryMs() / 1000;
        tokenRepository.save(userId, refreshToken, ttlSeconds);

        return new TokenResponse(accessToken, refreshToken,
                jwtProperties.getAccessTokenExpiryMs());
    }

    public TokenResponse refresh(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token");
        }

        Long userId = jwtProvider.getUserId(refreshToken);

        // [보안] Refresh Token Rotation(RTR): Redis에 저장된 토큰과 일치 확인.
        //       불일치 시 이미 사용됐거나 탈취된 토큰 — 즉시 거부.
        String stored = tokenRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Refresh Token이 Redis에 없음 (로그아웃 또는 탈취 감지)"));

        if (!stored.equals(refreshToken)) {
            // [보안] 토큰 재사용 감지: 같은 Refresh Token이 두 번 사용됨.
            //       해당 사용자의 모든 토큰 폐기 (강제 재로그인).
            tokenRepository.deleteByUserId(userId);
            throw new IllegalArgumentException("Refresh Token 재사용 감지 — 보안 위협");
        }

        // [현업패턴] Rotation: 기존 토큰 삭제 후 새 토큰 발급.
        tokenRepository.deleteByUserId(userId);
        return issueTokens(userId, "USER");
    }

    public void logout(Long userId) {
        tokenRepository.deleteByUserId(userId);
    }
}
```

- [ ] **Step 5: 테스트 재실행 — 통과 확인**

```bash
./gradlew test --tests "com.auth.practice.application.auth.AuthServiceTest"
```

Expected: 3 tests PASS

- [ ] **Step 6: 커밋**

```bash
git add -A
git commit -m "feat: add AuthService with token issuance, rotation, and logout"
```

---

### Task 10: JwtAuthenticationFilter (Cookie 방식) + SecurityConfig 최종

**Files:**
- Create: `src/main/java/com/auth/practice/infrastructure/security/jwt/JwtAuthenticationFilter.java`
- Modify: `src/main/java/com/auth/practice/infrastructure/security/config/SecurityConfig.java`
- Modify: `src/main/java/com/auth/practice/infrastructure/security/oauth/OAuth2AuthenticationSuccessHandler.java`
- Modify: `src/main/java/com/auth/practice/presentation/controller/AuthController.java`
- Modify: `src/main/resources/static/index.html`

- [ ] **Step 1: JwtAuthenticationFilter — Cookie에서 토큰 읽기**

```java
package com.auth.practice.infrastructure.security.jwt;

import com.auth.practice.domain.user.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

// [현업패턴] OncePerRequestFilter: 하나의 요청에 대해 딱 한 번만 실행되는 필터.
//           모든 요청의 JWT 검증이 여기서 이루어짐.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    public JwtAuthenticationFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // [현업패턴] Cookie에서 Access Token 추출.
        //           HttpOnly 쿠키는 JS에서 읽을 수 없으므로 서버에서만 처리.
        String token = extractTokenFromCookie(request);

        if (token != null && jwtProvider.validateToken(token)) {
            Long userId = jwtProvider.getUserId(token);
            String role = jwtProvider.getRole(token);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> "access_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
```

- [ ] **Step 2: OAuth2AuthenticationSuccessHandler — HttpOnly Cookie로 토큰 전달**

```java
package com.auth.practice.infrastructure.security.oauth;

import com.auth.practice.application.auth.AuthService;
import com.auth.practice.presentation.dto.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final AuthService authService;

    public OAuth2AuthenticationSuccessHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();
        TokenResponse tokens = authService.issueTokens(principal.getUserId(), "USER");

        // [보안] HttpOnly: JS에서 document.cookie로 접근 불가 → XSS 방어.
        // [보안] Secure: HTTPS에서만 전송 → 중간자 공격(MITM) 방어.
        // [보안] SameSite=Strict: 외부 사이트에서의 쿠키 전송 차단 → CSRF 방어.
        ResponseCookie accessCookie = ResponseCookie.from("access_token", tokens.accessToken())
                .httpOnly(true)
                .secure(false) // [주의] 운영 환경에서는 반드시 true (HTTPS 필요)
                .sameSite("Strict")
                .path("/")
                .maxAge(tokens.accessTokenExpiresInMs() / 1000)
                .build();

        // [현업패턴] Refresh Token 쿠키의 Path를 /api/auth/refresh로 제한.
        //           Refresh Token이 모든 API 요청에 포함되지 않도록 최소화.
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", tokens.refreshToken())
                .httpOnly(true)
                .secure(false) // [주의] 운영 환경에서는 반드시 true
                .sameSite("Strict")
                .path("/api/auth/refresh")
                .maxAge(7 * 24 * 60 * 60)
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());

        // [현업패턴] OAuth2 로그인 성공 후 프론트로 리다이렉트.
        //           SPA 환경에서는 프론트 URL로 리다이렉트.
        response.sendRedirect("/index.html");

        log.info("OAuth2 로그인 성공, 쿠키 발급 완료 - userId: {}", principal.getUserId());
    }
}
```

- [ ] **Step 3: SecurityConfig에 JwtAuthenticationFilter 추가 + CSRF 설정**

```java
package com.auth.practice.infrastructure.security.config;

import com.auth.practice.infrastructure.security.jwt.JwtAuthenticationFilter;
import com.auth.practice.infrastructure.security.jwt.JwtProvider;
import com.auth.practice.infrastructure.security.oauth.CustomOAuth2UserService;
import com.auth.practice.infrastructure.security.oauth.OAuth2AuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtProvider jwtProvider;

    public SecurityConfig(
            OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
            CustomOAuth2UserService customOAuth2UserService,
            JwtProvider jwtProvider) {
        this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
        this.customOAuth2UserService = customOAuth2UserService;
        this.jwtProvider = jwtProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // [보안] Cookie 기반 JWT에서는 CSRF 방어가 필요.
            //       SameSite=Strict 쿠키가 기본 방어를 제공하지만
            //       이전 브라우저 호환성을 위해 추가 방어 가능.
            //       여기서는 SameSite=Strict를 신뢰하고 disable.
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/error", "/oauth2/**",
                                 "/login/oauth2/**", "/index.html").permitAll()
                .requestMatchers("/api/auth/refresh", "/api/auth/logout").permitAll()
                .anyRequest().authenticated()
            )

            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2AuthenticationSuccessHandler)
            )

            // [현업패턴] JwtAuthenticationFilter를 UsernamePasswordAuthenticationFilter 앞에 등록.
            //           매 요청마다 쿠키에서 JWT를 꺼내 SecurityContext에 인증 정보 설정.
            .addFilterBefore(new JwtAuthenticationFilter(jwtProvider),
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

- [ ] **Step 4: AuthController에 refresh + logout 엔드포인트 추가**

```java
package com.auth.practice.presentation.controller;

import com.auth.practice.application.auth.AuthService;
import com.auth.practice.domain.user.User;
import com.auth.practice.domain.user.UserRepository;
import com.auth.practice.presentation.dto.TokenResponse;
import com.auth.practice.presentation.dto.UserInfoResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> me(@AuthenticationPrincipal Long userId) {
        // [왜?] JwtAuthenticationFilter가 SecurityContext에 userId(Long)를 principal로 설정.
        //       DB에서 최신 사용자 정보를 조회해 반환.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
        return ResponseEntity.ok(UserInfoResponse.from(user));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Refresh Token 없음"));
        }

        try {
            TokenResponse tokens = authService.refresh(refreshToken);
            setTokenCookies(response, tokens);
            return ResponseEntity.ok(Map.of("message", "토큰 갱신 성공"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @AuthenticationPrincipal Long userId,
            HttpServletResponse response) {

        authService.logout(userId);

        // [현업패턴] 로그아웃 시 쿠키를 maxAge=0으로 덮어쓰기 → 브라우저에서 즉시 삭제.
        ResponseCookie clearAccess = ResponseCookie.from("access_token", "")
                .httpOnly(true).path("/").maxAge(0).build();
        ResponseCookie clearRefresh = ResponseCookie.from("refresh_token", "")
                .httpOnly(true).path("/api/auth/refresh").maxAge(0).build();

        response.addHeader("Set-Cookie", clearAccess.toString());
        response.addHeader("Set-Cookie", clearRefresh.toString());

        return ResponseEntity.ok(Map.of("message", "로그아웃 완료"));
    }

    private void setTokenCookies(HttpServletResponse response, TokenResponse tokens) {
        ResponseCookie accessCookie = ResponseCookie.from("access_token", tokens.accessToken())
                .httpOnly(true).secure(false).sameSite("Strict").path("/")
                .maxAge(tokens.accessTokenExpiresInMs() / 1000).build();
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", tokens.refreshToken())
                .httpOnly(true).secure(false).sameSite("Strict").path("/api/auth/refresh")
                .maxAge(7 * 24 * 60 * 60).build();
        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
    }
}
```

- [ ] **Step 5: UserRepository에 findById 추가 (AuthController에서 사용)**

`UserRepository` 인터페이스에 추가:

```java
Optional<User> findById(Long id);
```

`UserJpaRepository`는 `JpaRepository<User, Long>`을 상속하므로 `findById`가 자동 제공됨.

- [ ] **Step 6: index.html — Cookie 패턴 테스트 페이지로 교체**

```html
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>Auth Guide — Cookie Pattern</title>
    <style>
        body { font-family: sans-serif; max-width: 700px; margin: 50px auto; padding: 0 20px; }
        button { margin: 8px 4px; padding: 10px 20px; cursor: pointer; }
        pre { background: #f4f4f4; padding: 16px; border-radius: 4px; overflow-x: auto; }
        .pattern-label { background: #e8f4fd; padding: 8px 16px; border-radius: 4px; font-weight: bold; }
    </style>
</head>
<body>
    <h1>Auth Guide</h1>
    <p class="pattern-label">패턴: HttpOnly Cookie (pattern/cookie-only)</p>
    <p>Access Token과 Refresh Token 모두 HttpOnly 쿠키에 저장.<br>
       JS에서 토큰에 직접 접근 불가 → XSS 방어.</p>

    <h2>소셜 로그인</h2>
    <button onclick="location.href='/oauth2/authorization/google'">Google 로그인</button>
    <button onclick="location.href='/oauth2/authorization/kakao'">Kakao 로그인</button>
    <button onclick="location.href='/oauth2/authorization/naver'">Naver 로그인</button>

    <h2>API 테스트</h2>
    <!-- credentials: 'include' — 쿠키를 자동으로 포함해 전송 -->
    <button onclick="call('GET', '/api/auth/me')">내 정보 조회</button>
    <button onclick="call('POST', '/api/auth/refresh')">토큰 갱신 (Rotation)</button>
    <button onclick="call('POST', '/api/auth/logout')">로그아웃</button>

    <pre id="result">결과가 여기에 표시됩니다</pre>

    <script>
        // [현업패턴] credentials: 'include' — 크로스 오리진 요청에도 쿠키 포함.
        //           SameSite=Strict 쿠키는 같은 오리진에서만 전송되므로 여기서는 include만으로 충분.
        async function call(method, url) {
            const res = await fetch(url, {
                method,
                credentials: 'include'
            });
            const data = await res.json();
            document.getElementById('result').textContent =
                `${method} ${url} → ${res.status}\n` +
                JSON.stringify(data, null, 2);
        }
    </script>
</body>
</html>
```

- [ ] **Step 7: 커밋**

```bash
git add -A
git commit -m "feat: complete pattern/cookie-only - HttpOnly Cookie JWT auth"
```

---

## Phase 4: pattern/memory-access 브랜치

### Task 11: base에서 분기, Memory Access Token 패턴 구현

- [ ] **Step 1: base에서 브랜치 생성**

```bash
git checkout base/oauth2-foundation
git checkout -b pattern/memory-access
```

- [ ] **Step 2: build.gradle.kts에 jjwt 추가**

Task 6 Step 2의 동일한 코드 적용. `dependencies` 블록에 추가:
```kotlin
implementation("io.jsonwebtoken:jjwt-api:0.12.6")
runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
```

- [ ] **Step 3: application.yaml에 JWT 설정 추가**

Task 6 Step 3의 동일한 yaml 블록 적용:
```yaml
jwt:
  secret: ${JWT_SECRET}
  access-token-expiry-ms: 900000
  refresh-token-expiry-ms: 604800000
```

- [ ] **Step 4: JwtProperties, JwtProvider 추가**

아래 파일들을 `pattern/cookie-only` 브랜치의 동일한 코드로 생성:
- `src/main/java/com/auth/practice/infrastructure/security/jwt/JwtProperties.java` — Task 6 Step 4
- `src/main/java/com/auth/practice/infrastructure/security/jwt/JwtProvider.java` — Task 7 Step 3
- `src/main/java/com/auth/practice/infrastructure/config/RedisConfig.java` — Task 8 Step 2

- [ ] **Step 5: RefreshTokenRepository, RedisRefreshTokenRepository, AuthService 추가**

아래 파일들을 `pattern/cookie-only` 브랜치의 동일한 코드로 생성:
- `src/main/java/com/auth/practice/domain/token/RefreshTokenRepository.java` — Task 8 Step 1
- `src/main/java/com/auth/practice/infrastructure/persistence/RedisRefreshTokenRepository.java` — Task 8 Step 3
- `src/main/java/com/auth/practice/application/auth/AuthService.java` — Task 9 Step 4
- `src/main/java/com/auth/practice/presentation/dto/TokenResponse.java` — Task 9 Step 1

- [ ] **Step 6: JwtAuthenticationFilter — Authorization 헤더에서 토큰 읽기**

```java
package com.auth.practice.infrastructure.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    public JwtAuthenticationFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // [현업패턴] Authorization: Bearer 헤더에서 Access Token 추출.
        //           쿠키가 아닌 헤더 방식: CSRF 공격에서 자유로움.
        //           (공격자의 사이트에서 JS가 헤더를 설정하는 것은 CORS가 차단)
        String token = extractTokenFromHeader(request);

        if (token != null && jwtProvider.validateToken(token)) {
            Long userId = jwtProvider.getUserId(token);
            String role = jwtProvider.getRole(token);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

- [ ] **Step 7: OAuth2AuthenticationSuccessHandler — Access Token은 body, Refresh Token은 Cookie**

```java
package com.auth.practice.infrastructure.security.oauth;

import com.auth.practice.application.auth.AuthService;
import com.auth.practice.presentation.dto.TokenResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OAuth2AuthenticationSuccessHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();
        TokenResponse tokens = authService.issueTokens(principal.getUserId(), "USER");

        // [현업패턴] Refresh Token만 HttpOnly 쿠키에 저장.
        //           Access Token은 응답 body로 전달 → 클라이언트 JS 변수에 보관.
        //           탭/창 닫으면 Access Token 자동 소멸 (메모리에만 존재).
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", tokens.refreshToken())
                .httpOnly(true)
                .secure(false) // [주의] 운영 환경에서는 true
                .sameSite("Strict")
                .path("/api/auth/refresh")
                .maxAge(7 * 24 * 60 * 60)
                .build();
        response.addHeader("Set-Cookie", refreshCookie.toString());

        // Access Token은 JSON body로 전달
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "accessToken", tokens.accessToken(),
                "expiresInMs", tokens.accessTokenExpiresInMs()
        )));
    }
}
```

- [ ] **Step 8: AuthController — refresh 엔드포인트 (Access Token은 body 반환)**

```java
@PostMapping("/refresh")
public ResponseEntity<?> refresh(
        @CookieValue(name = "refresh_token", required = false) String refreshToken,
        HttpServletResponse response) {

    if (refreshToken == null) {
        return ResponseEntity.badRequest().body(Map.of("error", "Refresh Token 없음"));
    }

    try {
        TokenResponse tokens = authService.refresh(refreshToken);

        // Refresh Token만 쿠키 갱신
        ResponseCookie newRefresh = ResponseCookie.from("refresh_token", tokens.refreshToken())
                .httpOnly(true).secure(false).sameSite("Strict")
                .path("/api/auth/refresh").maxAge(7 * 24 * 60 * 60).build();
        response.addHeader("Set-Cookie", newRefresh.toString());

        // Access Token은 body로 반환 → 클라이언트가 메모리에 저장
        return ResponseEntity.ok(Map.of(
                "accessToken", tokens.accessToken(),
                "expiresInMs", tokens.accessTokenExpiresInMs()
        ));
    } catch (IllegalArgumentException e) {
        return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
    }
}
```

- [ ] **Step 9: index.html — memory-access 패턴 테스트 페이지**

```html
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>Auth Guide — Memory Access Token Pattern</title>
    <style>
        body { font-family: sans-serif; max-width: 700px; margin: 50px auto; padding: 0 20px; }
        button { margin: 8px 4px; padding: 10px 20px; cursor: pointer; }
        pre { background: #f4f4f4; padding: 16px; border-radius: 4px; overflow-x: auto; }
        .pattern-label { background: #fff3cd; padding: 8px 16px; border-radius: 4px; font-weight: bold; }
    </style>
</head>
<body>
    <h1>Auth Guide</h1>
    <p class="pattern-label">패턴: Memory Access Token (pattern/memory-access)</p>
    <p>Access Token → JS 메모리 변수 (탭 닫으면 소멸)<br>
       Refresh Token → HttpOnly Cookie</p>

    <h2>소셜 로그인</h2>
    <button onclick="login('google')">Google 로그인</button>
    <button onclick="login('kakao')">Kakao 로그인</button>
    <button onclick="login('naver')">Naver 로그인</button>

    <h2>API 테스트</h2>
    <button onclick="getMe()">내 정보 조회</button>
    <button onclick="refreshToken()">토큰 갱신</button>
    <button onclick="logout()">로그아웃</button>

    <pre id="result">결과가 여기에 표시됩니다</pre>

    <script>
        // [현업패턴] Access Token을 JS 변수(클로저)에 보관.
        //           localStorage가 아닌 모듈 스코프 변수 → 탭 닫으면 소멸.
        //           XSS 공격으로 접근 가능하지만, 탈취 가능 시간이 짧음.
        let accessToken = null;

        function login(provider) {
            // [왜?] OAuth2 플로우는 리다이렉트 방식. 로그인 완료 후 서버에서 Access Token 반환.
            //       실제 구현에서는 팝업 창이나 별도 처리 필요.
            window.location.href = `/oauth2/authorization/${provider}`;
        }

        async function getMe() {
            if (!accessToken) {
                log('Access Token 없음. 먼저 로그인하거나 토큰 갱신을 실행하세요.');
                return;
            }
            // [현업패턴] Authorization: Bearer 헤더에 Access Token 포함.
            //           서버는 헤더에서 토큰을 추출하므로 credentials: 'include' 불필요.
            const res = await fetch('/api/auth/me', {
                headers: { 'Authorization': `Bearer ${accessToken}` }
            });
            const data = await res.json();
            log(`GET /api/auth/me → ${res.status}\n` + JSON.stringify(data, null, 2));
        }

        async function refreshToken() {
            // Refresh Token은 쿠키에 있으므로 credentials: 'include' 필요
            const res = await fetch('/api/auth/refresh', {
                method: 'POST',
                credentials: 'include'
            });
            const data = await res.json();
            if (res.ok) {
                accessToken = data.accessToken; // 메모리에 저장
                log(`토큰 갱신 성공. Access Token 메모리 업데이트.\n` +
                    `만료까지: ${data.expiresInMs / 1000}초`);
            } else {
                accessToken = null;
                log('토큰 갱신 실패: ' + JSON.stringify(data));
            }
        }

        async function logout() {
            const res = await fetch('/api/auth/logout', {
                method: 'POST',
                headers: accessToken ? { 'Authorization': `Bearer ${accessToken}` } : {},
                credentials: 'include'
            });
            accessToken = null; // 메모리에서 즉시 삭제
            log('로그아웃 완료. Access Token 메모리에서 삭제됨.');
        }

        function log(msg) {
            document.getElementById('result').textContent = msg;
        }
    </script>
</body>
</html>
```

- [ ] **Step 10: 커밋**

```bash
git add -A
git commit -m "feat: complete pattern/memory-access - memory Access Token + HttpOnly Refresh Cookie"
```

---

## Phase 5: pattern/localstorage 브랜치

### Task 12: base에서 분기, localStorage 패턴 구현 (경고 포함)

- [ ] **Step 1: base에서 브랜치 생성**

```bash
git checkout base/oauth2-foundation
git checkout -b pattern/localstorage
```

- [ ] **Step 2: build.gradle.kts, JwtProperties, JwtProvider, Redis, AuthService 추가**

Task 11 Step 2-5와 완전히 동일한 코드 및 파일 목록 적용 (jjwt 의존성, JWT yaml 설정, JwtProperties, JwtProvider, RedisConfig, RefreshTokenRepository, RedisRefreshTokenRepository, AuthService, TokenResponse).

- [ ] **Step 3: JwtAuthenticationFilter — Authorization 헤더 (memory-access와 동일)**

Task 11 Step 6의 코드와 동일. Authorization 헤더에서 Bearer 토큰 추출.

- [ ] **Step 4: OAuth2AuthenticationSuccessHandler — 모든 토큰을 body로 반환**

```java
@Override
public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                    Authentication authentication) throws IOException {
    CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();
    TokenResponse tokens = authService.issueTokens(principal.getUserId(), "USER");

    // [주의] localStorage 패턴: Access Token과 Refresh Token 모두 응답 body로 전달.
    //        클라이언트가 localStorage에 저장하면 XSS 공격으로 즉시 탈취 가능.
    //        이 패턴은 학습 목적으로만 사용. 실제 서비스에서는 HttpOnly Cookie 사용.
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write(objectMapper.writeValueAsString(Map.of(
            "accessToken", tokens.accessToken(),
            "refreshToken", tokens.refreshToken(),
            "expiresInMs", tokens.accessTokenExpiresInMs()
    )));
}
```

- [ ] **Step 5: AuthController — refresh 엔드포인트 (body에서 Refresh Token 수신)**

```java
@PostMapping("/refresh")
public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
    String refreshToken = body.get("refreshToken");
    if (refreshToken == null) {
        return ResponseEntity.badRequest().body(Map.of("error", "refreshToken 필드 없음"));
    }
    try {
        TokenResponse tokens = authService.refresh(refreshToken);
        return ResponseEntity.ok(Map.of(
                "accessToken", tokens.accessToken(),
                "refreshToken", tokens.refreshToken(),
                "expiresInMs", tokens.accessTokenExpiresInMs()
        ));
    } catch (IllegalArgumentException e) {
        return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
    }
}
```

- [ ] **Step 6: index.html — localStorage 패턴 테스트 (XSS 시연 포함)**

```html
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>Auth Guide — localStorage Pattern</title>
    <style>
        body { font-family: sans-serif; max-width: 700px; margin: 50px auto; padding: 0 20px; }
        button { margin: 8px 4px; padding: 10px 20px; cursor: pointer; }
        pre { background: #f4f4f4; padding: 16px; border-radius: 4px; overflow-x: auto; }
        .warning { background: #f8d7da; border: 1px solid #f5c6cb; padding: 12px 16px; border-radius: 4px; }
    </style>
</head>
<body>
    <h1>Auth Guide</h1>
    <div class="warning">
        ⚠️ <strong>경고 — localStorage 패턴 (학습 전용)</strong><br>
        이 패턴은 XSS 공격에 취약합니다. 실제 서비스에는 사용하지 마세요.<br>
        아래 "XSS 시뮬레이션" 버튼으로 직접 취약점을 확인해보세요.
    </div>

    <h2>소셜 로그인</h2>
    <button onclick="login('google')">Google 로그인</button>

    <h2>API 테스트</h2>
    <button onclick="getMe()">내 정보 조회</button>
    <button onclick="refreshToken()">토큰 갱신</button>
    <button onclick="logout()">로그아웃</button>

    <h2>XSS 취약점 시뮬레이션</h2>
    <button onclick="simulateXss()" style="background:#dc3545;color:white;">
        XSS 공격 시뮬레이션 (토큰 탈취)
    </button>

    <pre id="result">결과가 여기에 표시됩니다</pre>

    <script>
        function login(provider) {
            window.location.href = `/oauth2/authorization/${provider}`;
        }

        // [주의] localStorage에 저장된 토큰은 같은 오리진의 모든 JS가 접근 가능.
        //        XSS 취약점이 있으면 공격자 스크립트도 아래 코드로 즉시 탈취 가능.
        function saveTokens(accessToken, refreshToken) {
            localStorage.setItem('access_token', accessToken);
            localStorage.setItem('refresh_token', refreshToken);
        }

        async function getMe() {
            const token = localStorage.getItem('access_token');
            if (!token) { log('토큰 없음. 로그인하세요.'); return; }

            const res = await fetch('/api/auth/me', {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            log(`GET /api/auth/me → ${res.status}\n` + JSON.stringify(await res.json(), null, 2));
        }

        async function refreshToken() {
            const token = localStorage.getItem('refresh_token');
            if (!token) { log('Refresh Token 없음.'); return; }

            const res = await fetch('/api/auth/refresh', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ refreshToken: token })
            });
            const data = await res.json();
            if (res.ok) {
                saveTokens(data.accessToken, data.refreshToken);
                log('토큰 갱신 성공. localStorage 업데이트 완료.');
            } else {
                log('갱신 실패: ' + JSON.stringify(data));
            }
        }

        async function logout() {
            localStorage.removeItem('access_token');
            localStorage.removeItem('refresh_token');
            log('로그아웃: localStorage에서 토큰 삭제됨.');
        }

        // [주의] XSS 시뮬레이션: 공격자 스크립트가 삽입됐을 때의 시나리오.
        //        실제 XSS 공격에서는 이 코드가 외부 서버로 토큰을 전송.
        function simulateXss() {
            const stolen = {
                access_token: localStorage.getItem('access_token'),
                refresh_token: localStorage.getItem('refresh_token')
            };
            log('[XSS 시뮬레이션] 탈취된 토큰:\n' + JSON.stringify(stolen, null, 2) +
                '\n\n실제 공격에서는 이 값이 공격자 서버로 전송됩니다.\n' +
                'HttpOnly 쿠키 방식에서는 JS로 토큰 접근 자체가 불가능합니다.');
        }

        function log(msg) {
            document.getElementById('result').textContent = msg;
        }
    </script>
</body>
</html>
```

- [ ] **Step 7: 커밋**

```bash
git add -A
git commit -m "feat: complete pattern/localstorage - localStorage JWT with XSS warning and simulation"
```

---

## 최종 검증

- [ ] **전체 테스트 실행**

```bash
./gradlew test
```

Expected: 모든 테스트 PASS

- [ ] **브랜치 구조 확인**

```bash
git branch -a
```

Expected:
```
  base/oauth2-foundation
  main
  pattern/cookie-only
  pattern/memory-access
  pattern/localstorage
```

- [ ] **패턴 차이 확인**

```bash
git diff base/oauth2-foundation pattern/cookie-only -- src/main/java/com/auth/practice/infrastructure/security/oauth/OAuth2AuthenticationSuccessHandler.java
```

- [ ] **Docker 환경 기동 확인**

```bash
docker compose up -d
# PostgreSQL: localhost:5432, Redis: localhost:6379
```
