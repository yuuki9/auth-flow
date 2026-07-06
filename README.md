# base/oauth2-foundation — 소셜 로그인 → JWT

소셜 로그인(Google·Kakao·Naver)으로 JWT를 발급하고, 토큰 저장 방식 3가지를 패키지로 비교하는 브랜치입니다.  
`base/jwt-only`와 diff하면 OAuth2가 **로그인 진입점만 교체**한다는 것이 보입니다.

## 로그인 흐름

```
GET /oauth2/authorization/{provider}
  → Provider 동의 화면 → 콜백
  → CustomOAuth2UserService: DB upsert (신규 INSERT / 기존 이름·프로필 UPDATE)
  → OAuth2SuccessHandler → AuthService.issueTokens()
  → 토큰 전달 방식은 storage 패키지(아래)에 따라 다름
```

## 3가지 토큰 저장 패턴

`application.yaml`의 `spring.profiles.active` 한 줄로 전환합니다.

| 프로필 | Access Token | Refresh Token | 특징 |
|---|---|---|---|
| `cookie` (기본값) | HttpOnly 쿠키 | HttpOnly 쿠키 | XSS 방어, 현업 표준 |
| `memory` | JS 변수 (탭 닫으면 소멸) | HttpOnly 쿠키 | Silent Refresh 패턴 |
| `localstorage` | localStorage | localStorage | ⚠️ XSS 취약, 학습용 |

## 엔드포인트

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/oauth2/authorization/{provider}` | 소셜 로그인 진입 |
| `GET` | `/api/auth/me` | 내 정보 (JWT 필요) |
| `POST` | `/api/auth/refresh` | Refresh Token 재발급 |
| `POST` | `/api/auth/logout` | Redis 삭제 + 쿠키 정리 |

## 실행

```bash
docker compose up -d          # PostgreSQL + Redis
cp .env.example .env          # OAuth Client ID/Secret + JWT_SECRET 입력
./gradlew bootRun
```

Provider 콘솔에 Redirect URI 등록: `http://localhost:8080/login/oauth2/code/{google|kakao|naver}`

```bash
./gradlew test
```

## jwt-only와의 핵심 차이

```bash
git diff base/jwt-only base/oauth2-foundation
```

- `LoginController` 삭제 → `OAuth2SuccessHandler`로 대체 (로그인 진입점만 교체)
- `User` 필드 변경: `passwordHash` → `provider`, `providerId`, `profileImageUrl`
- `storage/` 패키지 추가: 패턴별 `JwtFilter` + `OAuth2SuccessHandler` + `RefreshTokenHandler`
- `AuthService`는 **두 브랜치가 동일** — 토큰 발급 로직은 로그인 방식과 무관
