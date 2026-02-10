# ✅ Stage 2 완료: 다중 Provider 지원 (카카오, 네이버)

## 🎉 구현 완료!

Stage 2에서는 구글에 이어 **카카오와 네이버 OAuth 2.0 로그인**을 추가했습니다.

---

## 📦 구현된 파일 목록

### 1️⃣ 도메인 모델
```
src/main/java/com/auth/practice/domain/oauth/
├── OAuth2Provider.java          # Provider 열거형 (GOOGLE, KAKAO, NAVER)
└── OAuth2UserInfo.java          # 통합 사용자 정보 인터페이스
```

### 2️⃣ Provider별 UserInfo 구현체
```
src/main/java/com/auth/practice/infrastructure/security/oauth/userinfo/
├── OAuth2UserInfoFactory.java   # Factory 패턴
├── GoogleOAuth2UserInfo.java    # Google 응답 처리
├── KakaoOAuth2UserInfo.java     # Kakao 응답 처리
└── NaverOAuth2UserInfo.java     # Naver 응답 처리
```

### 3️⃣ OAuth2 서비스
```
src/main/java/com/auth/practice/infrastructure/security/oauth/
├── CustomOAuth2UserService.java      # Provider별 사용자 정보 통합 처리
├── CustomOAuth2User.java             # 통합된 OAuth2User
└── OAuth2AuthenticationSuccessHandler.java  # 로그인 성공 핸들러 (업데이트)
```

### 4️⃣ 설정 파일
```
src/main/resources/
├── application.yaml                  # 카카오, 네이버 설정 추가
└── application-local.yaml.example    # 로컬 설정 템플릿
```

### 5️⃣ UI 파일
```
src/main/resources/templates/
├── login.html    # 카카오, 네이버 버튼 추가
└── index.html    # Stage 2 표시
```

---

## 🔑 주요 개념

### 1. Provider별 응답 구조 차이

#### Google
```json
{
  "sub": "1234567890",
  "email": "user@gmail.com",
  "name": "홍길동",
  "picture": "https://lh3.googleusercontent.com/..."
}
```

#### Kakao
```json
{
  "id": 1234567890,
  "kakao_account": {
    "email": "user@kakao.com",
    "profile": {
      "nickname": "홍길동",
      "profile_image_url": "https://k.kakaocdn.net/..."
    }
  }
}
```

#### Naver
```json
{
  "resultcode": "00",
  "message": "success",
  "response": {
    "id": "1234567890",
    "email": "user@naver.com",
    "name": "홍길동",
    "profile_image": "https://phinf.pstatic.net/..."
  }
}
```

### 2. 통합 인터페이스로 추상화

```java
public interface OAuth2UserInfo {
    String getProviderId();
    OAuth2Provider getProvider();
    String getEmail();
    String getName();
    String getProfileImageUrl();
}
```

→ 각 Provider의 복잡한 응답 구조를 **단일 인터페이스로 통합**!

### 3. Factory 패턴으로 Provider별 구현체 생성

```java
public static OAuth2UserInfo of(String registrationId, Map<String, Object> attributes) {
    OAuth2Provider provider = OAuth2Provider.fromRegistrationId(registrationId);
    
    return switch (provider) {
        case GOOGLE -> new GoogleOAuth2UserInfo(attributes);
        case KAKAO -> new KakaoOAuth2UserInfo(attributes);
        case NAVER -> new NaverOAuth2UserInfo(attributes);
    };
}
```

---

## 🧪 테스트 방법

### 사전 준비

#### 1. 카카오 앱 등록
1. https://developers.kakao.com/ 접속
2. **내 애플리케이션 > 애플리케이션 추가하기**
3. **제품 설정 > 카카오 로그인**
   - 활성화 설정: ON
   - Redirect URI: `http://localhost:8080/login/oauth2/code/kakao`
4. **제품 설정 > 카카오 로그인 > 동의항목**
   - 닉네임: 필수 동의
   - 프로필 사진: 선택 동의
   - 카카오계정(이메일): 필수 동의
5. **앱 키 > REST API 키** 복사

#### 2. 네이버 앱 등록
1. https://developers.naver.com/ 접속
2. **Application > 애플리케이션 등록**
3. **사용 API**: 네이버 로그인
4. **로그인 오픈 API 서비스 환경**
   - PC 웹: `http://localhost:8080/login/oauth2/code/naver`
5. **제공 정보 선택**
   - 회원이름
   - 이메일 주소
   - 프로필 사진
6. **Client ID, Client Secret** 복사

#### 3. application-local.yaml 생성

```bash
# 예제 파일 복사
cp src/main/resources/application-local.yaml.example src/main/resources/application-local.yaml
```

실제 값 입력:
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: your-google-client-id
            client-secret: your-google-client-secret
          
          kakao:
            client-id: your-kakao-rest-api-key
            client-secret: ""  # 비워두기 (카카오는 Client Secret 선택사항)
          
          naver:
            client-id: your-naver-client-id
            client-secret: your-naver-client-secret

server:
  port: 8080

logging:
  level:
    org.springframework.security: DEBUG
    org.springframework.security.oauth2: DEBUG
