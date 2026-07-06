# base/jwt-only — ID/PW 로그인 → JWT

ID/PW 로그인 이후 JWT Access Token + Refresh Token을 발급·검증·갱신하는 흐름에 집중하는 브랜치입니다.  
OAuth2는 없습니다. JWT lifecycle 자체를 먼저 익히는 것이 목적입니다.

---

## 로그인 흐름

```
클라이언트              서버                        Redis
     │                   │                            │
     │  [로그인]          │                            │
     │─ POST /api/auth/  │                            │
     │  login ─────────>│                            │
     │  {email,password} │                            │
     │                   │ LoginService               │
     │                   │ DB 조회 → BCrypt 검증      │
     │                   │                            │
     │                   │ AuthService.issueTokens()  │
     │                   │ AT 생성 (15분)             │
     │                   │ RT 생성 (7일)              │
     │                   │─── save(userId, RT) ──────>│
     │<── {AT, RT} ──────│                            │
     │                   │                            │
     │  [API 요청]        │                            │
     │─ GET /api/auth/me>│                            │
     │  Authorization:   │                            │
     │  Bearer {AT}      │                            │
     │                   │ JwtAuthenticationFilter    │
     │                   │ ① AT 서명 검증             │
     │                   │ ② userId 추출              │
     │                   │ ③ SecurityContext 설정     │
     │<── {id,name,role} ─│                            │
     │                   │                            │
     │  [토큰 갱신]       │                            │
     │─ POST /api/auth/  │                            │
     │  refresh ────────>│                            │
     │  {refreshToken}   │                            │
     │                   │ ① RT 서명 검증             │
     │                   │─── findByUserId ──────────>│
     │                   │<── 저장된 RT ──────────────│
     │                   │ ② 수신 RT == 저장 RT 비교  │
     │                   │─── deleteByUserId ────────>│  기존 RT 삭제
     │                   │ ③ 새 AT + RT 발급 (RTR)   │
     │                   │─── save(userId, newRT) ───>│  새 RT 저장
     │<── {newAT, newRT} ─│                            │
     │                   │                            │
     │  [로그아웃]        │                            │
     │─ POST /api/auth/  │                            │
     │  logout ─────────>│                            │
     │  Authorization:   │                            │
     │  Bearer {AT}      │─── deleteByUserId ────────>│  RT 삭제
     │<── 200 ───────────│                            │
     │  (클라이언트가     │                            │
     │   AT도 폐기해야 함)│                            │
```

> **RTR (Refresh Token Rotation):** 갱신마다 기존 RT를 폐기하고 새 RT를 발급합니다.  
> 탈취된 RT로 갱신 시도 시 저장값과 불일치 → 즉시 감지 → 해당 userId의 RT 전체 삭제.

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
