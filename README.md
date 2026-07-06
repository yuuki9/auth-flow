# OAuth 2.0 · JWT 학습 레포

ID/PW 로그인과 소셜 로그인(OAuth2)에서 JWT를 발급·검증·갱신하는 흐름을 직접 코드로 확인하는 학습용 프로젝트입니다.

---

## 브랜치 구성

```
main                      ← 지금 여기: OAuth2 스켈레톤 (JWT·DB·Redis 없음)
│
├─ base/jwt-only          ← [트랙 1] ID/PW 로그인 → JWT + Refresh lifecycle
│
└─ base/oauth2-foundation ← [트랙 2] 소셜 로그인 → JWT + 3가지 저장 패턴 비교
```

**학습 순서 제안:** `base/jwt-only` 먼저 → `base/oauth2-foundation` 순으로 보면  
`git diff base/jwt-only base/oauth2-foundation`에서 "OAuth2가 로그인 진입점만 교체한다"는 것이 보입니다.

---

## 이 브랜치(main)에 있는 것

OAuth2 Authorization Code Grant 흐름만 구현한 스켈레톤입니다.  
JWT 발급·DB 저장·Redis는 없습니다.

### 로그인 흐름

```
브라우저                   서버                      Provider (Google/Kakao/Naver)
   │                        │                               │
   │  GET /oauth2/           │                               │
   │  authorization/{provider}                              │
   │──────────────────────>  │                               │
   │                        │──── 리다이렉트 ──────────────>  │
   │                        │                               │ 동의 화면
   │                        │  <──── Authorization Code ──  │
   │                        │                               │
   │                        │  CustomOAuth2UserService      │
   │                        │  - Provider별 사용자 정보 파싱 │
   │                        │  - (DB 저장 없음)             │
   │                        │                               │
   │  <── "/" 리다이렉트 ──  │                               │
   │      (로그 출력만)      │                               │
```

### 파일 구성

```
domain/oauth/
├── OAuth2Provider.java          enum: GOOGLE, KAKAO, NAVER
└── OAuth2UserInfo.java          Provider별 사용자 정보 인터페이스

infrastructure/security/
├── config/SecurityConfig.java
└── oauth/
    ├── CustomOAuth2UserService.java   콜백 처리, Provider별 파싱
    ├── CustomOAuth2User.java          Spring Security principal 래퍼
    ├── OAuth2AuthenticationSuccessHandler.java  "/" 리다이렉트 + 로그
    └── userinfo/
        ├── OAuth2UserInfoFactory.java
        ├── GoogleOAuth2UserInfo.java
        ├── KakaoOAuth2UserInfo.java
        └── NaverOAuth2UserInfo.java
```

### 의존성

| 포함 | 없음 |
|---|---|
| spring-boot-starter-oauth2-client | spring-boot-starter-data-jpa |
| spring-boot-starter-security | spring-boot-starter-data-redis |
| spring-boot-starter-web | jjwt, PostgreSQL |

---

## 실행 방법

### 1. OAuth 앱 등록

각 Provider 콘솔에서 앱을 등록하고 아래 Redirect URI를 허용합니다.

| Provider | Redirect URI |
|---|---|
| Google | `http://localhost:8080/login/oauth2/code/google` |
| Kakao | `http://localhost:8080/login/oauth2/code/kakao` |
| Naver | `http://localhost:8080/login/oauth2/code/naver` |

### 2. 환경변수 설정

`.env.example`을 복사해 `.env` 파일을 만들고 발급받은 값을 입력합니다.

```
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
KAKAO_CLIENT_ID=...
NAVER_CLIENT_ID=...
NAVER_CLIENT_SECRET=...
```

### 3. 실행

```bash
./gradlew bootRun
```

`http://localhost:8080/oauth2/authorization/google` 접속 → 동의 → 로그 확인.

---

## 다음 브랜치에서 확인할 것

| 브랜치 | 추가되는 것 |
|---|---|
| `base/jwt-only` | JWT 발급·검증·갱신, Redis, DB, ID/PW 로그인 |
| `base/oauth2-foundation` | OAuth2 로그인 → JWT 발급, 3가지 토큰 저장 패턴 |

```bash
git checkout base/jwt-only
```

---

## 기술 스택

Java 17 · Spring Boot 3.x · Gradle Kotlin DSL
