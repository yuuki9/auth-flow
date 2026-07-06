# OAuth 2.0 · JWT flow

ID/PW 로그인과 소셜 로그인에서 JWT를 발급·검증·갱신하는 흐름을 코드로 확인합니다.

## 브랜치 구성

| 브랜치 | 내용 |
|---|---|
| `main` (현재) | OAuth2 스켈레톤 — JWT·DB·Redis 없음 |
| `base/jwt-only` | ID/PW 로그인 → JWT + Refresh lifecycle (단일 세션) |
| `base/oauth2-foundation` | 소셜 로그인 → JWT + 3가지 저장 패턴 비교 |
| `feature/multi-session` | `base/jwt-only` 확장 → jti 기반 멀티 세션 |

**추천 순서:** `base/jwt-only` → `base/oauth2-foundation`  
단일 세션의 한계를 이해했다면 `feature/multi-session`에서 jti 도입 diff를 확인합니다.

---

## 브랜치별 로그인 흐름

### main — OAuth2 스켈레톤

```
클라이언트              서버                       Provider
     │                   │                            │
     │─ GET /oauth2/     │                            │
     │  authorization/   │                            │
     │  {provider} ────>│                            │
     │                   │──── 리다이렉트 ───────────>│
     │                   │                            │ 동의 화면
     │                   │<─── Authorization Code ───│
     │                   │                            │
     │                   │ CustomOAuth2UserService    │
     │                   │ Provider별 사용자 정보 파싱 │
     │                   │ (DB 저장 없음, JWT 없음)   │
     │                   │                            │
     │<── "/" 리다이렉트 ─│                            │
     │    (로그 출력만)   │                            │
```

---

### base/jwt-only — ID/PW → JWT

```
클라이언트              서버                        Redis
     │                   │                            │
     │─ POST /api/auth/  │                            │
     │  login ─────────>│                            │
     │  {email,password} │                            │
     │                   │ LoginService: BCrypt 검증  │
     │                   │ AuthService.issueTokens()  │
     │                   │── save(userId, RT, 7d) ──>│
     │<── {AT, RT} ──────│                            │
     │                   │                            │
     │  [API 요청]        │                            │
     │─ Authorization:   │                            │
     │  Bearer {AT} ───>│                            │
     │                   │ JwtAuthenticationFilter    │
     │                   │ AT 검증 → userId 추출      │
     │<── 200 응답 ───────│                            │
     │                   │                            │
     │  [토큰 갱신]       │                            │
     │─ POST /refresh ──>│                            │
     │  {refreshToken}   │── findByUserId ──────────>│
     │                   │<── RT ────────────────────│
     │                   │ RTR: 기존 삭제 → 새 발급   │
     │                   │── save(userId, newRT) ───>│
     │<── {newAT, newRT} ─│                            │
     │                   │                            │
     │  [로그아웃]        │                            │
     │─ POST /logout ───>│                            │
     │                   │── deleteByUserId ─────────>│
     │<── 200 ───────────│                            │
```

---

### base/oauth2-foundation — 소셜 로그인 → JWT

```
클라이언트              서버                  Provider        Redis
     │                   │                      │               │
     │─ GET /oauth2/     │                      │               │
     │  authorization/   │                      │               │
     │  {provider} ────>│                      │               │
     │                   │──── 리다이렉트 ──────>│               │
     │                   │                      │ 동의 화면     │
     │                   │<─── code ────────────│               │
     │                   │                      │               │
     │                   │ CustomOAuth2UserService               │
     │                   │ DB upsert (신규 INSERT / 기존 UPDATE) │
     │                   │                      │               │
     │                   │ OAuth2SuccessHandler                  │
     │                   │ AuthService.issueTokens()             │
     │                   │── save(userId, RT) ──────────────────>│
     │                   │                      │               │
     │  [패턴별 토큰 전달]│                      │               │
     │  cookie      ←── Set-Cookie: AT, RT (HttpOnly)           │
     │  memory      ←── body: {AT} + Set-Cookie: RT (HttpOnly)  │
     │  localstorage←── redirect #AT&RT (⚠️ XSS 취약)          │
     │                   │                      │               │
     │  [토큰 갱신 / 로그아웃은 base/jwt-only와 동일 흐름]       │
```

---

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

## 이 브랜치(main)

OAuth2 Authorization Code 흐름만 구현한 출발점입니다.  
로그인 성공 시 사용자 정보를 로그에 출력하고 `/`로 리다이렉트합니다. JWT 발급은 없습니다.

## 실행

1. `.env.example` 복사 후 `.env` 작성 (OAuth 앱 Client ID/Secret 입력)
2. Provider 콘솔에 Redirect URI 등록: `http://localhost:8080/login/oauth2/code/{google|kakao|naver}`

```bash
./gradlew bootRun
```

## 다음 단계

```bash
git checkout base/jwt-only
```
