# 🎯 OAuth 2.0 인증 시스템 - Stage별 학습 로드맵

> **Hybrid 전략**: Stateless JWT (Access Token) + Redis (Refresh Token)

---

## 📚 전체 Stage 개요

| Stage | 제목 | 난이도 | 소요시간 | 핵심 개념 |
|-------|------|--------|----------|-----------|
| **Stage 1** | OAuth 2.0 기본 이해 (구글 로그인) | ⭐ | 3-4시간 | OAuth 2.0 Flow |
| **Stage 2** | 다중 Provider 지원 (카카오, 네이버) | ⭐ | 2-3시간 | Provider 추상화 |
| **Stage 3** | 데이터베이스 연동 & 회원 관리 | ⭐⭐ | 4-5시간 | JPA, Domain Model |
| **Stage 4** | JWT Access Token 구현 | ⭐⭐ | 3-4시간 | JWT 생성/검증 |
| **Stage 5** | Redis 연동 준비 | ⭐⭐ | 2-3시간 | Redis 기본 |
| **Stage 6** | Refresh Token + Redis 저장 | ⭐⭐⭐ | 4-5시간 | Stateful Token |
| **Stage 7** | 로그아웃 & 토큰 무효화 | ⭐⭐⭐ | 3-4시간 | 블랙리스트 전략 |
| **Stage 8** | 멀티 디바이스 세션 관리 | ⭐⭐⭐ | 5-6시간 | 디바이스 추적 |
| **Stage 9** | 보안 강화 (PKCE, Rate Limit) | ⭐⭐⭐⭐ | 5-6시간 | 고급 보안 |
| **Stage 10** | 프로덕션 배포 준비 | ⭐⭐⭐⭐ | 6-8시간 | Docker, CI/CD |

**총 예상 시간**: 37-48시간 (약 1주일 집중 학습)

---

## 📖 Stage 1: OAuth 2.0 기본 이해 (구글 로그인)

### 🎯 목표
- OAuth 2.0 Authorization Code Grant Flow 이해
- Spring Security OAuth2 Client로 구글 로그인 구현
- 기본적인 인증 흐름 구축

### 📚 학습 내용
1. OAuth 2.0 개념 및 용어 (Authorization Server, Resource Server, Client)
2. Authorization Code Grant Flow 단계별 이해
3. Google Cloud Console에서 OAuth 2.0 클라이언트 생성
4. Spring Security OAuth2 Client 설정

### 🛠️ 구현 파일
```
src/main/java/com/auth/practice/
├── infrastructure/security/
│   ├── config/SecurityConfig.java
│   └── oauth/OAuth2AuthenticationSuccessHandler.java
└── presentation/controller/HomeController.java

src/main/resources/
├── application.yaml
└── templates/
    ├── index.html
    └── login.html
```

### ✅ 완료 조건
- [ ] Google OAuth 2.0 클라이언트 등록 완료
- [ ] 구글 로그인 버튼 클릭 시 구글 로그인 페이지로 리다이렉트
- [ ] 로그인 성공 후 사용자 이메일 화면에 표시
- [ ] 로그아웃 기능 동작 (세션 기반)

### 🧪 테스트
- 구글 계정으로 로그인 → 성공
- 로그인 후 홈페이지에서 사용자 정보 확인
- 로그아웃 → 다시 로그인 페이지로 이동

---

## 📖 Stage 2: 다중 Provider 지원 (카카오, 네이버)

### 🎯 목표
- 카카오, 네이버 OAuth 2.0 추가 연동
- Provider별 사용자 정보 응답 형식 통합 처리

### 📚 학습 내용
1. 카카오/네이버 OAuth 2.0 특성 (OpenID Connect 미지원)
2. Custom OAuth2Provider 설정 (userInfoUri, userNameAttributeName)
3. Provider별 UserInfo 응답 구조 차이 처리
4. 통합 사용자 정보 모델 설계

### 🛠️ 구현 파일
```
src/main/java/com/auth/practice/
├── domain/oauth/
│   ├── OAuth2Provider.java (Enum)
│   └── OAuth2UserInfo.java (Interface)
├── infrastructure/security/oauth/
│   ├── CustomOAuth2UserService.java
│   └── userinfo/
│       ├── OAuth2UserInfoFactory.java
│       ├── GoogleOAuth2UserInfo.java
│       ├── KakaoOAuth2UserInfo.java
│       └── NaverOAuth2UserInfo.java
└── presentation/controller/OAuth2Controller.java

src/main/resources/
└── application.yaml (provider 설정 추가)
```

### ✅ 완료 조건
- [ ] 카카오 Developers에서 애플리케이션 등록
- [ ] 네이버 Developers에서 애플리케이션 등록
- [ ] 구글/카카오/네이버 3개 Provider 로그인 버튼 생성
- [ ] 각 Provider 로그인 성공 후 통합된 형식으로 사용자 정보 표시
- [ ] Provider별로 다른 이메일, 이름, 프로필 이미지 URL 처리

