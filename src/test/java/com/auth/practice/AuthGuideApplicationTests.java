package com.auth.practice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest(properties = {
		"spring.profiles.active=cookie",
		"spring.security.oauth2.client.registration.google.client-id=test-google-client-id",
		"spring.security.oauth2.client.registration.google.client-secret=test-google-client-secret",
		"spring.security.oauth2.client.registration.kakao.client-id=test-kakao-client-id",
		"spring.security.oauth2.client.registration.kakao.client-secret=test-kakao-client-secret",
		"spring.security.oauth2.client.registration.naver.client-id=test-naver-client-id",
		"spring.security.oauth2.client.registration.naver.client-secret=test-naver-client-secret",
		"jwt.secret=test-secret-key-must-be-at-least-256-bits-long-for-hs256",
		"spring.datasource.url=jdbc:h2:mem:authguide;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
class AuthGuideApplicationTests {

	@MockBean
	private StringRedisTemplate stringRedisTemplate;

	@Test
	void contextLoads() {
	}

}
