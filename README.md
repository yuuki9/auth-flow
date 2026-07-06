# base/oauth2-foundation — 소셜 로그인 → JWT

소셜 로그인(Google·Kakao·Naver)으로 JWT를 발급하고, 토큰 저장 방식 3가지를 패키지로 비교하는 브랜치입니다.  
`base/jwt-only`와 diff하면 OAuth2가 **로그인 진입점만 교체**한다는 것이 보입니다.

---

## 로그인 흐름

```
클라이언트              서버                  Provider        Redis
     │                   │                      │               │
     │  [소셜 로그인]     │                      │               │
     │─ GET /oauth2/     │                      │               │
     │  authorization/   │                      │               │
     │  {provider} ────>│                      │               │
     │                   │──── 리다이렉트 ──────>│               │
     │                   │                      │ 동의 화면     │
     │                   │<─── code ────────────│               │
     │                   │                      │               │
     │                   │ CustomOAuth2UserService               │
     │                   │ DB upsert                             │
     │                   │  신규: INSERT                        │
     │                   │  기존: 이름·프로필 UPDATE            │
     │                   │                      │               │
     │                   │ OAuth2SuccessHandler                  │
     │                   │ AuthService.issueTokens()             │
     │                   │ AT 생성 (15분)                       │
     │                   │ RT 생성 (7일)                        │
     │                   │──────────────── save(userId, RT) ───>│
     │                   │                      │               │
     │  [패턴별 토큰 전달 방식]                                  │
     │                   │                      │               │
     │  cookie ←── Set-Cookie: access_token (HttpOnly, 15분)   │
     │           ←── Set-Cookie: refresh_token (HttpOnly, 7일) │
     │                   │                      │               │
     │  memory ←── {accessToken} (body)                        │
     │           ←── Set-Cookie: refresh_token (HttpOnly, 7일) │
     │                   │                      │               │
     │  localstorage ←── redirect #access_token&refresh_token  │
     │              (⚠️ XSS 취약 — 학습 목적)                   │
     │                   │                      │               │
     │  [API 요청 — 패턴 무관]                                   │
     │─ GET /api/auth/me>│                      │               │
     │                   │ JwtAuthenticationFilter               │
     │                   │ AT 검증 → userId 추출                 │
     │<── {id,name,role} ─│                      │               │
     │                   │                      │               │
     │  [토큰 갱신 — 패턴별]                                     │
     │  cookie    ─ POST /api/auth/refresh (쿠키 자동 전송)     │
     │  memory    ─ POST /api/auth/refresh (쿠키 자동 전송)     │
     │  localstorage ─ POST /api/auth/refresh {refreshToken}   │
     │                   │──── findByUserId ────────────────────>│
     │                   │<─── 저장된 RT ────────────────────────│
     │                   │ RTR: 기존 삭제 → 새 AT + RT 발급     │
     │<── 새 토큰 (패턴별 방식으로 전달)                         │
```

---

## 3가지 토큰 저장 패턴

`application.yaml`의 `spring.profiles.active` 한 줄로 전환합니다.

| 프로필 | Access Token | Refresh Token | 보안 수준 |
|---|---|---|---|
| `cookie` (기본값) | HttpOnly 쿠키 | HttpOnly 쿠키 | 높음 — XSS 방어, 현업 표준 |
| `memory` | JS 변수 (탭 닫으면 소멸) | HttpOnly 쿠키 | 높음 — Silent Refresh 패턴 |
| `localstorage` | localStorage | localStorage | ⚠️ 낮음 — XSS 취약, 학습용 |

각 패턴은 `infrastructure/security/storage/{cookie|memory|localstorage}/` 패키지로 분리되어 있고  
`@Profile` 어노테이션으로 Spring이 활성 프로필에 맞는 구현체를 자동 선택합니다.

---

## 엔드포인트

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/oauth2/authorization/{provider}` | 소셜 로그인 진입 |
| `GET` | `/api/auth/me` | 내 정보 (AT 필요) |
| `POST` | `/api/auth/refresh` | RT → 새 AT + RT |
| `POST` | `/api/auth/logout` | Redis RT 삭제 + 쿠키 정리 |

---

## 실행

```bash
docker compose up -d
cp .env.example .env          # OAuth Client ID/Secret + JWT_SECRET 입력
./gradlew bootRun
```

Provider 콘솔에 Redirect URI 등록: `http://localhost:8080/login/oauth2/code/{google|kakao|naver}`

```bash
./gradlew test
```

---

## jwt-only와의 핵심 차이

```bash
git diff base/jwt-only base/oauth2-foundation
```

- `LoginController` 삭제 → `OAuth2SuccessHandler`로 대체 (진입점만 교체)
- `User` 필드: `passwordHash` → `provider`, `providerId`, `profileImageUrl`
- `storage/` 패키지 추가 (패턴별 JwtFilter, SuccessHandler, RefreshTokenHandler)
- `AuthService`는 **두 브랜치가 동일** — 토큰 발급 로직은 로그인 방식과 무관
