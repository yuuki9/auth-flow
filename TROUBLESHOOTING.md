# OAuth 로그인 문제 해결 가이드

## 에러: "The OAuth client was not found" (401: invalid_client)

이 에러는 Google이 제공된 클라이언트 ID를 인식하지 못할 때 발생합니다.

### 해결 방법 1: 리디렉션 URI 확인 (가장 흔한 원인)

1. [Google Cloud Console - 사용자 인증 정보](https://console.cloud.google.com/apis/credentials) 접속
2. OAuth 2.0 클라이언트 ID 클릭
3. **승인된 리디렉션 URI**에 정확히 다음 추가:
   ```
   http://localhost:8080/login/oauth2/code/google
   ```
   - ✅ `http://localhost:8080/login/oauth2/code/google` (정확)
   - ❌ `http://localhost:8080/login/oauth2/code/google/` (끝에 / 있음)
   - ❌ `https://localhost:8080/login/oauth2/code/google` (https)
   - ❌ `http://localhost/login/oauth2/code/google` (포트 번호 없음)
4. **저장** 버튼 클릭

### 해결 방법 2: 테스트 사용자 추가

1. 좌측 메뉴: **OAuth 동의 화면** 클릭
2. 아래로 스크롤: **테스트 사용자** 섹션
3. 로그인할 Google 계정 추가 (예: wlsghtjd23@gmail.com)
4. **저장** 클릭

### 해결 방법 3: 새 OAuth 클라이언트 생성

기존 클라이언트에 문제가 있을 경우:

#### 1단계: OAuth 동의 화면 구성

1. [Google Cloud Console](https://console.cloud.google.com/) 접속
2. 프로젝트 선택 (또는 새 프로젝트 생성)
3. 좌측 메뉴: **API 및 서비스** → **OAuth 동의 화면**
4. User Type: **외부** 선택 → **만들기**
5. 앱 정보 입력:
   - **앱 이름**: Auth Guide Practice
   - **사용자 지원 이메일**: 본인 Gmail 주소
   - **개발자 연락처 정보**: 본인 Gmail 주소
6. **저장 후 계속** 클릭
7. 범위: 기본값 유지 → **저장 후 계속**
8. 테스트 사용자: **ADD USERS** 클릭 → 본인 Gmail 주소 추가
9. **저장 후 계속** 클릭

#### 2단계: OAuth 클라이언트 ID 생성

1. 좌측 메뉴: **API 및 서비스** → **사용자 인증 정보**
2. 상단: **사용자 인증 정보 만들기** → **OAuth 클라이언트 ID**
3. 애플리케이션 유형: **웹 애플리케이션**
4. 이름: Auth Guide Web Client
5. **승인된 리디렉션 URI** 추가:
   ```
   http://localhost:8080/login/oauth2/code/google
   ```
6. **만들기** 클릭
7. **클라이언트 ID**와 **클라이언트 보안 비밀** 복사

#### 3단계: application.yaml 업데이트

`src/main/resources/application.yaml` 파일 수정:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: 새로-발급받은-클라이언트-ID
            client-secret: 새로-발급받은-클라이언트-시크릿
```

#### 4단계: 애플리케이션 재시작

```bash
# 현재 실행 중인 애플리케이션 중지 (Ctrl+C)
# 다시 실행
./gradlew bootRun
```

#### 5단계: 테스트

브라우저에서 `http://localhost:8080` 접속하여 로그인 테스트

### 추가 확인 사항

1. **프로젝트 확인**: Google Cloud Console에서 올바른 프로젝트를 선택했는지 확인
2. **클라이언트 상태**: OAuth 클라이언트가 삭제되지 않았는지 확인
3. **브라우저 캐시**: 브라우저 캐시 삭제 후 다시 시도
4. **시크릿 모드**: 시크릿/프라이빗 모드에서 테스트

### 로그 확인

애플리케이션 로그에서 자세한 오류 확인:

```bash
./gradlew bootRun
```

로그에서 다음과 같은 줄 확인:
- `client-id` 값이 올바르게 로드되었는지
- 리디렉션 URL이 정확한지

## 여전히 문제가 있다면

1. Google Cloud Console에서 클라이언트 ID 전체를 다시 복사
2. `application.yaml`에서 공백이나 줄바꿈 없이 정확히 붙여넣기
3. 애플리케이션 완전히 종료 후 재시작
4. 브라우저 캐시 삭제 및 시크릿 모드에서 테스트
