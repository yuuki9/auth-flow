# OAuth 2.0 · JWT 학습 프로젝트 (auth-guide)

Spring Boot 기반으로 **소셜 로그인(OAuth 2.0)** 을 단계적으로 구현하고, 목표로는 **JWT Access + Redis Refresh** 하이브리드 인증까지 확장하는 학습용 레포입니다.

**교재 중심 문서는 [`guide.md`](guide.md)입니다.** 재현 절차, Stage 정의, 용어 구분(OAuth vs JWT vs 세션), 안티패턴·보안 체크리스트가 여기에 모여 있습니다.

---

## 진행 상황

| Stage | 내용 | 상태 |
|-------|------|------|
| 1 | OAuth2 Client, Google, 세션 로그인 | 완료 |
| 2 | Kakao·Naver, `OAuth2UserInfo` 통합 | 완료 |
| 3 | DB·회원·소셜 연동 | 예정 |
| 4–6 | JWT Access, Redis, Refresh | 예정 |
| 7–8 | 로그아웃·무효화·멀티 디바이스 | 예정 |
| 9–10 | PKCE 등 보안 강화·프로덕션 준비 | 예정 |

---

## 빠른 시작 (재현)

1. **자격 증명 준비**  
   Google·Kakao·Naver 개발자 콘솔에서 클라이언트 ID/Secret 발급 후, [`guide.md`](guide.md)의 Redirect URI를 등록합니다.

2. **비밀값 주입 (커밋된 YAML에 비밀번호를 넣지 마세요)**  
   - 권장: 루트에 [`.env.example`](.env.example)을 참고해 `.env` 생성 후 변수 채우기.  
   - 또는: [`application-local.yaml.example`](src/main/resources/application-local.yaml.example)을 복사해 `application-local.yaml`로 두고 값 입력 (미커밋).

3. **실행**

```bash
.\gradlew.bat bootRun
```

(`spring-dotenv`가 `.env`를 로드합니다. 프로필만 쓸 경우 `--args="--spring.profiles.active=local"` 등은 `guide.md` 참고.)

4. 브라우저에서 `http://localhost:8080` → 로그인 플로우 확인.

---

## 문서

| 파일 | 설명 |
|------|------|
| [**guide.md**](guide.md) | **메인 교재** — 재현·Stage·용어·안티패턴·트러블슈팅 요약 |
| [ARCHITECTURE.md](ARCHITECTURE.md) | 목표 아키텍처(JWT·Redis·레이어) |

---

## 프로젝트 구조 (요약)

```
src/main/java/com/auth/practice/
├── AuthGuideApplication.java
├── domain/oauth/                    # OAuth2Provider, OAuth2UserInfo
├── infrastructure/security/
│   ├── config/SecurityConfig.java
│   └── oauth/                     # CustomOAuth2UserService, SuccessHandler, userinfo/*
└── presentation/controller/       # HomeController

src/main/resources/
├── application.yaml               # OAuth 설정 틀 (비밀값은 환경 변수)
├── application-local.yaml.example
└── templates/                     # index, login, home
```

---

## 기술 스택

- Java 17, Spring Boot 3.5.x, Spring Security 6, OAuth2 Client, Thymeleaf  
- 향후(계획): JPA, Redis, JWT(jjwt) 등 — [`ARCHITECTURE.md`](ARCHITECTURE.md) 참고.

---

## 테스트

```bash
.\gradlew.bat test
```

OAuth 자격 증명은 테스트 코드에서 더미 값으로 덮어써 컨텍스트만 로드합니다 (`AuthGuideApplicationTests`).

---

## OAuth 2.0 Authorization Code 흐름 (개요)

```
사용자 → 로그인 페이지 → Provider 로그인 → Authorization Code → 토큰 교환
→ UserInfo → 세션 생성 → 홈
```

세부 흐름·오류 대응은 [`guide.md`](guide.md) (§ 재현, § 자주 나오는 오류)를 참고하세요.

---

## 문제 해결

[`guide.md` § 9](guide.md) (자주 나오는 오류) 참고.

---

## 라이선스

MIT License
