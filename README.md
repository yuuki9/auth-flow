# 🔐 OAuth 2.0 인증 시스템 학습 프로젝트

> **Hybrid 전략**: Stateless JWT (Access Token) + Redis (Refresh Token)

Spring Boot, Spring Security, OAuth 2.0을 사용한 실무 수준의 소셜 로그인 및 인증 시스템 구현 학습 프로젝트입니다.

## 🎯 프로젝트 목표

- OAuth 2.0 Authorization Code Grant Flow 완벽 이해
- 다중 소셜 로그인 Provider 통합 (Google, Kakao, Naver)
- JWT Access Token (Stateless) + Redis Refresh Token (Stateful) Hybrid 전략
- 멀티 디바이스 세션 관리
- 프로덕션 수준의 보안 및 배포 구성

## 📊 현재 진행 상황

### ✅ Stage 1: OAuth 2.0 기본 이해 (구글 로그인) - **완료**

- [x] Spring Security OAuth2 Client 설정
- [x] 구글 OAuth 2.0 연동
- [x] Authorization Code Grant Flow 구현
- [x] 세션 기반 로그인/로그아웃
- [x] 사용자 정보 표시 (이름, 이메일, 프로필 이미지)

### 📋 다음 Stage

- [ ] **Stage 2**: 다중 Provider 지원 (카카오, 네이버)
- [ ] **Stage 3**: 데이터베이스 연동 & 회원 관리
- [ ] **Stage 4**: JWT Access Token 구현
- [ ] **Stage 5**: Redis 연동 준비
- [ ] **Stage 6**: Refresh Token + Redis 저장
- [ ] **Stage 7**: 로그아웃 & 토큰 무효화
- [ ] **Stage 8**: 멀티 디바이스 세션 관리
- [ ] **Stage 9**: 보안 강화 (PKCE, Rate Limit)
- [ ] **Stage 10**: 프로덕션 배포 준비

## 🚀 빠른 시작

### 사전 요구사항

- Java 17+
- Gradle 8.x
- Google Cloud Console 계정

### 1. Google OAuth 2.0 클라이언트 생성

