# Auth Guide — JWT · OAuth 학습 교재 가이드

이 문서는 **재현 가능하고 검증 가능한** 학습 경로를 제공합니다. 코드와 문서가 어긋나지 않도록 단계(Stage) 정의, 실행 방법, 용어 경계를 한곳에 모았습니다.

---

## 1. 이 레포를 무엇으로 볼 것인가

| 목표 | 설명 |
|------|------|
| OAuth 2.0 | Authorization Code Grant로 **소셜 로그인(위임 인가)** 흐름 이해 |
| 향후 JWT | 로그인 성공 후 **우리 서비스가 발급하는** Access Token(JWT) — OAuth 제공자가 준 토큰과 구분 |
| 향후 Redis | Refresh Token 저장·무효화 — `ARCHITECTURE.md` 하이브리드 전략 |

**이 레포에서 구분해야 할 것**

- **OAuth 제공자 Access Token** (Google/Kakao/Naver와의 통신용): Spring OAuth2 Client가 처리하고, 학습 초반에는 직접 다루지 않아도 됨.
- **세션(`JSESSIONID`)**: 현재 로그인 유지 방식.
- **향후 우리 JWT**: API 인증용으로 설계 예정 — OAuth와 동일 개념이 아님.

---

## 2. 재현 가능한 실행 (필수 절차)

커밋된 `application.yaml`에는 **클라이언트 비밀값이 없습니다.** 다음 중 하나로 값을 넣습니다.

### 방법 A — `.env` (권장)

1. 프로젝트 루트에 `.env.example`을 참고해 `.env` 파일 생성.
2. 각 Provider 콘솔에서 발급한 값 입력 (변수 이름은 `.env.example` 참고).
3. 실행:

```bash
.\gradlew.bat bootRun
```

`spring-dotenv`가 `.env`를 로드하고, YAML의 `${변수명}`과 연결됩니다.

### 방법 B — `application-local.yaml`

1. `src/main/resources/application-local.yaml.example`를 복사해 `application-local.yaml`로 저장 (이 파일은 `.gitignore`로 커밋되지 않음).
2. 예제에 안내된 형태로 `client-id` / `client-secret` 채움.
3. 실행:

```bash
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

프로필을 나누어 운영할 경우 `application-local.yaml`에서 OAuth 설정을 덮어쓸 수 있습니다.

`.\gradlew.bat test`는 `AuthGuideApplicationTests`에서 OAuth 등록 값을 더미로 지정해 컨텍스트만 검증합니다. 실제 Provider 호출은 하지 않습니다.

### 리디렉션 URI (공통)

로컬 기준 예시는 다음과 같습니다.

| Provider | Redirect URI |
|----------|----------------|
| Google | `http://localhost:8080/login/oauth2/code/google` |
| Kakao | `http://localhost:8080/login/oauth2/code/kakao` |
| Naver | `http://localhost:8080/login/oauth2/code/naver` |

포트를 바꾼 경우 앞부분만 동일하게 맞춥니다.

---

## 3. Stage 로드맵과 코드 상태

문서·코드 동기화를 위해 **아래 표가 현재 기준**입니다. README의 체크리스트와 함께 유지합니다.

| Stage | 주제 | 상태 |
|-------|------|------|
| 1 | OAuth2 Client, Google, 세션 로그인 | 완료 |
| 2 | Kakao·Naver, `OAuth2UserInfo` 통합 | 완료 |
| 3 | DB·회원 저장·소셜 연동 모델 | 예정 |
| 4 | 우리 서비스 JWT Access Token 발급 | 예정 |
| 5 | Redis 등 인프라 준비 | 예정 |
| 6 | Refresh Token 저장·재발급 | 예정 |
| 7 | 로그아웃·토큰 무효화 | 예정 |
| 8 | 멀티 디바이스·세션/토큰 회전 | 예정 |
| 9 | PKCE·Rate limit 등 보안 강화 | 예정 |
| 10 | 프로덕션 배포·운영 체크리스트 | 예정 |

