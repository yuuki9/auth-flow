package com.auth.practice.application.auth;

import com.auth.practice.domain.user.User;
import com.auth.practice.domain.user.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

// [왜?] UserDetailsService를 구현하는 이유:
//        Spring Security의 AuthenticationManager가 이 인터페이스를 통해
//        "DB에서 사용자 조회 → BCrypt 비밀번호 비교"를 자동으로 수행한다.
//        직접 BCrypt를 호출하지 않고 Spring Security에 위임 → 검증 로직 표준화.
@Service
public class LoginService implements UserDetailsService {

    private final UserRepository userRepository;

    public LoginService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // [왜?] loadUserByUsername의 "username" 파라미터로 email을 사용한다.
    //        Spring Security의 인터페이스 명칭이 username이지만 실제로는 식별자 역할.
    //        이 프로젝트에서는 email이 로그인 식별자.
    // [보안] UsernameNotFoundException을 던지면 Spring Security가 내부적으로
    //        BadCredentialsException으로 변환 → 사용자에게 "아이디가 없다"는 힌트를 주지 않음.
    //        (User Enumeration 공격 방지)
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자 없음: " + email));

        // [왜?] username 자리에 userId(String)를 넣는다.
        //        인증 성공 후 Authentication.getName()으로 userId를 꺼내
        //        AuthService.issueTokens(userId)를 호출하기 위함.
        //        email을 username으로 쓰면 이후 조회에서 email → userId 변환이 추가로 필요.
        return new org.springframework.security.core.userdetails.User(
                user.getId().toString(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