### 🧪 테스트
- 구글 로그인 → 사용자 정보 표시
- 카카오 로그인 → 사용자 정보 표시
- 네이버 로그인 → 사용자 정보 표시
- 각 Provider 응답 형식이 통합되었는지 확인

### 💡 핵심 포인트
```java
// OAuth2UserInfo 인터페이스로 추상화
public interface OAuth2UserInfo {
    String getProviderId();
    String getEmail();
    String getName();
    String getProfileImage();
}

// Factory 패턴으로 Provider별 구현체 생성
public class OAuth2UserInfoFactory {
    public static OAuth2UserInfo of(String provider, Map<String, Object> attributes) {
        return switch (OAuth2Provider.valueOf(provider.toUpperCase())) {
            case GOOGLE -> new GoogleOAuth2UserInfo(attributes);
            case KAKAO -> new KakaoOAuth2UserInfo(attributes);
            case NAVER -> new NaverOAuth2UserInfo(attributes);
        };
    }
}
```

---

## 📖 Stage 3: 데이터베이스 연동 & 회원 관리

### 🎯 목표
- 소셜 로그인 사용자를 데이터베이스에 저장
- 도메인 모델 설계 (DDD 기반)
- 회원가입 자동화 (첫 로그인 시 회원 생성)

### 📚 학습 내용
1. DDD Entity 설계 (User, SocialProvider)
2. JPA 관계 매핑 (1:N)
3. 동일 이메일 처리 전략
4. 소프트 삭제 (Soft Delete) 구현

### 🛠️ 구현 파일
```
build.gradle.kts (PostgreSQL, H2 의존성 추가)

src/main/java/com/auth/practice/
├── domain/
│   └── user/
│       ├── User.java (Entity)
│       ├── SocialProvider.java (Entity)
│       ├── UserRole.java (Enum)
│       └── UserStatus.java (Enum)
├── application/service/
│   ├── UserService.java
│   └── OAuth2Service.java
├── infrastructure/persistence/
│   └── repository/
│       ├── UserRepository.java
│       └── SocialProviderRepository.java
└── infrastructure/security/oauth/
    └── CustomOAuth2UserService.java (수정: DB 저장 로직 추가)

src/main/resources/
├── application-dev.yaml (H2)
├── application-prod.yaml (PostgreSQL)
└── db/migration/
    ├── V1__create_users_table.sql
    └── V2__create_social_providers_table.sql
```

### 🗂️ 도메인 모델
```java
@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue
    private Long id;
    private String email;
    private String name;
    private String profileImage;
    @Enumerated(EnumType.STRING)
    private UserRole role;  // USER, ADMIN
    @Enumerated(EnumType.STRING)
    private UserStatus status;  // ACTIVE, INACTIVE, DELETED
    
    @OneToMany(mappedBy = "user")
    private List<SocialProvider> socialProviders = new ArrayList<>();
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;  // Soft Delete
}

@Entity
@Table(name = "social_providers")
public class SocialProvider {
    @Id @GeneratedValue
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Enumerated(EnumType.STRING)
    private OAuth2Provider provider;  // GOOGLE, KAKAO, NAVER
    
    private String providerId;  // Provider에서 제공하는 고유 ID
    private LocalDateTime linkedAt;
}
```

### ✅ 완료 조건
- [ ] H2 Database 연결 및 테이블 자동 생성 확인
- [ ] 구글 로그인 시 users, social_providers 테이블에 데이터 저장
- [ ] 동일한 이메일로 다른 Provider 로그인 시 social_providers만 추가
- [ ] 로그인 후 DB에서 사용자 정보 조회하여 표시
- [ ] H2 Console에서 데이터 확인 가능

### 🧪 테스트
- 구글로 첫 로그인 → users, social_providers 생성
- 같은 이메일로 카카오 로그인 → social_providers만 추가
- H2 Console 접속하여 데이터 확인
- 로그아웃 후 재로그인 시 기존 데이터 사용

### 💡 핵심 로직
```java
@Service
public class OAuth2Service {
    public User saveOrUpdate(OAuth2UserInfo userInfo) {
        // 1. Provider ID로 기존 연동 확인
        Optional<SocialProvider> socialProvider = 
            socialProviderRepository.findByProviderAndProviderId(
                userInfo.getProvider(), userInfo.getProviderId());
        
        if (socialProvider.isPresent()) {
            // 기존 사용자 반환
            return socialProvider.get().getUser();
        }
        
        // 2. 이메일로 기존 사용자 확인
        User user = userRepository.findByEmail(userInfo.getEmail())
            .orElseGet(() -> createNewUser(userInfo));
        
        // 3. 새로운 소셜 Provider 연동
        linkSocialProvider(user, userInfo);
        
        return user;
    }
}
```

---

## 📖 Stage 4: JWT Access Token 구현 (Stateless)

### 🎯 목표
- OAuth 로그인 성공 후 JWT Access Token 발급
- Stateless 인증 구현 (세션 제거)
- JWT 검증 필터 추가

### 📚 학습 내용
1. JWT 구조 (Header, Payload, Signature)
2. JWT 생성 및 서명 (HMAC-SHA256)
3. JWT 검증 및 파싱
4. Spring Security Filter Chain에 JWT 필터 추가
5. Stateless Session 설정