향후 Stage별로 **Git 태그**(`stage-1`, `stage-2`, …)를 붙이면 학습자가 동일 커밋을 체크아웃해 재현하기 좋습니다.

---

## 4. 코드베이스 지도

| 문서 | 역할 |
|------|------|
| `README.md` | 진입점 |
| `guide.md` | 본 문서 — 교재 운영 원칙·재현·Stage |
| `ARCHITECTURE.md` | 목표 아키텍처(JWT·Redis·레이어) |

패키지 요약:

- `presentation` — `HomeController`, 템플릿 라우팅
- `domain/oauth` — `OAuth2Provider`, `OAuth2UserInfo`
- `infrastructure/security` — `SecurityConfig`, OAuth2 사용자 서비스·성공 핸들러, Provider별 `userinfo`

---

## 5. 학습 단계별로 무엇을 보나

### Stage 1~2 (현재)

- Authorization Code 흐름, 리디렉션 URI, 동의 화면.
- `CustomOAuth2UserService` · `OAuth2UserInfoFactory`로 Provider별 JSON 차이 흡수.

### Stage 3 이후 (설계)

- `ARCHITECTURE.md`의 Application·Domain·JWT·Redis 계층으로 확장.
- REST API와 브라우저 클라이언트를 나눌 경우 **CORS·쿠키·CSRF** 전략을 별도로 정리.

---

## 6. 안티패턴 메모 (교재용)

| 안티패턴 | 이유 |
|----------|------|
| 클라이언트 시크릿을 Git에 커밋 | 즉시 유출로 간주하고 프로바이더에서 폐기·재발급 |
| OAuth 액세스 토큰을 프런트에 장기 보관 | 우리 API용과 혼동·탈취 면적 증가 |
| JWT에 과도한 PII·권한 상세를 장기 저장 | 토큰 크기·유출 시 피해 확대 |
| Refresh Token을 로컬스토리지에만 저장 | XSS에 취약 — HttpOnly 쿠키·서버 저장과 병행 검토 |

---

## 7. 참고 표준·문서

- [RFC 6749](https://datatracker.ietf.org/doc/html/rfc6749) — OAuth 2.0  
- [RFC 7519](https://datatracker.ietf.org/doc/html/rfc7519) — JWT  
- [Spring Security OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)

---

## 8. 보안 점검 (배포 전)

- [ ] 저장소에 비밀값 없음 — CI에서 시크릿 스캔  
- [ ] 프로바이더 콘솔의 Redirect URI가 배포 URL과 일치  
- [ ] HTTPS (프로덕션)  
- [ ] 로그에 토큰·비밀번호·전체 Authorization 헤더 미출력  

---

## 9. 자주 나오는 오류 (로컬 OAuth)

| 증상 | 점검 |
|------|------|
| `redirect_uri_mismatch` | 콘솔에 등록한 Redirect URI가 위 표와 **문자 하나까지** 동일한지 (스킴·포트·끝 `/` 유무 포함). |
| `invalid_client` / 클라이언트를 찾을 수 없음 | `.env` 또는 `application-local.yaml`의 ID·Secret 공백·오타. Google은 **OAuth 동의 화면**에서 테스트 사용자에 본인 계정이 등록되어 있는지. |
| 포트 충돌 | 다른 프로세스 종료 또는 `server.port` 변경 후 Redirect URI도 같은 호스트·포트로 맞춤. |

---

## 10. 비밀값이 과거에 커밋된 경우

히스토리에 클라이언트 시크릿이 남아 있다면 **삭제 커밋만으로는 부족**할 수 있습니다. 해당 OAuth 클라이언트는 프로바이더 콘솔에서 **폐기 후 새 자격 증명**을 발급하고, `.env` 또는 로컬 전용 설정으로만 보관하세요.
