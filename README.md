# OAuth 2.0 · JWT flow

ID/PW 로그인과 소셜 로그인에서 JWT를 발급·검증·갱신하는 흐름을 코드로 확인합니다.

## 브랜치 구성

| 브랜치 | 내용 |
|---|---|
| `main` | OAuth2 스켈레톤 — JWT·DB·Redis 없음 (출발점) |
| `base/jwt-only` | ID/PW 로그인 → JWT + Refresh lifecycle (단일 세션) |
| `base/oauth2-foundation` | 소셜 로그인 → JWT + 3가지 저장 패턴 비교 |
| `feature/multi-session` | `base/jwt-only` 확장 → jti 기반 멀티 세션 |

**추천 순서:** `base/jwt-only` → `base/oauth2-foundation`  
단일 세션의 한계를 이해했다면 `feature/multi-session`에서 jti 도입 diff를 확인합니다.

---

## 브랜치별 로그인 흐름

### base/jwt-only — ID/PW → JWT

![JWT-only 인증 흐름](docs/diagrams/jwt-only-flow.png)

### base/oauth2-foundation — 소셜 로그인 → JWT

![OAuth2 로그인 흐름](docs/diagrams/oauth2-login-flow.png)

`application.yaml`의 `spring.profiles.active` 한 줄로 패턴을 전환합니다.  
구현체는 `infrastructure/security/storage/{cookie|memory|localstorage}/` 패키지에 `@Profile`로 분리되어 있습니다.

#### 패턴별 토큰 전달

| 항목 | `cookie` | `memory` | `localstorage` |
|------|----------|----------|----------------|
| **Access Token (AT)** | HttpOnly 쿠키 `access_token` (15분, path=/) | JS 변수 — URL `#fragment`에서 읽어 메모리 보관 (탭 닫으면 소멸) | localStorage `accessToken` |
| **Refresh Token (RT)** | HttpOnly 쿠키 `refresh_token` (7일, path=/api/auth/refresh) | HttpOnly 쿠키 (동일) | localStorage `refreshToken` |
| **로그인 성공 시** | `Set-Cookie` AT+RT → `/index.html` 리다이렉트 | `#access_token=...` 리다이렉트 + RT 쿠키 | `#access_token=...&refresh_token=...` 리다이렉트 |
| **API 요청** | AT 쿠키 자동 전송 | `Authorization: Bearer {AT}` | `Authorization: Bearer {AT}` |
| **토큰 갱신** | RT 쿠키 자동 전송 (`POST /api/auth/refresh`) | RT 쿠키 자동 전송 | body `{ "refreshToken": "..." }` |
| **갱신 응답** | AT·RT 쿠키 재설정 | body `{ accessToken }` + RT 쿠키 | body `{ accessToken, refreshToken }` |
| **로그아웃** | 서버가 AT·RT 쿠키 만료 처리 | RT 쿠키 만료 (AT는 JS에서 폐기) | 클라이언트가 localStorage 직접 삭제 |

#### 적합한 사용 사례

| 패턴 | 언제 쓰나 | 보안 | 비고 |
|------|-----------|------|------|
| `cookie` | SSR·동일 도메인 **브라우저 웹앱**, 서버가 HTML을 내려주는 구조 | ⭐⭐⭐ — JS가 토큰 접근 불가 (HttpOnly) | **현업 표준**. CSRF는 `SameSite=Strict`로 방어 |
| `memory` | **SPA** + API 서버 분리, Silent Refresh로 UX 유지하고 싶을 때 | ⭐⭐⭐ — RT는 HttpOnly, AT만 JS 노출 | AT는 XSS에 취약하지만 **탭/세션 단위**로만 유지 |
| `localstorage` | **학습·데모** — cookie/memory와 XSS 차이를 비교할 때 | ⚠️ — AT·RT 모두 JS로 탈취 가능 | **운영 환경 사용 금지** |

### feature/multi-session — jti 기반 멀티 세션

```
클라이언트 A            서버                        Redis
     │                   │                            │
     │─ POST /login ───>│                            │
     │                   │ Refresh Token 생성         │
     │                   │ jti = UUID ("aaa")         │
     │                   │── save("aaa", userId) ───>│  refresh:aaa = 100
     │<── {AT-A, RT-A} ──│                            │
     │                   │                            │
클라이언트 B             │                            │
     │─ POST /login ───>│                            │
     │                   │ jti = UUID ("bbb")         │
     │                   │── save("bbb", userId) ───>│  refresh:bbb = 100
     │<── {AT-B, RT-B} ──│                            │  (aaa는 그대로)
     │                   │                            │
     │  [A가 토큰 갱신]   │                            │
     │─ POST /refresh ──>│                            │
     │  {refreshToken:   │── findByJti("aaa") ──────>│
     │   RT-A}           │<── userId: 100 ───────────│
     │                   │── deleteByJti("aaa") ────>│  refresh:aaa 삭제
     │                   │── save("ccc", userId) ───>│  refresh:ccc = 100
     │<── {newAT, newRT} ─│                            │
     │                   │                            │
     │  B는 여전히 유효  │                            │  refresh:bbb = 100 (유지)
     │                   │                            │
     │  [A만 로그아웃]    │                            │
     │─ POST /logout ───>│                            │
     │  {refreshToken:   │── deleteByJti("ccc") ────>│  refresh:ccc 삭제
     │   newRT}          │                            │
     │                   │                            │  refresh:bbb = 100 (유지)
```

---

## 실행 (main)

1. `.env.example` 복사 후 `.env` 작성 (OAuth Client ID/Secret)
2. Provider Redirect URI: `http://localhost:8080/login/oauth2/code/{google|kakao|naver}`

```bash
./gradlew bootRun
```

## 다음 단계

```bash
git checkout base/jwt-only
```
