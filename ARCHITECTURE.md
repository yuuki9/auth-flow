# 🏗️ OAuth 2.0 인증 시스템 아키텍처

## 프로젝트 목표
DDD 기반 레이어드 아키텍처로 구현하는 실무 수준의 OAuth 2.0 + JWT + Redis 인증 시스템

---

## 아키텍처: 레이어드 아키텍처 (Layered Architecture)

```
┌─────────────────────────────────────────────┐
│       Presentation Layer (컨트롤러)          │
│   - REST API 엔드포인트                      │
│   - DTO 변환                                 │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│      Application Layer (애플리케이션)         │
│   - 비즈니스 로직 (Service)                  │
│   - Use Case 구현                            │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│         Domain Layer (도메인)                │
│   - 엔티티, Value Object                     │
│   - 도메인 이벤트                            │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│    Infrastructure Layer (인프라)             │
│   - Repository 구현                          │
│   - 외부 API 연동 (OAuth Provider)           │
│   - Security 설정 (JWT, OAuth2)              │
│   - Redis, Database                          │
└─────────────────────────────────────────────┘
```

---

## 인증 전략: Hybrid (JWT + Redis)

### Access Token (Stateless JWT)
- **저장 위치**: 클라이언트 (메모리 or LocalStorage)
- **만료 시간**: 15분
- **검증 방식**: JWT 서명만 확인 (Redis 조회 없음)
- **목적**: 성능 최적화

### Refresh Token (Stateful with Redis)
- **저장 위치**: Redis (서버) + HttpOnly Cookie (클라이언트)
- **만료 시간**: 7일
- **검증 방식**: Redis 조회 필수
- **목적**: 보안 강화, 강제 로그아웃 가능

### Flow
```
1. 로그인 성공
   → Access Token (JWT) 발급
   → Refresh Token 생성 후 Redis 저장
   
2. API 호출
   → Access Token 검증 (Stateless)
   → Redis 조회 없음 (빠름!)
   
3. Access Token 만료
   → Refresh Token으로 재발급 요청
   → Redis에서 Refresh Token 확인
   → 새로운 Access Token 발급
   
4. 로그아웃
   → Redis에서 Refresh Token 삭제
   → Access Token은 15분 후 자동 만료
```

---

## 최종 디렉토리 구조

```
src/main/java/com/auth/practice/
├── presentation/                 # 프레젠테이션 계층
│   ├── controller/
│   │   ├── AuthController.java
│   │   ├── UserController.java
│   │   └── OAuth2Controller.java
│   └── dto/
│       ├── request/
│       │   ├── TokenRefreshRequest.java
│       │   └── LogoutRequest.java
│       └── response/
│           ├── TokenResponse.java
│           ├── UserResponse.java
│           └── ApiResponse.java
│
├── application/                  # 애플리케이션 계층
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── UserService.java
│   │   ├── OAuth2Service.java
│   │   ├── JwtService.java
│   │   └── TokenService.java
│   └── facade/
│       └── AuthFacade.java      # 여러 서비스 조합
│
├── domain/                       # 도메인 계층
│   ├── user/
│   │   ├── User.java            # 사용자 엔티티
│   │   ├── UserRole.java        # 권한
│   │   ├── UserStatus.java      # 상태
│   │   └── SocialProvider.java  # 소셜 Provider 정보
│   ├── auth/
│   │   ├── RefreshToken.java   # Refresh Token 도메인
│   │   ├── UserSession.java    # 사용자 세션 (디바이스)
│   │   └── DeviceInfo.java     # 디바이스 정보
│   └── oauth/
│       ├── OAuth2Provider.java # OAuth Provider 열거형
│       └── OAuth2UserInfo.java # OAuth 사용자 정보 인터페이스
│
└── infrastructure/               # 인프라 계층
    ├── persistence/
    │   ├── repository/
    │   │   ├── UserRepository.java
    │   │   ├── SocialProviderRepository.java
    │   │   └── UserSessionRepository.java
    │   └── entity/
    │       ├── UserEntity.java
    │       └── (필요시 엔티티와 도메인 분리)
    │
    ├── security/
    │   ├── jwt/
    │   │   ├── JwtTokenProvider.java
    │   │   ├── JwtAuthenticationFilter.java
    │   │   └── JwtExceptionFilter.java
    │   ├── oauth/
    │   │   ├── CustomOAuth2UserService.java
    │   │   ├── OAuth2AuthenticationSuccessHandler.java
    │   │   ├── OAuth2AuthenticationFailureHandler.java
    │   │   └── userinfo/
    │   │       ├── OAuth2UserInfoFactory.java
    │   │       ├── GoogleOAuth2UserInfo.java
    │   │       ├── KakaoOAuth2UserInfo.java
    │   │       └── NaverOAuth2UserInfo.java
    │   └── config/
    │       └── SecurityConfig.java
    │
    ├── redis/
    │   ├── RedisConfig.java
    │   ├── RedisTokenRepository.java
    │   └── RedisService.java
    │
    └── common/
        ├── exception/
        │   ├── GlobalExceptionHandler.java
        │   ├── AuthException.java
        │   └── ErrorCode.java
        ├── util/
        │   ├── CookieUtil.java
        │   └── SecurityUtil.java
        └── config/
            ├── WebConfig.java
            └── JpaConfig.java

src/main/resources/
├── application.yaml
├── application-dev.yaml
├── application-prod.yaml
└── db/
    └── migration/                # Flyway or Liquibase
        ├── V1__init_user.sql
        └── V2__init_social_provider.sql
```

