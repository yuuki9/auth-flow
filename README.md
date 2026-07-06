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
