# 🚀 Stage 1 구현 완료 - 빠른 시작 가이드

## ✅ 구현 완료 항목

- **Spring Security OAuth2 Client 설정**
- **구글 OAuth 2.0 연동**
- **로그인/로그아웃 기능**
- **사용자 정보 표시**
- **세션 기반 인증**

---

## 📋 시작하기 전에

### 1. Google OAuth 2.0 클라이언트 생성

애플리케이션을 실행하기 전에 **반드시** Google Cloud Console에서 OAuth 클라이언트를 생성해야 합니다.

#### 단계별 가이드:

1. **Google Cloud Console 접속**
   - https://console.cloud.google.com/

2. **프로젝트 생성**
   - 새 프로젝트 만들기 또는 기존 프로젝트 선택

3. **OAuth 동의 화면 구성**
   - 왼쪽 메뉴 → "API 및 서비스" → "OAuth 동의 화면"
   - 사용자 유형: "외부" 선택
   - 앱 이름: "Auth Guide" (원하는 이름)
   - 사용자 지원 이메일: 본인 이메일
   - 범위: `/auth/userinfo.email`, `/auth/userinfo.profile` 추가

4. **OAuth 클라이언트 ID 만들기**
   - 왼쪽 메뉴 → "API 및 서비스" → "사용자 인증 정보"
   - "사용자 인증 정보 만들기" → "OAuth 클라이언트 ID"
   - 애플리케이션 유형: **웹 애플리케이션**
   - 이름: "Auth Guide Web Client"
   
5. **승인된 리디렉션 URI 추가** (중요!)
   ```
   http://localhost:8080/login/oauth2/code/google
   ```

6. **클라이언트 ID와 클라이언트 보안 비밀번호 복사**
   - 생성 후 표시되는 값을 복사해두세요

---

## ⚙️ 환경 설정

### 방법 1: application.yaml 직접 수정 (권장)

`src/main/resources/application.yaml` 파일을 열어 수정:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: YOUR_GOOGLE_CLIENT_ID_HERE
            client-secret: YOUR_GOOGLE_CLIENT_SECRET_HERE
```

### 방법 2: 환경 변수 사용

`.env` 파일 생성 (`.env.example` 복사):

```bash
GOOGLE_CLIENT_ID=YOUR_GOOGLE_CLIENT_ID_HERE
GOOGLE_CLIENT_SECRET=YOUR_GOOGLE_CLIENT_SECRET_HERE
```

---

## 🏃 애플리케이션 실행

### Gradle로 실행 (터미널)

```bash
# Windows
.\gradlew bootRun

# Mac/Linux
./gradlew bootRun
```

### IDE에서 실행

1. `AuthGuideApplication.java` 파일 열기
2. main 메서드 옆 실행 버튼 클릭
3. 또는 `Shift + F10` (IntelliJ) / `Ctrl + F11` (Eclipse)

### 실행 확인

애플리케이션이 성공적으로 시작되면 다음 메시지가 표시됩니다:

```
Started AuthGuideApplication in X.XXX seconds
Tomcat started on port 8080 (http)
```

---

## 🧪 테스트하기

### 1. 메인 페이지 접속

브라우저에서 http://localhost:8080 접속

- Stage 1 배지와 함께 메인 페이지 표시
- "로그인 시작하기" 버튼 확인

### 2. 로그인 페이지

"로그인 시작하기" 클릭

- 구글 로그인 버튼 표시
- 설정 안내 메시지 확인

### 3. 구글 로그인

"구글로 로그인" 버튼 클릭

- Google 로그인 페이지로 리다이렉트
- Google 계정으로 로그인
- 권한 동의 (이메일, 프로필 접근)

### 4. 로그인 성공

로그인 성공 후 홈 페이지 표시:

- ✓ 로그인 성공 배지
- ✓ 프로필 이미지
- ✓ 사용자 이름
- ✓ 이메일 주소
- ✓ OAuth2 사용자 정보 (JSON 형식)

### 5. 로그아웃

"로그아웃" 버튼 클릭

- 세션 종료
- 메인 페이지로 리다이렉트

---

## 📊 동작 확인

### 콘솔 로그 확인

애플리케이션 실행 중 콘솔에서 다음 로그를 확인할 수 있습니다:

```
OAuth2 로그인 성공!
사용자 이름: 홍길동
사용자 이메일: hong@gmail.com
프로필 이미지: https://lh3.googleusercontent.com/...
```

### 브라우저 개발자 도구

1. F12 키로 개발자 도구 열기
2. Network 탭에서 OAuth 요청 흐름 확인:
   - `/oauth2/authorization/google`
   - Google OAuth 서버 요청
   - `/login/oauth2/code/google?code=...` (리다이렉트)
   - `/home` (최종 페이지)

---

## 🎯 Stage 1 완료 체크리스트

- [ ] Google OAuth 2.0 클라이언트 등록 완료
- [ ] 구글 로그인 버튼 클릭 시 구글 로그인 페이지로 리다이렉트
- [ ] 로그인 성공 후 사용자 이메일 화면에 표시
- [ ] 로그아웃 기능 동작 (세션 기반)
- [ ] 프로필 이미지 표시
- [ ] OAuth 2.0 Flow 이해

---

## 🔍 문제 해결

### 로그인 버튼 클릭 시 오류

**오류**: `[invalid_client] ... redirect_uri_mismatch`

**원인**: 리다이렉트 URI가 Google Cloud Console 설정과 일치하지 않음

**해결**:
1. Google Cloud Console에서 OAuth 클라이언트 설정 확인
2. 승인된 리디렉션 URI에 정확히 추가:
   ```
   http://localhost:8080/login/oauth2/code/google
   ```
3. 대소문자, 슬래시(/) 포함 정확히 일치해야 함

---

### 클라이언트 ID/Secret 오류

**오류**: `[invalid_client] The OAuth client was not found.`

**원인**: application.yaml의 클라이언트 ID 또는 Secret이 잘못됨

**해결**:
1. Google Cloud Console에서 클라이언트 ID 재확인
2. application.yaml에 정확히 복사
3. 공백이나 줄바꿈 없는지 확인
4. 애플리케이션 재시작

---

### 포트 충돌 오류

**오류**: `Port 8080 is already in use`

**원인**: 8080 포트를 다른 애플리케이션이 사용 중

**해결**:

**방법 1**: 다른 애플리케이션 종료

**방법 2**: 포트 변경 - application.yaml에 추가:
```yaml
server:
  port: 8081