---

## 주요 기술 스택

### Backend
- **Framework**: Spring Boot 3.5.x
- **Security**: Spring Security 6.x
- **OAuth2**: Spring Security OAuth2 Client
- **JWT**: jjwt (io.jsonwebtoken)
- **Database**: PostgreSQL (H2 for dev)
- **Cache**: Redis (Lettuce)
- **ORM**: Spring Data JPA

### Build & Test
- **Build**: Gradle (Kotlin DSL)
- **Test**: JUnit 5, Mockito, TestContainers

---

## 데이터베이스 스키마

```sql
-- User 테이블
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    profile_image VARCHAR(500),
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP
);

-- Social Provider 테이블 (1:N 관계)
CREATE TABLE social_providers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    linked_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE (provider, provider_id)
);

-- User Session 테이블 (멀티 디바이스)
CREATE TABLE user_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_id VARCHAR(100) NOT NULL,
    device_name VARCHAR(100),
    user_agent TEXT,
    ip_address VARCHAR(45),
    last_accessed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE (user_id, device_id)
);
```

### Redis 데이터 구조

```
# Refresh Token
Key: RT:{userId}:{deviceId}
Value: {
  "token": "encrypted-refresh-token",
  "userAgent": "Chrome/122.0",
  "ip": "192.168.1.1",
  "createdAt": "2026-02-08T10:00:00Z"
}
TTL: 7일

# 블랙리스트 (로그아웃된 Access Token)
Key: BL:{jti}
Value: "true"
TTL: 15분 (Access Token 남은 시간)
```

---

## API 엔드포인트 설계

### 인증 API
```
POST   /api/auth/refresh         - Access Token 재발급
POST   /api/auth/logout          - 로그아웃 (현재 디바이스)
POST   /api/auth/logout-all      - 전체 디바이스 로그아웃
GET    /api/auth/sessions        - 활성 세션 조회
DELETE /api/auth/sessions/{id}   - 특정 세션 삭제
```

### OAuth2 API
```
GET    /oauth2/authorization/{provider}  - OAuth 로그인 시작 (Google, Kakao, Naver)
GET    /login/oauth2/code/{provider}     - OAuth 콜백
```

### 사용자 API
```
GET    /api/users/me             - 내 정보 조회
PUT    /api/users/me             - 내 정보 수정
DELETE /api/users/me             - 회원 탈퇴
GET    /api/users/me/providers   - 연동된 소셜 계정 조회
POST   /api/users/me/providers   - 소셜 계정 추가 연동
DELETE /api/users/me/providers/{provider} - 소셜 계정 연동 해제
```

---

## 보안 고려사항

1. **CSRF 보호**: SameSite Cookie + CSRF Token
2. **XSS 방지**: HttpOnly Cookie, Content Security Policy
3. **CORS**: 허용된 Origin만 접근
4. **Rate Limiting**: API 호출 제한 (Stage 9)
5. **PKCE**: OAuth 2.0 보안 강화 (Stage 9)
6. **Refresh Token Rotation**: 재발급 시 이전 토큰 무효화 (Stage 8)

---

## 모니터링 & 로깅

- **Application Metrics**: Spring Actuator + Prometheus
- **Logging**: Logback + ELK Stack
- **Audit Log**: 인증/인가 이벤트 기록
- **Alert**: Redis 장애, JWT 검증 실패율 모니터링

---

이 아키텍처는 Stage별로 점진적으로 완성됩니다!