### 🛠️ 구현 파일
```
build.gradle.kts (jjwt 의존성 추가)

src/main/java/com/auth/practice/
├── application/service/
│   └── JwtService.java
├── infrastructure/security/
│   ├── jwt/
│   │   ├── JwtTokenProvider.java
│   │   ├── JwtAuthenticationFilter.java
│   │   └── JwtExceptionFilter.java
│   ├── oauth/
│   │   └── OAuth2AuthenticationSuccessHandler.java (수정: JWT 발급)
│   └── config/
│       ├── SecurityConfig.java (수정: Stateless)
│       └── JwtConfig.java
└── presentation/dto/response/
    └── TokenResponse.java

src/main/resources/
└── application.yaml (JWT 설정 추가)
```

### 🔐 JWT 구조
```
Access Token (15분):
{
  "sub": "user123",                    // 사용자 ID
  "email": "user@example.com",
  "name": "홍길동",
  "role": "USER",
  "iat": 1234567890,                   // 발급 시간
  "exp": 1234568790,                   // 만료 시간 (15분 후)
  "jti": "uuid-1234-5678-..."          // JWT ID (블랙리스트용)
}
```

### ✅ 완료 조건
- [ ] OAuth 로그인 성공 시 JWT Access Token 발급
- [ ] 토큰을 프론트엔드로 전달 (쿼리 파라미터 or JSON 응답)
- [ ] API 요청 시 `Authorization: Bearer {token}` 헤더 검증
- [ ] 유효한 토큰: 요청 성공
- [ ] 만료된 토큰: 401 Unauthorized
- [ ] 잘못된 토큰: 401 Unauthorized

### 🧪 테스트
1. 구글 로그인 → JWT Access Token 발급 확인
2. Postman으로 `/api/users/me` 호출 (토큰 포함) → 성공
3. 토큰 없이 호출 → 401 에러
4. 만료된 토큰으로 호출 → 401 에러
5. jwt.io에서 토큰 디코딩하여 Payload 확인

### 💡 핵심 코드
```java
@Component
public class JwtTokenProvider {
    private final String SECRET_KEY;
    private final long ACCESS_TOKEN_EXPIRE_TIME = 15 * 60 * 1000; // 15분
    
    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ACCESS_TOKEN_EXPIRE_TIME);
        
        return Jwts.builder()
            .setSubject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("name", user.getName())
            .claim("role", user.getRole())
            .setIssuedAt(now)
            .setExpiration(expiry)
            .setId(UUID.randomUUID().toString())  // JTI
            .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
            .compact();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

---

## 📖 Stage 5: Redis 연동 준비

### 🎯 목표
- Redis 설치 및 연결
- Spring Data Redis 설정
- 간단한 캐시 기능 테스트

### 📚 학습 내용
1. Redis 설치 (Docker 권장)
2. Spring Data Redis 설정
3. RedisTemplate vs StringRedisTemplate
4. Redis 기본 명령어 (SET, GET, EXPIRE)

### 🛠️ 구현 파일
```
docker-compose.yaml (Redis 추가)

build.gradle.kts (spring-boot-starter-data-redis 추가)

src/main/java/com/auth/practice/
├── infrastructure/redis/
│   ├── RedisConfig.java
│   └── RedisService.java
└── application/service/
    └── CacheTestService.java (테스트용)

src/main/resources/
└── application.yaml (Redis 설정 추가)
```

### 🐳 Docker Compose
```yaml
version: '3.8'
services:
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    command: redis-server --appendonly yes
    
volumes:
  redis-data:
```

### ✅ 완료 조건
- [ ] Docker로 Redis 실행 중
- [ ] Spring Boot 애플리케이션에서 Redis 연결 성공
- [ ] RedisTemplate을 통해 데이터 저장/조회 가능
- [ ] Redis CLI로 저장된 데이터 확인 가능

### 🧪 테스트
```bash
# Redis 실행
docker-compose up -d redis

# Redis CLI 접속
docker exec -it <container-id> redis-cli

# 테스트
> SET test "Hello Redis"
> GET test
> EXPIRE test 60
> TTL test
```

### 💡 핵심 설정
```yaml
# application.yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password:  # 필요시
      lettuce:
        pool:
          max-active: 10
          max-idle: 5
          min-idle: 2
```

---

## 📖 Stage 6: Refresh Token + Redis 저장 (Hybrid 핵심!)

### 🎯 목표
- Refresh Token 생성 및 Redis 저장
- Access Token 재발급 API 구현
- Stateless (Access) + Stateful (Refresh) Hybrid 전략 완성

### 📚 학습 내용
1. Refresh Token의 역할과 필요성
2. Redis TTL을 활용한 자동 만료
3. Refresh Token으로 Access Token 재발급 흐름
4. HttpOnly Cookie로 Refresh Token 전달

### 🛠️ 구현 파일
```
src/main/java/com/auth/practice/
├── domain/auth/
│   ├── RefreshToken.java (VO)
│   └── DeviceInfo.java (VO)
├── application/service/
│   └── TokenService.java
├── infrastructure/
│   ├── redis/
│   │   └── RedisTokenRepository.java
│   ├── security/oauth/
│   │   └── OAuth2AuthenticationSuccessHandler.java (수정: Refresh Token 발급)
│   └── common/util/
│       └── CookieUtil.java
└── presentation/
    ├── controller/
    │   └── AuthController.java
    └── dto/
        ├── request/TokenRefreshRequest.java
        └── response/TokenResponse.java

