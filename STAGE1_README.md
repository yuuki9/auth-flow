# Stage 1: OAuth 2.0 기본 이해 (구글 로그인)

## 구현 완료 항목

### 1. 프로젝트 구조
```
src/main/java/com/auth/practice/
├── infrastructure/security/
│   ├── config/SecurityConfig.java
│   └── oauth/OAuth2AuthenticationSuccessHandler.java
└── presentation/controller/HomeController.java

src/main/resources/
├── application.yaml
└── templates/
    ├── index.html
    ├── login.html
    └── home.html
```

### 2. 구현 기능
- [x] Spring Security OAuth2 Client 설정
- [x] 구글 OAuth 2.0 연동 설정
- [x] 로그인 성공 핸들러 구현
- [x] 홈 컨트롤러 구현 (메인, 로그인, 홈 페이지)
- [x] Thymeleaf 템플릿 구현
  - index.html: 메인 페이지
  - login.html: 로그인 페이지 (구글 로그인 버튼)
  - home.html: 로그인 성공 후 사용자 정보 표시
- [x] 세션 기반 로그아웃 기능

## 설정 방법

### 1. Google OAuth 2.0 클라이언트 생성

1. [Google Cloud Console](https://console.cloud.google.com/) 접속
2. 프로젝트 생성 (또는 기존 프로젝트 선택)
3. "API 및 서비스" > "사용자 인증 정보" 이동
4. "사용자 인증 정보 만들기" > "OAuth 클라이언트 ID" 선택
5. 애플리케이션 유형: "웹 애플리케이션"
6. 승인된 리디렉션 URI 추가:
   - `http://localhost:8080/login/oauth2/code/google`
7. 클라이언트 ID와 클라이언트 보안 비밀번호 복사

### 2. 환경 변수 설정

`.env` 파일 생성 (또는 `.env.example` 복사):

```bash
GOOGLE_CLIENT_ID=발급받은-클라이언트-ID
GOOGLE_CLIENT_SECRET=발급받은-클라이언트-시크릿
```

또는 `application.yaml`에서 직접 수정:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: 발급받은-클라이언트-ID
            client-secret: 발급받은-클라이언트-시크릿
```

### 3. 애플리케이션 실행

```bash
# Gradle로 실행
./gradlew bootRun

# 또는 IDE에서 AuthGuideApplication 실행
```

### 4. 테스트

1. 브라우저에서 `http://localhost:8080` 접속
2. "로그인 시작하기" 버튼 클릭
3. "구글로 로그인" 버튼 클릭
4. 구글 계정으로 로그인
5. 로그인 성공 후 사용자 정보 확인 (이름, 이메일, 프로필 이미지)
6. 로그아웃 테스트

## OAuth 2.0 Flow 설명

### Authorization Code Grant Flow

```
1. 사용자: "구글로 로그인" 버튼 클릭
   ↓
2. 앱 → Google: Authorization 요청
   GET https://accounts.google.com/o/oauth2/v2/auth?
       client_id={CLIENT_ID}
       &redirect_uri=http://localhost:8080/login/oauth2/code/google
       &response_type=code
       &scope=email profile
   ↓
3. 사용자: Google 로그인 & 권한 동의
   ↓
4. Google → 앱: Authorization Code 발급
   Redirect: http://localhost:8080/login/oauth2/code/google?code={CODE}
   ↓
5. 앱 → Google: Token 요청
   POST https://oauth2.googleapis.com/token
   Body: {
     code: {CODE},
     client_id: {CLIENT_ID},
     client_secret: {CLIENT_SECRET},
     redirect_uri: http://localhost:8080/login/oauth2/code/google,
     grant_type: authorization_code
   }
   ↓
6. Google → 앱: Access Token 발급
   {
     "access_token": "ya29.a0...",
     "expires_in": 3599,
     "scope": "email profile openid",
     "token_type": "Bearer",
     "id_token": "eyJhbG..."
   }
   ↓
7. 앱 → Google: 사용자 정보 요청
   GET https://www.googleapis.com/oauth2/v3/userinfo
   Header: Authorization: Bearer {ACCESS_TOKEN}
   ↓
8. Google → 앱: 사용자 정보 응답
   {
     "sub": "1234567890",
     "name": "홍길동",
     "email": "hong@gmail.com",
     "picture": "https://..."
   }
   ↓
9. 앱: 사용자 정보로 세션 생성 & 홈 페이지로 리다이렉트
```

## 핵심 코드 설명

### SecurityConfig.java
- Spring Security 설정
- OAuth2 로그인 활성화
- URL별 권한 설정
- 로그아웃 설정

### OAuth2AuthenticationSuccessHandler.java
- 로그인 성공 후 처리 로직
- OAuth2User에서 사용자 정보 추출
- 로깅 및 리다이렉트

### HomeController.java
- 메인 페이지, 로그인 페이지, 홈 페이지 라우팅
- 사용자 정보를 뷰에 전달

## 다음 단계 (Stage 2)

- 카카오, 네이버 OAuth 2.0 추가 연동
- Provider별 사용자 정보 통합 처리
- OAuth2UserInfo 인터페이스 설계

## 문제 해결

### 로그인 버튼 클릭 시 오류
- Google Cloud Console에서 OAuth 클라이언트 ID 생성 확인
- 리다이렉트 URI 정확히 설정 (`http://localhost:8080/login/oauth2/code/google`)
- application.yaml의 client-id, client-secret 확인

### 로그인 성공 후 오류
- Thymeleaf 의존성 확인
- templates 폴더 경로 확인
- 로그에서 상세 오류 확인 (logging.level.org.springframework.security=DEBUG)
