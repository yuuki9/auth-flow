# base/jwt-only — ID/PW 로그인 → JWT

ID/PW 로그인 이후 JWT Access Token + Refresh Token을 발급·검증·갱신하는 흐름에 집중하는 브랜치입니다.  
OAuth2는 없습니다. JWT lifecycle 자체를 먼저 익히는 것이 목적입니다.

## 로그인 흐름

```
POST /api/auth/login { email, password }
  → LoginService: BCrypt 검증
  → AuthService.issueTokens()
      ├─ JwtProvider: Access Token (15분)
      ├─ JwtProvider: Refresh Token (7일)
      └─ Redis에 Refresh Token 저장
  → 응답 body: { accessToken, refreshToken, expiresInMs }

이후 API 요청: Authorization: Bearer {accessToken}
```

## 엔드포인트

| Method | Path | 설명 |
|---|---|---|
| `POST` | `/api/auth/login` | 로그인 → 토큰 발급 |
| `GET` | `/api/auth/me` | 내 정보 (JWT 필요) |
| `POST` | `/api/auth/refresh` | Refresh Token으로 재발급 (Rotation) |
| `POST` | `/api/auth/logout` | Redis에서 Refresh Token 삭제 |

## 실행

```bash
docker compose up -d          # PostgreSQL + Redis
cp .env.example .env          # JWT_SECRET 등 입력
./gradlew bootRun
```

앱 시작 시 테스트 계정이 자동 생성됩니다: `test@example.com` / `password123`

```bash
./gradlew test
```

## 읽을 파일 순서

1. `domain/user/User.java` — 필드 구조
2. `infrastructure/security/jwt/JwtProvider.java` — 토큰 생성·검증
3. `application/auth/AuthService.java` — 토큰 발급·Rotation·logout
4. `infrastructure/security/jwt/JwtAuthenticationFilter.java` — 매 요청 JWT 검증
5. `infrastructure/security/config/SecurityConfig.java` — Stateless 설정
6. `presentation/controller/LoginController.java` — 로그인 엔드포인트
7. `presentation/controller/AuthController.java` — me·refresh·logout

## 다음 단계

```bash
git checkout base/oauth2-foundation
git diff base/jwt-only base/oauth2-foundation  # OAuth2가 로그인 진입점만 교체함을 확인
```