src/main/resources/
└── application.yaml (토큰 만료 시간 설정)
```

### 🔐 Refresh Token 구조
```
Redis Key: RT:{userId}:{deviceId}
Value (JSON):
{
  "token": "encrypted-refresh-token-string",
  "userId": 123,
  "deviceId": "uuid-device-123",
  "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)...",
  "ip": "192.168.1.1",
  "createdAt": "2026-02-08T10:00:00Z"
}
TTL: 604800초 (7일)
```

### 📡 API 엔드포인트
```
POST /api/auth/refresh
Request:
  - Cookie: refreshToken={token}
  - Body: { "deviceId": "uuid-device-123" }
  
Response:
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 900  // 15분 (초)
}
```

### ✅ 완료 조건
- [ ] OAuth 로그인 성공 시 Access Token + Refresh Token 발급
- [ ] Refresh Token은 Redis에 저장 (TTL 7일)
- [ ] Refresh Token은 HttpOnly Cookie로 전달
- [ ] `/api/auth/refresh` API로 Access Token 재발급 성공
- [ ] Refresh Token이 없거나 만료된 경우 401 에러

### 🧪 테스트 시나리오
1. 구글 로그인 → Access Token + Refresh Token 발급
2. Redis에서 `KEYS RT:*` 명령어로 저장 확인
3. 15분 대기 (또는 강제로 만료된 Access Token 생성)
4. 만료된 Access Token으로 `/api/users/me` 호출 → 401
5. `/api/auth/refresh` 호출 → 새로운 Access Token 발급
6. 새 Access Token으로 `/api/users/me` 호출 → 성공

### 💡 핵심 로직
```java
@Service
public class TokenService {
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTokenRepository redisTokenRepository;
    
    public TokenResponse refresh(String refreshToken, String deviceId) {
        // 1. Redis에서 Refresh Token 조회
        RefreshToken storedToken = redisTokenRepository
            .findByUserIdAndDeviceId(userId, deviceId)
            .orElseThrow(() -> new AuthException("Refresh token not found"));
        
        // 2. 토큰 검증
        if (!storedToken.getToken().equals(refreshToken)) {
            throw new AuthException("Invalid refresh token");
        }
        
        // 3. 새로운 Access Token 발급
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        
        return new TokenResponse(newAccessToken, "Bearer", 900);
    }
}
```

```java
@Repository
public class RedisTokenRepository {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String PREFIX = "RT:";
    private static final long REFRESH_TOKEN_TTL = 7 * 24 * 60 * 60; // 7일
    
    public void save(RefreshToken refreshToken) {
        String key = PREFIX + refreshToken.getUserId() + ":" + refreshToken.getDeviceId();
        redisTemplate.opsForValue().set(key, refreshToken, REFRESH_TOKEN_TTL, TimeUnit.SECONDS);
    }
    
    public Optional<RefreshToken> findByUserIdAndDeviceId(Long userId, String deviceId) {
        String key = PREFIX + userId + ":" + deviceId;
        RefreshToken token = (RefreshToken) redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(token);
    }
}
```

---

## 📖 Stage 7: 로그아웃 & 토큰 무효화

### 🎯 목표
- 로그아웃 시 Refresh Token 삭제
- Access Token 블랙리스트 구현 (선택)
- 강제 로그아웃 기능

### 📚 학습 내용
1. Stateless JWT의 로그아웃 문제점
2. Refresh Token 삭제로 재발급 차단
3. Access Token 블랙리스트 전략 (JTI 활용)
4. 로그아웃 vs 강제 로그아웃

### 🛠️ 구현 파일
```
src/main/java/com/auth/practice/
├── application/service/
│   └── AuthService.java
├── infrastructure/
│   ├── redis/
│   │   └── RedisTokenRepository.java (수정: 블랙리스트 추가)
│   └── security/jwt/
│       └── JwtAuthenticationFilter.java (수정: 블랙리스트 확인)
└── presentation/
    ├── controller/
    │   └── AuthController.java (로그아웃 API 추가)
    └── dto/request/
        └── LogoutRequest.java
```

### 🗑️ 블랙리스트 구조
```
Redis Key: BL:{jti}
Value: "true"
TTL: Access Token 남은 만료 시간 (최대 15분)

예시:
Key: BL:uuid-1234-5678-9abc
Value: "true"
TTL: 850초 (14분 10초 남음)
```

### 📡 API 엔드포인트
```
POST /api/auth/logout
Request:
  - Header: Authorization: Bearer {accessToken}
  - Body: { "deviceId": "uuid-device-123" }
  