1. [Google Cloud Console](https://console.cloud.google.com/) 접속
2. 프로젝트 생성
3. OAuth 동의 화면 구성
4. OAuth 클라이언트 ID 생성 (웹 애플리케이션)
5. 승인된 리디렉션 URI 추가:
   ```
   http://localhost:8080/login/oauth2/code/google
   ```

### 2. 환경 설정

`src/main/resources/application.yaml` 수정:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: YOUR_GOOGLE_CLIENT_ID
            client-secret: YOUR_GOOGLE_CLIENT_SECRET
```

### 3. 애플리케이션 실행

```bash
# Windows
.\gradlew bootRun

# Mac/Linux
./gradlew bootRun
```

### 4. 브라우저에서 접속

```
http://localhost:8080
```

## 📂 프로젝트 구조

```
src/main/java/com/auth/practice/
├── AuthGuideApplication.java
├── infrastructure/
│   └── security/
│       ├── config/SecurityConfig.java
│       └── oauth/OAuth2AuthenticationSuccessHandler.java
└── presentation/
    └── controller/HomeController.java

src/main/resources/
├── application.yaml
└── templates/
    ├── index.html
    ├── login.html
    └── home.html
```

## 📚 문서

- [전체 Stage 로드맵](STAGE_ROADMAP.md) - 10단계 학습 로드맵
- [빠른 시작 가이드](QUICKSTART.md) - Stage 1 실행 가이드
- [Stage 1 상세 가이드](STAGE1_README.md) - OAuth 2.0 기본 구현
- [프로젝트 구조](PROJECT_STRUCTURE.md) - 파일 구조 설명
- [아키텍처 문서](ARCHITECTURE.md) - 전체 시스템 아키텍처

## 🛠️ 기술 스택

### 현재 (Stage 1)
- **Java 17**
- **Spring Boot 3.5.x**
- **Spring Security 6.x**
- **Spring OAuth2 Client**
- **Thymeleaf**

### 향후 추가 예정
- PostgreSQL / H2
- Spring Data JPA
- Redis
- JWT (jjwt)
- Docker
- Prometheus + Grafana

## 🧪 테스트

### Stage 1 기능 테스트

1. **메인 페이지 접속**: http://localhost:8080
2. **로그인 시작**: "로그인 시작하기" 버튼 클릭
3. **구글 로그인**: "구글로 로그인" 버튼 클릭
4. **로그인 성공**: 사용자 정보 표시 확인
5. **로그아웃**: 로그아웃 버튼 클릭 → 메인으로 이동

## 🔍 주요 학습 내용

### Stage 1에서 학습한 내용

#### OAuth 2.0 개념
- Authorization Server, Resource Server, Client 역할
- Authorization Code Grant Flow
- Redirect URI와 콜백 처리

#### Spring Security OAuth2 Client
- `oauth2Login()` 설정
- OAuth2User 정보 추출
- 세션 기반 인증

#### 구글 OAuth 연동
- Google Cloud Console 설정
- 클라이언트 ID/Secret 관리
- 사용자 정보 API (email, profile, picture)

## 📖 OAuth 2.0 Flow (Stage 1)

```
┌─────────┐                                           ┌─────────┐
│         │                                           │         │
│  사용자   │◄─────────────(1) 로그인 페이지 ─────────────│   앱    │
│         │                                           │         │
└────┬────┘                                           └────┬────┘
     │                                                     │
     │ (2) 구글 로그인 버튼 클릭                              │
     │                                                     │
     └────────────────────────────────────────────────────►│
                                                           │
                                                           │ (3) Google로 리다이렉트
                                                           │
┌─────────┐                                           ┌────▼────┐
│         │◄────────(4) Authorization 요청──────────────│         │
│ Google  │                                           │   앱    │
│  OAuth  │─────────(5) Authorization Code────────────►│         │
│         │                                           └────┬────┘
└─────────┘                                                │
     ▲                                                     │
     │                                                     │
     │          (6) Access Token 요청                      │
     │◄────────────────────────────────────────────────────┘
     │
     │          (7) Access Token 발급
     └─────────────────────────────────────────────────────►
                                                           │
                                                           │ (8) 사용자 정보 요청
                                                           │
┌─────────┐                                           ┌────▼────┐
│         │◄────────(9) 사용자 정보 응답───────────────────│         │
│ Google  │                                           │   앱    │
│UserInfo │                                           │         │
│   API   │                                           └────┬────┘
└─────────┘                                                │
                                                           │ (10) 세션 생성 & 홈으로 이동
                                                           │
┌─────────┐                                           ┌────▼────┐
│         │◄─────────(11) 홈 페이지 표시─────────────────│         │
│  사용자   │                                           │   앱    │
│         │                                           │         │
└─────────┘                                           └─────────┘
```

## 🎯 다음 단계

Stage 1을 완료하셨다면 Stage 2로 진행하세요:

- 카카오, 네이버 OAuth 2.0 추가
- Provider별 사용자 정보 통합 처리
- OAuth2UserInfo 인터페이스 설계
- Factory 패턴 적용

## 🐛 문제 해결

### 자주 발생하는 오류

#### `redirect_uri_mismatch`
- Google Cloud Console의 리디렉션 URI 확인
- `http://localhost:8080/login/oauth2/code/google` 정확히 입력

#### `invalid_client`
- application.yaml의 client-id, client-secret 확인
- 공백이나 줄바꿈 없이 정확히 복사

#### `Port 8080 is already in use`
- 다른 애플리케이션 종료 또는 포트 변경
- application.yaml에 `server.port: 8081` 추가

## 📞 도움말

- [Spring Security 공식 문서](https://spring.io/projects/spring-security)
- [OAuth 2.0 RFC 6749](https://datatracker.ietf.org/doc/html/rfc6749)
- [Google OAuth 2.0 가이드](https://developers.google.com/identity/protocols/oauth2)

## 📝 라이선스

MIT License

---

**현재 진행 상황**: Stage 1 완료 ✅

**다음 학습**: Stage 2 - 다중 Provider 지원
