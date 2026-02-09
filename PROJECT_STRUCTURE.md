# Stage 1 - 프로젝트 구조

```
auth-guide/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── auth/
│   │   │           └── practice/
│   │   │               ├── AuthGuideApplication.java        # 메인 애플리케이션 클래스
│   │   │               ├── infrastructure/
│   │   │               │   └── security/
│   │   │               │       ├── config/
│   │   │               │       │   └── SecurityConfig.java   # Spring Security 설정
│   │   │               │       └── oauth/
│   │   │               │           └── OAuth2AuthenticationSuccessHandler.java  # 로그인 성공 핸들러
│   │   │               └── presentation/
│   │   │                   └── controller/
│   │   │                       └── HomeController.java      # 메인/로그인/홈 페이지 컨트롤러
│   │   └── resources/
│   │       ├── application.yaml                             # Spring Boot 설정
│   │       └── templates/                                   # Thymeleaf 템플릿
│   │           ├── index.html                               # 메인 페이지
│   │           ├── login.html                               # 로그인 페이지
│   │           └── home.html                                # 로그인 후 홈 페이지
│   └── test/
│       └── java/
│           └── com/
│               └── auth/
│                   └── practice/
│                       └── AuthGuideApplicationTests.java
├── build.gradle.kts                                         # Gradle 빌드 설정
├── settings.gradle.kts
├── gradlew                                                  # Gradle Wrapper (Unix)
├── gradlew.bat                                              # Gradle Wrapper (Windows)
├── .gitignore
├── .env.example                                             # 환경 변수 예시
├── STAGE_ROADMAP.md                                         # 전체 Stage 로드맵
├── STAGE1_README.md                                         # Stage 1 상세 가이드
├── QUICKSTART.md                                            # 빠른 시작 가이드
└── ARCHITECTURE.md                                          # 아키텍처 문서
```

## 주요 파일 설명

### Java 클래스

#### `AuthGuideApplication.java`
- Spring Boot 메인 클래스
- `@SpringBootApplication` 어노테이션

#### `SecurityConfig.java`
- Spring Security 보안 설정
- OAuth2 로그인 활성화
- URL별 권한 설정
- 로그아웃 설정

#### `OAuth2AuthenticationSuccessHandler.java`
- OAuth 로그인 성공 후 처리
- 사용자 정보 추출 및 로깅
- 홈 페이지로 리다이렉트

#### `HomeController.java`
- `/` - 메인 페이지
- `/login` - 로그인 페이지
- `/home` - 로그인 후 홈 페이지
- `@AuthenticationPrincipal`로 사용자 정보 주입

### 설정 파일

#### `application.yaml`
- Spring Boot 기본 설정
- OAuth2 클라이언트 설정 (Google)
- 로깅 레벨 설정

#### `build.gradle.kts`
- 프로젝트 의존성 관리
- Spring Boot 3.5.x
- Spring Security OAuth2 Client
- Thymeleaf 템플릿 엔진

### HTML 템플릿

#### `index.html`
- 메인 랜딩 페이지
- Stage 1 소개
- 로그인 시작 버튼

#### `login.html`
- 로그인 페이지
- 구글 로그인 버튼
- 설정 안내 정보

#### `home.html`
- 로그인 성공 페이지
- 사용자 프로필 정보 표시
- 로그아웃 버튼

## 레이어 아키텍처

### Presentation Layer (표현 계층)
- `HomeController.java`
- HTML 템플릿 (index.html, login.html, home.html)

### Infrastructure Layer (인프라 계층)
- `SecurityConfig.java` - Spring Security 설정
- `OAuth2AuthenticationSuccessHandler.java` - OAuth 핸들러

### 의존성 흐름

```
HomeController
      ↓
SecurityConfig
      ↓
OAuth2AuthenticationSuccessHandler
```

## Stage 2에서 추가될 구조

```
src/main/java/com/auth/practice/
├── domain/
│   └── oauth/
│       ├── OAuth2Provider.java (Enum)
│       └── OAuth2UserInfo.java (Interface)
├── infrastructure/
│   └── security/
│       └── oauth/
│           ├── CustomOAuth2UserService.java
│           └── userinfo/
│               ├── OAuth2UserInfoFactory.java
│               ├── GoogleOAuth2UserInfo.java
│               ├── KakaoOAuth2UserInfo.java
│               └── NaverOAuth2UserInfo.java
└── presentation/
    └── controller/
        └── OAuth2Controller.java
```
