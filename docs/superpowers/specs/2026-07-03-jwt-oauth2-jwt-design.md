# JWT + OAuth2+JWT 현업 수준 인증 시스템 설계

**날짜**: 2026-07-03  
**목적**: JWT lifecycle을 먼저 순수하게 익힌 뒤, OAuth2가 로그인 진입점만 교체한다는 것을 명확히 이해하는 2-트랙 학습 구조.

---

## 1. 브랜치 구조

```
main                          ← 코드 정리 (Thymeleaf 제거, STAGE 파일 삭제)
│
├─ base/jwt-only              ← [트랙 1] ID/PW 로그인 → JWT + Refresh lifecycle 학습
│
└─ base/oauth2-foundation     ← [트랙 2] OAuth2 소셜 로그인 → JWT + Refresh
                                  (3가지 JWT 저장 패턴을 storage 패키지로 구분)
```

**핵심 학습 diff:**
- `git diff base/jwt-only base/oauth2-foundation` → **OAuth2가 로그인 진입점만 교체함**을 한눈에 확인

---

## 2. 두 트랙의 차이

| 항목 | base/jwt-only | base/oauth2-foundation |
|---|---|---|
| 로그인 진입점 | `POST /api/auth/login {email, password}` | `GET /oauth2/authorization/{provider}` |
| 사용자 식별 | DB의 email + BCrypt 검증 | OAuth Provider 콜백 → DB upsert |
| User 테이블 | email, passwordHash, name, role | email, name, provider, providerId, profileImageUrl, role |
| SecurityConfig | stateless, no oauth2Login | stateless, oauth2Login 활성 |
| 토큰 전달 | response body → Authorization 헤더 | storage 패키지별 상이 |
| 저장 패턴 비교 | 없음 (단일 패턴으로 lifecycle에 집중) | 3개 storage 패키지로 비교 |

**JWT 발급 이후는 완전히 동일:**
- `JwtProvider` (토큰 생성/검증)
- `AuthService` (issueTokens / refresh / revoke)
- `RefreshTokenRepository` → `RedisRefreshTokenRepository`
- `AuthController` (`/api/auth/me`, `/api/auth/refresh`, `/api/auth/logout`)

---

## 3. 공통 패키지 구조

두 트랙에서 동일하게 유지되는 구조:

```
com.auth.practice
├── domain/
│   ├── user/
│   │   ├── User.java               (트랙별 필드 다름 — 아래 참고)
│   │   ├── UserRole.java           (enum: USER, ADMIN)
│   │   └── UserRepository.java
│   └── token/
│       └── RefreshTokenRepository.java  (interface: save / findByUserId / delete)
│
├── infrastructure/
│   ├── persistence/
│   │   ├── UserJpaRepository.java
│   │   └── RedisRefreshTokenRepository.java
│   ├── config/
│   │   └── RedisConfig.java
│   └── security/
│       ├── config/
│       │   └── SecurityConfig.java
│       └── jwt/
│           ├── JwtProperties.java
│           ├── JwtProvider.java
│           └── JwtAuthenticationFilter.java
│
├── application/
│   └── auth/
│       └── AuthService.java        (issueTokens / refresh / revoke)
│
└── presentation/
    ├── controller/
    │   └── AuthController.java     (/api/auth/me, /api/auth/refresh, /api/auth/logout)
    └── dto/
        ├── TokenResponse.java
        └── UserInfoResponse.java
```

---

## 4. 트랙별 추가 파일

### base/jwt-only 전용

```
presentation/controller/
└── LoginController.java       (POST /api/auth/login)
presentation/dto/
└── LoginRequest.java          ({email, password})
application/auth/
└── LoginService.java          (UserDetailsService 구현, BCrypt 검증)
```

### base/oauth2-foundation 전용

```
domain/oauth/
├── OAuth2UserInfo.java         (기존 코드 유지)
└── OAuth2Provider.java         (기존 코드 유지)
infrastructure/security/oauth/
├── CustomOAuth2User.java
├── CustomOAuth2UserService.java  (DB upsert)
├── OAuth2SuccessHandler.java
└── userinfo/
    ├── GoogleOAuth2UserInfo.java
    ├── KakaoOAuth2UserInfo.java
    └── NaverOAuth2UserInfo.java
infrastructure/security/storage/
├── RefreshTokenHandler.java        (공통 인터페이스)
├── cookie/
│   ├── CookieJwtFilter.java
│   ├── CookieOAuth2SuccessHandler.java
│   └── CookieRefreshTokenHandler.java
├── localstorage/
│   ├── LocalStorageJwtFilter.java
│   ├── LocalStorageOAuth2SuccessHandler.java
│   └── LocalStorageRefreshTokenHandler.java
└── memory/
    ├── MemoryJwtFilter.java
    ├── MemoryOAuth2SuccessHandler.java
    └── MemoryRefreshTokenHandler.java
```

---

## 5. 인증 흐름

### 5-1. JWT-only 로그인

```
1. POST /api/auth/login { email, password }
2. LoginService: DB에서 User 조회 → BCrypt 비교
3. AuthService.issueTokens(userId)
   ├─ JwtProvider로 Access Token (15분) 생성
   ├─ JwtProvider로 Refresh Token (7일) 생성
   └─ Redis에 Refresh Token 저장 (key: "refresh:{userId}", TTL: 7일)
4. 응답 body: { accessToken, refreshToken, expiresInMs }
5. 이후 API 요청: Authorization: Bearer {accessToken}
```

