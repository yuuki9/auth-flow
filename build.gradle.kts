plugins {
	java
	id("org.springframework.boot") version "3.5.11-SNAPSHOT"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.auth.practice"
version = "0.0.1-SNAPSHOT"
description = "OAuth 2.0, JWT, OpenID Connect"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
	maven { url = uri("https://repo.spring.io/snapshot") }
}

dependencies {
	// OAuth2 Client (구글 로그인 용)
	implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-web")
	
	// Thymeleaf (HTML 템플릿 엔진)
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")
	
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
