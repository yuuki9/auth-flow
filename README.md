# feature/multi-session — jti 기반 멀티 세션

`base/jwt-only`(단일 세션)에서 **jti(JWT ID)를 도입해 멀티 세션을 지원**하도록 확장한 브랜치입니다.

---

## 로그인 흐름

```
                         서버                        Redis
                          │                            │
  브라우저 A ─ POST /login>│                            │
                          │ RT 생성 + jti = "aaa"(UUID)│
                          │──── save("aaa", 100) ─────>│  refresh:aaa = 100
  브라우저 A <── {AT-A, RT-A}                           │
                          │                            │
  브라우저 B ─ POST /login>│                            │
                          │ RT 생성 + jti = "bbb"(UUID)│
                          │──── save("bbb", 100) ─────>│  refresh:bbb = 100
  브라우저 B <── {AT-B, RT-B}                           │  (aaa는 유지됨)
                          │                            │
  [A가 토큰 갱신]          │                            │
  A ─ POST /refresh ─────>│                            │
      {refreshToken: RT-A}│                            │
                          │ RT-A에서 jti "aaa" 추출    │
                          │──── findByJti("aaa") ─────>│
                          │<─── userId: 100 ───────────│
                          │──── deleteByJti("aaa") ───>│  refresh:aaa 삭제
                          │ 새 RT 생성 + jti = "ccc"   │
                          │──── save("ccc", 100) ─────>│  refresh:ccc = 100
  A <── {newAT-A, newRT-A}│                            │
                          │                            │
  B는 RT-B로 여전히 유효  │                            │  refresh:bbb = 100 유지
                          │                            │
  [A만 로그아웃]           │                            │
  A ─ POST /logout ───────>│                            │
      {refreshToken: newRT-A}                           │
                          │ newRT-A에서 jti "ccc" 추출 │
                          │──── deleteByJti("ccc") ───>│  refresh:ccc 삭제
  A <── 200               │                            │
                          │                            │
  B는 여전히 로그인 유지  │                            │  refresh:bbb = 100 유지
```

---

## base/jwt-only(단일 세션)과의 비교

```
단일 세션 (base/jwt-only)           멀티 세션 (feature/multi-session)
─────────────────────────────       ─────────────────────────────────
Redis["refresh:100"] = RT           Redis["refresh:aaa"] = 100
                                    Redis["refresh:bbb"] = 100
                                    Redis["refresh:ccc"] = 100  ...

B 로그인 시 A의 RT 덮어씀            각 세션이 독립적인 key 보유
갱신 시 전 세션 영향                  갱신 시 해당 세션만 교체
로그아웃 시 전 세션 종료              로그아웃 시 해당 세션만 종료
```

---

## SOP (단계별 절차)

### 로그인

| 단계 | 처리 | 실패 시 |
|---|---|---|
| 1 | `POST /api/auth/login { email, password }` 수신 | — |
| 2 | `LoginService`: BCrypt 검증 | 불일치 → 401 |
| 3 | `AuthService.issueTokens()`: AT(15분) + RT(7일) 생성 | — |
| 4 | RT payload에서 `jti`(UUID) 추출 | — |
| 5 | Redis에 `refresh:{jti} = userId` 저장 (TTL 7일) | Redis 장애 시 500 |
| 6 | `{ accessToken, refreshToken, expiresInMs }` 반환 | — |

> 같은 userId로 재로그인해도 jti가 UUID라 항상 다른 key → 기존 세션 유지됨

### 토큰 갱신 (RTR)

| 단계 | 처리 | 실패 시 |
|---|---|---|
| 1 | `POST /api/auth/refresh { refreshToken }` 수신 | 필드 없으면 400 |
| 2 | RT 서명 검증 | 변조·만료 → 401 |
| 3 | RT payload에서 `jti` 추출 | — |
| 4 | Redis에서 `refresh:{jti}` 조회 → userId 반환 | key 없음 → 401 `"만료·로그아웃·탈취 감지"` |
| 5 | 기존 `refresh:{jti}` 삭제 | — |
| 6 | 새 AT + RT(새 jti) 발급 → `refresh:{newJti}` 저장 | — |
| 7 | `{ newAT, newRT }` 반환 | — |

> 갱신된 세션만 새 jti를 가짐. 다른 브라우저의 `refresh:{jti}` key는 변경 없이 유지됨.

### 로그아웃 (특정 세션)

| 단계 | 처리 | 실패 시 |
|---|---|---|
| 1 | `POST /api/auth/logout { refreshToken }` 수신 | 필드 없으면 400 |
| 2 | RT 서명 검증 | 만료·변조 → 처리 없이 종료 (이미 무효) |
| 3 | RT payload에서 `jti` 추출 | — |
| 4 | Redis에서 `refresh:{jti}` 삭제 | — |
| 5 | 200 반환 | — |
| ⚠️ | 다른 세션의 `refresh:{jti}` key는 삭제되지 않음 | 다른 브라우저는 로그인 유지 |
| ⚠️ | AT는 TTL(15분) 만료까지 서버가 차단 불가 | 클라이언트가 AT 직접 폐기 필요 |

---

## 로그아웃 정책

**목적:** 특정 세션(기기)만 로그아웃하고, 나머지 세션은 유지합니다.

**방식:** `POST /api/auth/logout { refreshToken }` → RT에서 jti 추출 → Redis `refresh:{jti}` 삭제

**왜 RT를 body로 받나:**  
단일 세션에서는 userId만 알면 RT를 찾을 수 있었지만,  
멀티 세션에서는 "어느 세션을 로그아웃할지" 특정하려면 jti가 필요합니다.  
jti는 RT 안에 있으므로 RT를 직접 받아서 추출합니다.

**트레이드오프:**

| 항목 | 내용 |
|---|---|
| AT 즉시 무효화 불가 | Stateless 구조 한계 — TTL(15분) 만료까지 유효 |
| 전체 로그아웃 불편 | 보유한 RT 수만큼 `/logout`을 호출하거나, 별도 전체 로그아웃 API 필요 |
| RT 분실 시 로그아웃 불가 | RT가 없으면 jti를 알 수 없어 해당 세션 삭제 불가 (TTL 만료 대기) |

**단일 세션 대비 개선점:**  
브라우저 A 로그아웃이 브라우저 B 세션에 영향을 주지 않습니다.

---

## 변경된 파일

```bash
git diff base/jwt-only feature/multi-session
```

| 파일 | 변경 내용 |
|---|---|
| `JwtProvider` | `generateRefreshToken()`에 `.id(UUID)` 추가, `getJti()` 추가 |
| `RefreshTokenRepository` | `save(jti, userId, ttl)` / `findUserIdByJti` / `deleteByJti` |
| `RedisRefreshTokenRepository` | key `refresh:{jti}`, value `userId` |
| `AuthService` | jti 추출 후 저장·조회, `logout(refreshToken)`으로 변경 |
| `AuthController` | `/logout` body에 `refreshToken` 수신 |

---

## 엔드포인트

| Method | Path | 변경점 |
|---|---|---|
| `POST` | `/api/auth/login` | 동일 |
| `GET` | `/api/auth/me` | 동일 |
| `POST` | `/api/auth/refresh` | 동일 |
| `POST` | `/api/auth/logout` | body `{ "refreshToken": "..." }` 필요 (특정 세션만 종료) |

---

## 실행

```bash
docker compose up -d
cp .env.example .env
./gradlew bootRun
```

```bash
./gradlew test
```