```

---

### 애플리케이션 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 테스트 시나리오

#### 1. 구글 로그인
1. http://localhost:8080/login 접속
2. **"구글로 로그인"** 클릭
3. Google 계정 선택 및 권한 동의
4. 로그인 성공 → 홈 페이지 이동
5. 사용자 정보 확인 (Provider: GOOGLE)

#### 2. 카카오 로그인
1. http://localhost:8080/login 접속
2. **"카카오로 로그인"** 클릭
3. 카카오 계정 로그인 및 동의
4. 로그인 성공 → 홈 페이지 이동
5. 사용자 정보 확인 (Provider: KAKAO)

#### 3. 네이버 로그인
1. http://localhost:8080/login 접속
2. **"네이버로 로그인"** 클릭
3. 네이버 계정 로그인 및 동의
4. 로그인 성공 → 홈 페이지 이동
5. 사용자 정보 확인 (Provider: NAVER)

---

## 📊 로그 확인

### 성공 로그 예시

```
OAuth2 Login - Provider: google, UserNameAttribute: sub
OAuth2 User Info - Provider: GOOGLE, Email: user@gmail.com, Name: 홍길동
OAuth2 로그인 성공!
Provider: GOOGLE
Provider ID: 1234567890
사용자 이름: 홍길동
사용자 이메일: user@gmail.com
프로필 이미지: https://lh3.googleusercontent.com/...
```

```
OAuth2 Login - Provider: kakao, UserNameAttribute: id
OAuth2 User Info - Provider: KAKAO, Email: user@kakao.com, Name: 홍길동
OAuth2 로그인 성공!
Provider: KAKAO
Provider ID: 1234567890
사용자 이름: 홍길동
사용자 이메일: user@kakao.com
프로필 이미지: https://k.kakaocdn.net/...
```

```
OAuth2 Login - Provider: naver, UserNameAttribute: response
OAuth2 User Info - Provider: NAVER, Email: user@naver.com, Name: 홍길동
OAuth2 로그인 성공!
Provider: NAVER
Provider ID: abc123xyz
사용자 이름: 홍길동
사용자 이메일: user@naver.com
프로필 이미지: https://phinf.pstatic.net/...
```

---

## 🔍 트러블슈팅

### ❌ 문제 1: Kakao - "invalid_client"

**원인**: REST API 키가 잘못되었거나 Redirect URI 불일치

**해결**:
1. Kakao Developers > 내 애플리케이션 > 앱 키 > REST API 키 확인
2. 제품 설정 > 카카오 로그인 > Redirect URI 정확히 확인
   ```
   http://localhost:8080/login/oauth2/code/kakao
   ```

### ❌ 문제 2: Naver - "unauthorized"

**원인**: 
- Client ID/Secret 불일치
- 로그인 오픈 API가 비활성화됨
- 제공 정보가 설정되지 않음

**해결**:
1. Naver Developers > Application > 내 애플리케이션 > API 설정 확인
2. 서비스 환경: PC 웹 활성화 확인
3. Callback URL 정확히 확인
   ```
   http://localhost:8080/login/oauth2/code/naver
   ```

### ❌ 문제 3: Kakao - 이메일이 null

**원인**: 카카오계정(이메일) 동의항목이 설정되지 않음

**해결**:
1. Kakao Developers > 제품 설정 > 카카오 로그인 > 동의항목
2. **카카오계정(이메일)** 필수 동의로 설정
3. **비즈니스 채널 추가 및 검수 필요** (개인정보 보호법)
   - 테스트 단계에서는 개발자 계정으로만 테스트 가능

### ❌ 문제 4: Provider 설정 오류

**증상**:
```
java.lang.IllegalArgumentException: Unknown provider: kakao
```

**해결**: application.yaml에 provider 설정 확인
```yaml
provider:
  kakao:
    authorization-uri: https://kauth.kakao.com/oauth/authorize
    token-uri: https://kauth.kakao.com/oauth/token
    user-info-uri: https://kapi.kakao.com/v2/user/me
    user-name-attribute: id
```

---

## 🎯 Stage 2에서 배운 것

### 1. **추상화 (Abstraction)**
- Provider별 다른 응답 구조를 단일 인터페이스로 통합
- `OAuth2UserInfo` 인터페이스로 일관된 접근

### 2. **Factory 패턴 (Factory Pattern)**
- `OAuth2UserInfoFactory`로 Provider별 구현체 생성
- 확장 가능한 구조 (새로운 Provider 추가 용이)

### 3. **전략 패턴 (Strategy Pattern)**
- `CustomOAuth2UserService`가 Provider별 전략 선택
- Spring Security OAuth2 Client와 자연스러운 통합

### 4. **Provider별 특성 이해**
- Google: OpenID Connect 지원, 표준 준수
- Kakao: 독특한 응답 구조 (`kakao_account`)
- Naver: 응답 래핑 (`response` 객체)

---

## 🚀 다음 단계: Stage 3

**Stage 3: 데이터베이스 연동 및 회원 관리**

- JPA로 사용자 정보 DB 저장
- 소셜 로그인 첫 로그인 시 회원 자동 생성
- Provider별 연동 정보 관리 (1:N 관계)
- 동일 이메일로 여러 Provider 로그인 처리

준비되면 말씀해주세요! 🎉
