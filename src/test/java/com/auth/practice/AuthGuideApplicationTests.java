package com.auth.practice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.security.oauth2.client.registration.google.client-id=test-google-client-id",
		"spring.security.oauth2.client.registration.google.client-secret=test-google-client-secret",
		"spring.security.oauth2.client.registration.kakao.client-id=test-kakao-client-id",
		"spring.security.oauth2.client.registration.kakao.client-secret=test-kakao-client-secret",
		"spring.security.oauth2.client.registration.naver.client-id=test-naver-client-id",
		"spring.security.oauth2.client.registration.naver.client-secret=test-naver-client-secret"
})
class AuthGuideApplicationTests {

	@Test
	void contextLoads() {
	}

}