Response:
{
  "message": "로그아웃되었습니다."
}
```

### ✅ 완료 조건
- [ ] 로그아웃 API 호출 시 Redis에서 Refresh Token 삭제
- [ ] (선택) Access Token의 JTI를 블랙리스트에 추가
- [ ] 블랙리스트에 등록된 Access Token으로 API 호출 시 401 에러
- [ ] 로그아웃 후 `/api/auth/refresh` 호출 시 401 에러

### 🧪 테스트 시나리오
1. 로그인 → Access Token + Refresh Token 발급
2. `/api/users/me` 호출 → 성공
3. 로그아웃 API 호출
4. Redis에서 `GET RT:{userId}:{deviceId}` → null (삭제 확인)
5. 같은 Access Token으로 `/api/users/me` 호출 → 401 (블랙리스트)
6. `/api/auth/refresh` 호출 → 401 (Refresh Token 없음)

### 💡 핵심 로직
```java
@Service
public class AuthService {
    public void logout(String accessToken, String deviceId) {
        // 1. JWT에서 userId, jti 추출
        Claims claims = jwtTokenProvider.parseClaims(accessToken);
        Long userId = Long.parseLong(claims.getSubject());
        String jti = claims.getId();
        
        // 2. Redis에서 Refresh Token 삭제
        redisTokenRepository.deleteByUserIdAndDeviceId(userId, deviceId);
        
        // 3. Access Token 블랙리스트 등록
        long remainingTime = claims.getExpiration().getTime() - System.currentTimeMillis();
        if (remainingTime > 0) {
            redisTokenRepository.addToBlacklist(jti, remainingTime);
        }
    }
}

// JwtAuthenticationFilter에서 블랙리스트 확인
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        String token = extractToken(request);
        
        if (token != null && jwtTokenProvider.validateToken(token)) {
            String jti = jwtTokenProvider.getJti(token);
            
            // 블랙리스트 확인
            if (redisTokenRepository.isBlacklisted(jti)) {
                throw new AuthException("Token has been revoked");
            }
            
            // 인증 처리
            Authentication auth = getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        
        filterChain.doFilter(request, response);
    }
}
```

---

## 📖 Stage 8: 멀티 디바이스 세션 관리

### 🎯 목표
- 사용자의 여러 디바이스(PC, 모바일, 태블릿) 동시 로그인 지원
- 디바이스별 세션 조회 및 관리
- 특정 디바이스만 로그아웃 (또는 전체 로그아웃)

### 📚 학습 내용
1. Device Fingerprinting
2. 디바이스별 Refresh Token 분리 저장
3. Refresh Token Rotation 전략
4. 동시 로그인 수 제한

### 🛠️ 구현 파일
```
src/main/java/com/auth/practice/
├── domain/auth/
│   ├── UserSession.java (Entity)
│   └── DeviceInfo.java (VO)
├── application/service/
│   ├── SessionService.java
│   └── DeviceService.java
├── infrastructure/
│   ├── persistence/repository/
│   │   └── UserSessionRepository.java
│   ├── redis/
│   │   └── RedisTokenRepository.java (수정: 디바이스별 관리)
│   └── common/util/
│       └── DeviceUtil.java
└── presentation/
    ├── controller/
    │   └── SessionController.java
    └── dto/response/
        └── SessionResponse.java

src/main/resources/db/migration/
└── V3__create_user_sessions_table.sql
```

### 🗂️ 도메인 모델
```java
@Entity
@Table(name = "user_sessions")
public class UserSession {
    @Id @GeneratedValue
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;
    
    private String deviceId;         // UUID
    private String deviceName;       // "Chrome on Windows"
    private String userAgent;
    private String ipAddress;
    private LocalDateTime lastAccessedAt;
    private LocalDateTime createdAt;
}
```

### 📡 API 엔드포인트
```
GET    /api/auth/sessions           - 활성 세션 목록 조회
DELETE /api/auth/sessions/{id}      - 특정 세션 삭제
POST   /api/auth/logout-all         - 모든 디바이스 로그아웃
```

### 📱 활성 세션 응답
```json
{
  "sessions": [
    {
      "id": 1,
      "deviceId": "uuid-device-123",
      "deviceName": "Chrome on Windows",
      "ipAddress": "192.168.1.100",
      "lastAccessedAt": "2026-02-08T15:30:00Z",
      "isCurrent": true
    },
    {
      "id": 2,
      "deviceId": "uuid-device-456",
      "deviceName": "Safari on iPhone",
      "ipAddress": "211.234.567.89",
      "lastAccessedAt": "2026-02-08T10:15:00Z",
      "isCurrent": false
    }
  ]
}
```

### ✅ 완료 조건
- [ ] 로그인 시 디바이스 정보 자동 감지 (User-Agent, IP)
- [ ] 동일 사용자가 여러 디바이스에서 동시 로그인 가능
- [ ] `/api/auth/sessions` API로 모든 활성 세션 조회 가능
- [ ] 특정 세션만 삭제 가능 (다른 디바이스는 유지)
- [ ] `/api/auth/logout-all` 호출 시 모든 디바이스 로그아웃
- [ ] 최대 동시 로그인 수 제한 (예: 5대)

### 🧪 테스트 시나리오
1. Chrome에서 로그인 → deviceId-1 생성
2. Safari에서 같은 계정 로그인 → deviceId-2 생성
3. `/api/auth/sessions` 호출 → 2개 세션 표시
4. Chrome에서 로그아웃 → deviceId-1 삭제
5. `/api/auth/sessions` 호출 → 1개 세션 표시 (Safari만)
6. Safari에서 `/api/auth/logout-all` 호출 → 모든 세션 삭제

### 💡 핵심 로직
```java
@Service
public class SessionService {
    private static final int MAX_DEVICES = 5;
    
