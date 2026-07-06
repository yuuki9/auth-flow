# base/jwt-only — ID/PW 로그인 → JWT

ID/PW 로그인 이후 JWT Access Token + Refresh Token을 발급·검증·갱신하는 흐름에 집중하는 브랜치입니다.  
OAuth2는 없습니다. JWT lifecycle 자체를 먼저 익히는 것이 목적입니다.

---

## 로그인 흐름

![JWT-only 인증 흐름](docs/diagrams/jwt-only-flow.png)

> **RTR (Refresh Token Rotation):** 갱신마다 기존 RT를 폐기하고 새 RT를 발급합니다.  
> 탈취된 RT로 갱신 시도 시 저장값과 불일치 → 즉시 감지 → 해당 userId의 RT 전체 삭제.

---

## SOP (단계별 절차)

### 로그인

| 단계 | 처리 | 실패 시 |
|---|---|---|
| 1 | `POST /api/auth/login { email, password }` 수신 | — |
| 2 | `LoginService`: DB에서 email로 User 조회 | 404 → 401 반환 (User Enumeration 방지) |
| 3 | BCrypt로 password 검증 | 불일치 → 401 `"이메일 또는 비밀번호가 올바르지 않습니다"` |
| 4 | `AuthService.issueTokens()`: AT(15분) + RT(7일) 생성 | — |
| 5 | Redis에 `refresh:{userId} = RT` 저장 (TTL 7일) | Redis 장애 시 500 |
| 6 | `{ accessToken, refreshToken, expiresInMs }` 반환 | — |

### API 요청

| 단계 | 처리 | 실패 시 |
|---|---|---|
| 1 | `Authorization: Bearer {AT}` 헤더 수신 | 헤더 없으면 SecurityContext 미설정 → 403 |
| 2 | `JwtAuthenticationFilter`: AT 서명 검증 | 변조·만료 → 403 |
| 3 | AT payload에서 userId 추출 | — |
| 4 | SecurityContext에 userId 설정 | — |
| 5 | 컨트롤러에서 `@AuthenticationPrincipal Long userId`로 주입 | — |

### 토큰 갱신 (RTR)

| 단계 | 처리 | 실패 시 |
|---|---|---|
| 1 | `POST /api/auth/refresh { refreshToken }` 수신 | 필드 없으면 400 |
| 2 | RT 서명 검증 | 변조·만료 → 401 `"유효하지 않은 Refresh Token"` |
| 3 | RT payload에서 userId 추출 | — |
| 4 | Redis에서 `refresh:{userId}` 조회 | key 없음 → 401 `"만료·로그아웃·탈취 감지"` |
| 5 | 수신 RT == 저장 RT 일치 확인 | **불일치 → Redis 즉시 삭제 후 401** `"재사용 감지"` (탈취 의심) |
| 6 | 기존 RT 삭제 → 새 AT + RT 발급 → Redis 저장 | — |
| 7 | `{ accessToken, refreshToken, expiresInMs }` 반환 | — |

> **5단계 불일치의 의미:** 이미 사용된 RT로 재시도 = 탈취 후 재사용 의심.  
> Redis를 삭제해 진짜 사용자도 재로그인하게 만드는 것이 의도된 fail-safe 설계입니다.

### 로그아웃

| 단계 | 처리 | 비고 |
|---|---|---|
| 1 | `POST /api/auth/logout` 수신 (AT 필요) | AT 없으면 403 |
| 2 | AT에서 userId 추출 | — |
| 3 | Redis에서 `refresh:{userId}` 삭제 | RT 즉시 무효화 |
| 4 | 200 반환 | — |
| ⚠️ | AT는 TTL(15분) 만료까지 서버가 차단 불가 | 클라이언트가 AT 직접 폐기 필요 |

---

## 로그아웃 정책

**목적:** 탈취된 RT를 즉시 무효화해 피해를 차단합니다.

**방식:** `POST /api/auth/logout` → Redis에서 `refresh:{userId}` key 삭제

**왜 Redis 삭제만으로 충분한가:**  
RT는 다음 갱신 시 Redis 조회를 반드시 거칩니다. Key가 없으면 갱신 불가 → RT 즉시 무효화.

**트레이드오프 — AT는 즉시 무효화 불가:**  
AT는 Stateless(서버가 상태를 보관하지 않음)이므로 서버가 직접 차단할 수 없습니다.  
로그아웃 후에도 AT TTL(15분)이 남아있으면 API 호출이 가능합니다.  
→ 클라이언트가 AT를 직접 폐기해야 하며, TTL을 15분으로 짧게 유지하는 이유가 여기 있습니다.

**단일 세션 한계:**  
같은 userId의 RT를 하나만 관리하므로, 로그아웃 시 모든 기기가 함께 로그아웃됩니다.  
기기별 독립 로그아웃이 필요하면 `feature/multi-session`(jti 기반)을 참고하세요.

---

## 엔드포인트

| Method | Path | 인증 | 설명 |
|---|---|---|---|
| `POST` | `/api/auth/login` | 불필요 | 로그인 → AT + RT 발급 |
| `GET` | `/api/auth/me` | AT 필요 | 내 정보 조회 |
| `POST` | `/api/auth/refresh` | 불필요 | RT → 새 AT + RT |
| `POST` | `/api/auth/logout` | AT 필요 | Redis RT 삭제 |

---

## 실행

```bash
docker compose up -d          # PostgreSQL + Redis
cp .env.example .env          # JWT_SECRET 등 입력
./gradlew bootRun
```

앱 시작 시 테스트 계정 자동 생성: `test@example.com` / `password123`

```bash
./gradlew test
```

---

## 읽을 파일 순서

1. `domain/user/User.java`
2. `infrastructure/security/jwt/JwtProvider.java`
3. `application/auth/AuthService.java`
4. `infrastructure/security/jwt/JwtAuthenticationFilter.java`
5. `infrastructure/security/config/SecurityConfig.java`
6. `presentation/controller/LoginController.java`
7. `presentation/controller/AuthController.java`

---

## 다음 단계

```bash
git checkout base/oauth2-foundation       # OAuth2가 로그인 진입점만 교체함을 확인
git checkout feature/multi-session        # jti로 멀티 세션 지원
git diff base/jwt-only feature/multi-session  # 단일 → 멀티 세션 변경점
```
