# OAuth 2.0 · JWT 학습 레포

ID/PW 로그인과 소셜 로그인에서 JWT를 발급·검증·갱신하는 흐름을 코드로 확인하는 학습 프로젝트입니다.

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

## 이 브랜치(main)

OAuth2 Authorization Code 흐름만 구현한 출발점입니다.  
로그인 성공 시 사용자 정보를 로그에 출력하고 `/`로 리다이렉트합니다. JWT 발급은 없습니다.

```
브라우저 → GET /oauth2/authorization/{provider}
         → Provider 동의 화면
         → CustomOAuth2UserService: 사용자 정보 파싱 (DB 저장 없음)
         → "/" 리다이렉트
```

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