    public void createSession(User user, DeviceInfo deviceInfo) {
        // 1. 기존 세션 개수 확인
        List<UserSession> sessions = userSessionRepository.findByUser(user);
        
        if (sessions.size() >= MAX_DEVICES) {
            // 가장 오래된 세션 삭제
            UserSession oldest = sessions.stream()
                .min(Comparator.comparing(UserSession::getLastAccessedAt))
                .orElseThrow();
            deleteSession(oldest);
        }
        
        // 2. 새 세션 생성
        UserSession session = UserSession.builder()
            .user(user)
            .deviceId(deviceInfo.getDeviceId())
            .deviceName(deviceInfo.getDeviceName())
            .userAgent(deviceInfo.getUserAgent())
            .ipAddress(deviceInfo.getIpAddress())
            .createdAt(LocalDateTime.now())
            .lastAccessedAt(LocalDateTime.now())
            .build();
        
        userSessionRepository.save(session);
    }
    
    public void logoutAll(Long userId) {
        // 1. DB에서 모든 세션 조회
        List<UserSession> sessions = userSessionRepository.findByUserId(userId);
        
        // 2. Redis에서 모든 Refresh Token 삭제
        sessions.forEach(session -> {
            redisTokenRepository.deleteByUserIdAndDeviceId(
                userId, session.getDeviceId());
        });
        
        // 3. DB에서 세션 삭제
        userSessionRepository.deleteAll(sessions);
    }
}
```

### 🔒 Refresh Token Rotation
```java
// Refresh Token 재발급 시 이전 토큰 무효화
public TokenResponse refresh(String oldRefreshToken, String deviceId) {
    // 1. 기존 토큰 검증 및 조회
    RefreshToken storedToken = redisTokenRepository
        .findByUserIdAndDeviceId(userId, deviceId)
        .orElseThrow(() -> new AuthException("Invalid refresh token"));
    
    // 2. 토큰 일치 확인
    if (!storedToken.getToken().equals(oldRefreshToken)) {
        // Refresh Token Reuse 감지 → 보안 위협!
        // 해당 사용자의 모든 세션 삭제
        sessionService.logoutAll(userId);
        throw new AuthException("Refresh token reuse detected");
    }
    
    // 3. 새로운 Access Token + Refresh Token 발급
    String newAccessToken = jwtTokenProvider.generateAccessToken(user);
    String newRefreshToken = generateRefreshToken();
    
    // 4. Redis에 새 Refresh Token 저장 (기존 토큰 덮어쓰기)
    redisTokenRepository.save(RefreshToken.builder()
        .userId(userId)
        .deviceId(deviceId)
        .token(newRefreshToken)
        .build());
    
    return new TokenResponse(newAccessToken, newRefreshToken);
}
```

---

## 📖 Stage 9: 보안 강화 (PKCE, Rate Limit, CSRF)

### 🎯 목표
- PKCE (Proof Key for Code Exchange) 적용
- API Rate Limiting 구현
- CSRF 보호 강화
- 보안 헤더 설정

### 📚 학습 내용
1. PKCE 개념 및 필요성 (Authorization Code 탈취 방지)
2. Rate Limiting 전략 (Redis 기반)
3. CSRF 토큰 vs SameSite Cookie
4. Security Headers (CSP, HSTS, X-Frame-Options)

### 🛠️ 구현 파일
```
build.gradle.kts (Bucket4j 의존성 추가)

src/main/java/com/auth/practice/
├── infrastructure/
│   ├── security/
│   │   ├── filter/
│   │   │   ├── RateLimitFilter.java
│   │   │   └── CsrfCookieFilter.java
│   │   ├── oauth/
│   │   │   └── PkceOAuth2AuthorizationRequestResolver.java
│   │   └── config/
│   │       ├── SecurityConfig.java (보안 헤더 추가)
│   │       └── CorsConfig.java
│   └── redis/
│       └── RateLimiter.java
└── common/
    ├── exception/
    │   └── RateLimitExceededException.java
    └── config/
        └── SecurityHeadersConfig.java

src/main/resources/
└── application.yaml (Rate Limit 설정)
```

### 🔐 PKCE Flow
```
1. 클라이언트: Code Verifier 생성 (랜덤 문자열)
   code_verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"

2. 클라이언트: Code Challenge 생성
   code_challenge = BASE64URL(SHA256(code_verifier))
                 = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"

3. Authorization 요청 시 code_challenge 포함
   GET /oauth2/authorization/google?
       code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM
       &code_challenge_method=S256

