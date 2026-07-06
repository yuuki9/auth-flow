# feature/multi-session — jti 기반 멀티 세션

`base/jwt-only`(단일 세션)에서 **jti(JWT ID)를 도입해 멀티 세션을 지원**하도록 확장한 브랜치입니다.

## base/jwt-only와의 핵심 차이

```bash
git diff base/jwt-only feature/multi-session
```

| | base/jwt-only | feature/multi-session |
|---|---|---|
| Redis key | `refresh:{userId}` | `refresh:{jti}` |
| 동시 로그인 | 나중 로그인이 기존 세션 덮어씀 | 세션마다 독립적인 key |
| 로그아웃 | userId로 전체 삭제 | 해당 세션 jti만 삭제 |

## jti란?

JWT 표준 payload에 정의된 고유 식별자 클레임(`id`)입니다.  
Refresh Token 생성 시 UUID를 심어 **토큰 한 장 = Redis key 하나**가 되도록 합니다.

```
브라우저 A 로그인 → Redis["refresh:aaa-uuid"] = 100
브라우저 B 로그인 → Redis["refresh:bbb-uuid"] = 100  ← 덮어쓰지 않음

브라우저 A 갱신  → Redis["refresh:aaa-uuid"] 삭제, Redis["refresh:ccc-uuid"] 생성
브라우저 B       → Redis["refresh:bbb-uuid"] 그대로 유효
```

## 변경된 파일

| 파일 | 변경 내용 |
|---|---|
| `JwtProvider` | `generateRefreshToken()`에 `.id(UUID)` 추가, `getJti()` 메서드 추가 |
| `RefreshTokenRepository` | `save(jti, userId, ttl)` / `findUserIdByJti(jti)` / `deleteByJti(jti)` |
| `RedisRefreshTokenRepository` | key `refresh:{jti}`, value `userId` |
| `AuthService` | jti 추출 후 Redis 저장·조회, `logout(refreshToken)`으로 시그니처 변경 |
| `AuthController` | `/logout` 엔드포인트에서 body의 `refreshToken` 수신 |

## 엔드포인트

| Method | Path | 변경점 |
|---|---|---|
| `POST` | `/api/auth/login` | 동일 |
| `GET` | `/api/auth/me` | 동일 |
| `POST` | `/api/auth/refresh` | 동일 |
| `POST` | `/api/auth/logout` | body에 `{ "refreshToken": "..." }` 필요 |

## 실행

```bash
docker compose up -d
cp .env.example .env
./gradlew bootRun
```

```bash
./gradlew test
```
