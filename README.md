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

## SOP (단계별 절차)

### 소셜 로그인

| 단계 | 처리 | 실패 시 |
|---|---|---|
| 1 | `GET /oauth2/authorization/{provider}` 수신 | — |
| 2 | Provider 동의 화면으로 리다이렉트 | — |
| 3 | Provider가 Authorization Code와 함께 콜백 | 사용자 거부 → 로그인 취소 |
| 4 | `CustomOAuth2UserService`: Provider별 사용자 정보 파싱 (`OAuth2UserInfoFactory`) | Provider 응답 오류 → 500 |
| 5 | DB에서 email + provider로 기존 User 조회 | — |
| 6 | 신규: `INSERT` / 기존: 이름·프로필 `UPDATE` | DB 장애 시 500 |
| 7 | `OAuth2SuccessHandler` → `AuthService.issueTokens()`: AT(15분) + RT(7일) 생성 | — |
| 8 | Redis에 `refresh:{userId} = RT` 저장 (TTL 7일) | Redis 장애 시 500 |
| 9 | 패턴별 토큰 전달 후 `/index.html` 리다이렉트 | — |

**패턴별 9단계 상세:**

| 패턴 | 전달 방식 |
|---|---|
| `cookie` | `Set-Cookie: access_token` (HttpOnly, 15분) + `Set-Cookie: refresh_token` (HttpOnly, path=/api/auth/refresh) |
| `memory` | body `{ accessToken }` + `Set-Cookie: refresh_token` (HttpOnly) |
| `localstorage` | `redirect /index.html#access_token=...&refresh_token=...` |

### API 요청

| 단계 | 처리 | 실패 시 |
|---|---|---|
| 1 | 패턴별 JwtFilter가 AT 추출 (쿠키 or Authorization 헤더) | AT 없으면 SecurityContext 미설정 → 403 |
| 2 | AT 서명 검증 | 변조·만료 → 403 |
| 3 | userId 추출 → SecurityContext 설정 | — |

### 토큰 갱신 (RTR)

| 단계 | 처리 | 실패 시 |
|---|---|---|
| 1 | `POST /api/auth/refresh` 수신 | — |
| 2 | `RefreshTokenHandler`: 패턴별 RT 추출 (쿠키 or body) | RT 없으면 400 |
| 3 | RT 서명 검증 | 변조·만료 → 401 |
| 4 | Redis에서 저장된 RT 조회 | key 없음 → 401 `"만료·로그아웃·탈취 감지"` |
| 5 | 수신 RT == 저장 RT 일치 확인 | **불일치 → Redis 즉시 삭제 후 401** `"재사용 감지"` |
| 6 | 기존 RT 삭제 → 새 AT + RT 발급 → Redis 저장 | — |
| 7 | `RefreshTokenHandler`: 패턴별 응답 반환 | — |

### 로그아웃

| 단계 | 처리 | 비고 |
|---|---|---|
| 1 | `POST /api/auth/logout` 수신 (AT 필요) | AT 없으면 403 |
| 2 | Redis에서 `refresh:{userId}` 삭제 | RT 즉시 무효화 |
| 3 | `RefreshTokenHandler.clearOnLogout()`: 패턴별 클라이언트 정리 | cookie만 서버에서 쿠키 삭제 처리 |
| 4 | 200 반환 | — |
| ⚠️ | AT는 TTL(15분) 만료까지 서버가 차단 불가 | 클라이언트가 AT 직접 폐기 필요 |

---

## 로그아웃 정책

**목적:** RT를 즉시 무효화하고, 패턴별 클라이언트 측 토큰도 함께 정리합니다.

**방식:** `POST /api/auth/logout` → Redis RT 삭제 + `RefreshTokenHandler.clearOnLogout()` 호출

**패턴별 클라이언트 정리:**

| 패턴 | 서버가 추가로 하는 일 | 클라이언트가 해야 할 일 |
|---|---|---|
| `cookie` | Set-Cookie로 쿠키 만료 처리 | 없음 (서버가 쿠키 삭제) |
| `memory` | Set-Cookie로 RT 쿠키 만료 처리 | JS 변수(AT) 직접 폐기 |
| `localstorage` | 없음 | localStorage에서 AT·RT 직접 삭제 |

**트레이드오프 — AT 즉시 무효화 불가:**  
RT는 Redis 삭제로 즉시 무효화되지만, AT는 Stateless라 서버가 차단할 수 없습니다.  
TTL(15분) 내에는 탈취된 AT도 유효합니다. TTL을 짧게 유지하는 이유가 여기 있습니다.

**단일 세션 한계:**  
Redis key가 `refresh:{userId}`이므로 로그아웃 시 모든 기기가 함께 로그아웃됩니다.

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