4. Token 요청 시 code_verifier 포함
   POST /oauth2/token
   Body: {
     "code": "authorization_code",
     "code_verifier": "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
   }

5. Authorization Server에서 검증
   if (SHA256(code_verifier) == code_challenge) → 성공
```

### ⏱️ Rate Limiting
```
Redis Key: RL:{ip}:{endpoint}
Value: 요청 횟수
TTL: 60초

예시:
Key: RL:192.168.1.100:/api/auth/login
Value: 5
TTL: 60초

제한: 1분당 10회
```

### 🛡️ 보안 헤더
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) {
    http
        .headers(headers -> headers
            .contentSecurityPolicy(csp -> 
                csp.policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'"))
            .httpStrictTransportSecurity(hsts -> 
                hsts.maxAgeInSeconds(31536000).includeSubDomains(true))
            .frameOptions(FrameOptionsConfig::deny)
            .xssProtection(xss -> xss.block(true))
        );
    
    return http.build();
}
```

### ✅ 완료 조건
- [ ] OAuth 로그인 시 PKCE 적용 확인
- [ ] 로그인 API에 Rate Limit 적용 (1분당 5회)
- [ ] 제한 초과 시 429 Too Many Requests 에러
- [ ] CSRF 토큰 쿠키 자동 설정
- [ ] 보안 헤더 응답에 포함 확인

### 🧪 테스트
1. Postman으로 로그인 API 1분에 6회 호출 → 6번째 요청 429 에러
2. 1분 대기 후 다시 호출 → 성공
3. 브라우저 개발자 도구에서 응답 헤더 확인
   - `Content-Security-Policy`
   - `Strict-Transport-Security`
   - `X-Frame-Options: DENY`

### 💡 핵심 구현
```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final RateLimiter rateLimiter;
    
    // 엔드포인트별 제한 설정
    private static final Map<String, Integer> RATE_LIMITS = Map.of(
        "/api/auth/login", 5,      // 1분당 5회
        "/api/auth/refresh", 10,   // 1분당 10회
        "/api/auth/logout", 20     // 1분당 20회
    );
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        String endpoint = request.getRequestURI();
        String clientIp = getClientIP(request);
        
        Integer limit = RATE_LIMITS.get(endpoint);
        if (limit != null) {
            if (!rateLimiter.tryConsume(clientIp, endpoint, limit)) {
                throw new RateLimitExceededException(
                    "Too many requests. Please try again later.");
            }
        }
        
        filterChain.doFilter(request, response);
    }
}

@Service
public class RateLimiter {
    private final RedisTemplate<String, String> redisTemplate;
    private static final int WINDOW_SECONDS = 60;
    
    public boolean tryConsume(String ip, String endpoint, int limit) {
        String key = "RL:" + ip + ":" + endpoint;
        
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, WINDOW_SECONDS, TimeUnit.SECONDS);
        }
        
        return count <= limit;
    }
}
```

---

## 📖 Stage 10: 프로덕션 배포 준비

### 🎯 목표
- 환경별 설정 분리 (dev, staging, prod)
- Docker 컨테이너화
- CI/CD 파이프라인 구축
- 모니터링 및 로깅 설정

### 📚 학습 내용
1. Spring Profile 활용
2. Docker Multi-stage Build
3. GitHub Actions CI/CD
4. Prometheus + Grafana 모니터링
5. ELK Stack 로깅

### 🛠️ 구현 파일
```
프로젝트 루트/
├── Dockerfile
├── docker-compose.yaml (전체 스택)
├── .github/workflows/
│   ├── ci.yaml
│   └── cd.yaml
├── nginx/
│   └── nginx.conf
├── monitoring/
│   ├── prometheus.yaml
│   └── grafana/
│       └── dashboards/
└── scripts/
    ├── deploy.sh
    └── health-check.sh

src/main/resources/
├── application.yaml
├── application-dev.yaml
├── application-staging.yaml
├── application-prod.yaml
└── logback-spring.xml
```

### 🐳 Dockerfile (Multi-stage)
```dockerfile
# Build Stage
FROM gradle:8-jdk17 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src
RUN gradle bootJar --no-daemon

# Runtime Stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# 비root 사용자로 실행
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=${SPRING_PROFILE}", "app.jar"]
```

### 🐳 Docker Compose (전체 스택)
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILE=prod
      - DB_HOST=postgres
      - REDIS_HOST=redis
    depends_on:
      - postgres
      - redis
    restart: unless-stopped
  
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: auth_db
      POSTGRES_USER: auth_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres-data:/var/lib/postgresql/data
    restart: unless-stopped
  
  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis-data:/data
    restart: unless-stopped
  
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf
      - ./ssl:/etc/nginx/ssl
    depends_on:
      - app
    restart: unless-stopped
  
  prometheus:
    image: prom/prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yaml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    restart: unless-stopped
  
  grafana:
    image: grafana/grafana
    ports:
      - "3000:3000"
    volumes:
      - grafana-data:/var/lib/grafana
      - ./monitoring/grafana:/etc/grafana/provisioning
    restart: unless-stopped

