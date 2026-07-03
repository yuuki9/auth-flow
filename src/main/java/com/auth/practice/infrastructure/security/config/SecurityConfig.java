package com.auth.practice.infrastructure.security.config;

import com.auth.practice.infrastructure.security.jwt.JwtAuthenticationFilter;
import com.auth.practice.infrastructure.security.oauth.CustomOAuth2UserService;
import com.auth.practice.infrastructure.security.oauth.OAuth2SuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// [학습] spring.profiles.active 값(cookie | memory | localstorage)에 따라
//        Spring이 @Profile이 일치하는 구현체 bean을 자동 선택한다.
//        이 파일은 패턴이 바뀌어도 수정할 필요가 없다.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(OAuth2SuccessHandler oAuth2SuccessHandler,
                          CustomOAuth2UserService customOAuth2UserService,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.customOAuth2UserService = customOAuth2UserService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/error", "/oauth2/**", "/login/oauth2/**",
                                 "/index.html").permitAll()
                .requestMatchers("/api/auth/refresh", "/api/auth/logout").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2SuccessHandler)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