### 5-2. OAuth2+JWT 로그인

```
1. GET /oauth2/authorization/{provider}
2. Provider 인증 페이지 → callback
3. CustomOAuth2UserService.loadUser()
   └─ DB에 User upsert (신규: INSERT, 기존: 이름/프로필 UPDATE)
4. OAuth2SuccessHandler / 패턴별 SuccessHandler
   └─ AuthService.issueTokens(userId) 호출
5. 토큰 전달: storage 패키지별 방식 (cookie / localstorage / memory)
```

**OAuth ↔ JWT 책임 분리:**
- `OAuth2AuthenticationSuccessHandler` → OAuth 완료 후 `AuthService` 호출만. JWT 직접 다루지 않음
- `AuthService` → 두 트랙의 로그인 결과를 동일한 방식으로 처리하는 오케스트레이터

### 5-3. 토큰 갱신 (공통 — Refresh Token Rotation)

```
1. POST /api/auth/refresh
   - jwt-only: body { refreshToken }
   - oauth: storage 패키지별 (쿠키 or body)
2. JwtProvider로 서명 검증
3. Redis에서 저장된 토큰과 일치 여부 확인
4. 기존 토큰 Redis에서 삭제 → 새 Access/Refresh Token 발급 → Redis에 저장
5. 동일한 방식으로 응답 반환
```

### 5-4. 로그아웃 (공통)

```
1. POST /api/auth/logout
2. Redis에서 해당 userId의 Refresh Token 삭제
3. 패키지별 쿠키 삭제 (cookie 패키지) 또는 클라이언트 처리
```

---

## 6. 패턴별 차이점 (oauth 트랙 — storage 패키지)

### storage/cookie
- Access Token + Refresh Token: HttpOnly Cookie
- API 요청: 쿠키 자동 전송
- XSS 위험: 낮음 / CSRF 방어 필요 (SameSite=Strict)

### storage/memory
- Access Token: response body → JS 변수 (탭 닫으면 소멸)
- Refresh Token: HttpOnly Cookie
- API 요청: `Authorization: Bearer {accessToken}`
- Silent Refresh: 401 응답 시 `/api/auth/refresh` 자동 호출

### storage/localstorage
- Access Token + Refresh Token: response body → localStorage
- API 요청: `Authorization: Bearer {accessToken}`
- XSS 위험: **높음** — 코드 내 경고 주석 명시
- 목적: 이 패턴이 왜 위험한지 직접 경험

---

## 7. 보안 설정

| 항목 | 설정값 | 이유 |
|---|---|---|
| Access Token TTL | 15분 | 탈취 시 피해 최소화 |
| Refresh Token TTL | 7일 | UX와 보안 밸런스 |
| Refresh Token Rotation | 활성화 | 탈취 감지 가능 |
| HttpOnly Cookie | 활성화 | XSS 방어 |
| Secure Cookie | 활성화 (HTTPS) | 중간자 공격 방어 |
| SameSite=Strict | cookie 패턴 | CSRF 방어 |
| 세션 | Stateless | JWT 기반, 서버 세션 없음 |

---

## 8. 기술 스택

| 항목 | 선택 |
|---|---|
| Java | 17 |
| Spring Boot | 3.x |
| 빌드 | Gradle Kotlin DSL |
| DB | PostgreSQL (Docker) |
| 캐시/토큰 저장 | Redis (Docker) |
| ORM | Spring Data JPA |
| OAuth2 Providers | Google, Kakao, Naver (oauth 트랙만) |
| JWT 라이브러리 | `io.jsonwebtoken:jjwt 0.12.6` |
| 비밀번호 해싱 | BCrypt (jwt-only 트랙만) |
| 프론트엔드 | 바닐라 HTML + fetch (테스트용) |

---

## 9. 의존성

### base/jwt-only

```kotlin
implementation("org.springframework.boot:spring-boot-starter-security")
implementation("org.springframework.boot:spring-boot-starter-web")
implementation("org.springframework.boot:spring-boot-starter-data-jpa")
implementation("org.springframework.boot:spring-boot-starter-data-redis")
runtimeOnly("org.postgresql:postgresql")
implementation("io.jsonwebtoken:jjwt-api:0.12.6")
runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
implementation("me.paulschwarz:spring-dotenv:4.0.0")
```

### base/oauth2-foundation (jwt-only 의존성 + 아래 추가)

```kotlin
implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
// BCrypt는 spring-security에 포함 — 별도 의존성 불필요
```

---

## 10. 삭제 대상 (main 브랜치 정리)

- `src/main/resources/templates/` (Thymeleaf 전체)
- `src/main/java/.../presentation/controller/HomeController.java`
- `build.gradle.kts`에서 Thymeleaf 의존성 제거
- `SecurityConfig.java`에서 `.loginPage("/login")` 제거

---

## 11. 주석 전략

```java
// [왜?]       설계 결정 이유 (왜 이렇게 했는가, 다른 선택지와의 트레이드오프)
// [보안]      공격 유형, 방어 방법, 주의해야 할 패턴
// [현업패턴]  실제 현업에서 이 방식을 채택하는 이유
// [주의]      잘못 수정하면 취약점이 되는 코드
// [TODO]      추후 개선 가능한 부분
```