volumes:
  postgres-data:
  redis-data:
  prometheus-data:
  grafana-data:
```

### 🔄 GitHub Actions CI/CD
```yaml
# .github/workflows/cd.yaml
name: Deploy to Production

on:
  push:
    branches: [ main ]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Build with Gradle
        run: ./gradlew bootJar
      
      - name: Run tests
        run: ./gradlew test
      
      - name: Build Docker image
        run: docker build -t auth-guide:latest .
      
      - name: Push to Docker Hub
        run: |
          echo ${{ secrets.DOCKER_PASSWORD }} | docker login -u ${{ secrets.DOCKER_USERNAME }} --password-stdin
          docker tag auth-guide:latest ${{ secrets.DOCKER_USERNAME }}/auth-guide:latest
          docker push ${{ secrets.DOCKER_USERNAME }}/auth-guide:latest
      
      - name: Deploy to server
        uses: appleboy/ssh-action@master
        with:
          host: ${{ secrets.SERVER_HOST }}
          username: ${{ secrets.SERVER_USER }}
          key: ${{ secrets.SSH_PRIVATE_KEY }}
          script: |
            cd /app
            docker-compose pull
            docker-compose up -d
            docker-compose exec app ./scripts/health-check.sh
```

### 📊 Spring Actuator + Prometheus
```yaml
# application-prod.yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
```

### 📝 로깅 설정
```xml
<!-- logback-spring.xml -->
<configuration>
    <springProfile name="prod">
        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>/var/log/auth-guide/application.log</file>
            <encoder>
                <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>/var/log/auth-guide/application.%d{yyyy-MM-dd}.log</fileNamePattern>
                <maxHistory>30</maxHistory>
            </rollingPolicy>
        </appender>
        
        <root level="INFO">
            <appender-ref ref="FILE" />
        </root>
    </springProfile>
</configuration>
```

### ✅ 완료 조건
- [ ] 환경별 설정 파일 분리 (dev, staging, prod)
- [ ] Docker 이미지 빌드 성공
- [ ] docker-compose로 전체 스택 실행 성공
- [ ] GitHub Actions CI/CD 파이프라인 동작
- [ ] Prometheus에서 메트릭 수집 확인
- [ ] Grafana 대시보드에서 모니터링 확인
- [ ] Nginx HTTPS 설정 완료

### 🧪 테스트
```bash
# 로컬 Docker 빌드 테스트
docker build -t auth-guide:test .
docker run -p 8080:8080 -e SPRING_PROFILE=dev auth-guide:test

# 전체 스택 실행
docker-compose up -d

# 헬스 체크
curl http://localhost:8080/actuator/health

# Prometheus 메트릭
curl http://localhost:8080/actuator/prometheus

# Grafana 접속
open http://localhost:3000
```

### 🎯 프로덕션 체크리스트
- [ ] 민감 정보 환경 변수로 분리 (.env)
- [ ] HTTPS 적용 (Let's Encrypt)
- [ ] 데이터베이스 백업 자동화
- [ ] Redis persistence 설정 (AOF)
- [ ] 로그 로테이션 설정
- [ ] 에러 알림 (Slack, Email)
- [ ] API 문서 자동화 (Swagger/OpenAPI)
- [ ] 부하 테스트 (JMeter, Gatling)

---

## 🎓 학습 완료 후 추가 학습 권장 사항

### 고급 주제
1. **OAuth 2.1 업데이트 사항** - PKCE 필수화, Implicit Flow 폐기
2. **OpenID Connect** - ID Token, UserInfo Endpoint
3. **SSO (Single Sign-On)** - 여러 서비스 간 통합 인증
4. **마이크로서비스 환경** - API Gateway + JWT 전파
5. **FIDO2/WebAuthn** - 생체 인증, 패스키

### 성능 최적화
1. **JWT 캐싱** - 자주 사용되는 사용자 정보 Redis 캐싱
2. **Connection Pooling** - HikariCP 튜닝
3. **Redis Cluster** - 고가용성 확보

### 보안 강화
1. **2FA (Two-Factor Authentication)** - TOTP, SMS
2. **Anomaly Detection** - 비정상 로그인 패턴 감지
3. **Penetration Testing** - OWASP Top 10 점검

---

## 📚 참고 자료

### 공식 문서
- [Spring Security OAuth2](https://spring.io/projects/spring-security-oauth)
- [RFC 6749 - OAuth 2.0](https://datatracker.ietf.org/doc/html/rfc6749)
- [OpenID Connect Core](https://openid.net/specs/openid-connect-core-1_0.html)

### Provider 문서
- [Google OAuth 2.0](https://developers.google.com/identity/protocols/oauth2)
- [Kakao Login](https://developers.kakao.com/docs/latest/ko/kakaologin/common)
- [Naver Login](https://developers.naver.com/docs/login/overview/)

---

**이 로드맵을 완료하면 실무 수준의 인증 시스템을 구축할 수 있습니다!** 🚀

각 Stage별 상세 구현 가이드는 별도 문서로 제공됩니다.