```
그리고 Google Cloud Console의 리다이렉트 URI도 변경:
```
http://localhost:8081/login/oauth2/code/google
```

---

### Thymeleaf 템플릿 오류

**오류**: `Error resolving template "index"`

**원인**: 템플릿 파일이 올바른 위치에 없음

**해결**:
1. 파일 위치 확인:
   ```
   src/main/resources/templates/
   ├── index.html
   ├── login.html
   └── home.html
   ```
2. Gradle 빌드 재실행: `./gradlew clean build`

---

## 📚 학습 포인트

### OAuth 2.0 Flow 이해

이번 Stage에서 구현한 **Authorization Code Grant Flow**:

```
1. 사용자가 "구글로 로그인" 클릭
2. Spring Security가 Google Authorization Server로 리다이렉트
3. 사용자가 Google에서 로그인 & 권한 동의
4. Google이 Authorization Code 발급
5. Spring Security가 Code로 Access Token 교환
6. Access Token으로 사용자 정보 조회
7. 세션 생성 & 홈 페이지로 리다이렉트
```

### Spring Security OAuth2 Client

- `@EnableWebSecurity`: Security 설정 활성화
- `oauth2Login()`: OAuth2 로그인 기능 활성화
- `OAuth2AuthenticationSuccessHandler`: 로그인 성공 처리
- `OAuth2User`: 인증된 사용자 정보 객체

### 주요 설정

- `client-id`, `client-secret`: OAuth 클라이언트 인증 정보
- `scope`: 요청할 사용자 정보 범위 (email, profile)
- `redirect-uri`: 인증 후 돌아올 URI
- `user-name-attribute`: 사용자 식별자 (Google의 경우 "sub")

---

## 🎓 다음 단계 (Stage 2)

Stage 1을 완료했다면 다음 내용을 학습할 준비가 되었습니다:

- **카카오, 네이버 OAuth 2.0 추가**
- **다중 Provider 지원**
- **Provider별 사용자 정보 통합 처리**
- **OAuth2UserInfo 인터페이스 설계**
- **Factory 패턴 적용**

---

## 💡 참고 자료

- [Spring Security OAuth2 공식 문서](https://spring.io/guides/tutorials/spring-boot-oauth2/)
- [Google OAuth 2.0 가이드](https://developers.google.com/identity/protocols/oauth2)
- [OAuth 2.0 RFC 6749](https://datatracker.ietf.org/doc/html/rfc6749)

---

**축하합니다! Stage 1을 완료했습니다!** 🎉

이제 OAuth 2.0의 기본 개념과 구글 로그인 연동을 이해하셨습니다. Stage 2에서는 여러 소셜 로그인 Provider를 통합하는 방법을 학습합니다.
