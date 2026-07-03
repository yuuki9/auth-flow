package com.auth.practice.infrastructure.security.storage;

import com.auth.practice.presentation.dto.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

// [학습] 패턴마다 달라지는 "Refresh Token 처리" 3가지를 추상화.
//        AuthController는 이 인터페이스 하나만 알면 된다.
//
//  cookie     : 읽기=쿠키, 쓰기=쿠키 (JS 접근 불가)
//  memory     : 읽기=쿠키, 쓰기=refresh는 쿠키 + access는 body (탭 닫으면 소멸)
//  localstorage: 읽기=body, 쓰기=body (XSS 취약)
public interface RefreshTokenHandler {

    // 요청에서 Refresh Token을 추출한다.
    // bodyToken: localstorage 패턴에서 클라이언트가 body에 담아 보낸 값 (null 가능)
    String extractRefreshToken(HttpServletRequest request, String bodyToken);

    // 새 토큰을 응답에 기록하고, 컨트롤러가 반환할 body를 구성한다.
    Map<String, Object> buildRefreshResponse(HttpServletResponse response, TokenResponse tokens);

    // 로그아웃 시 토큰 정리 (쿠키 삭제 등 패턴별 처리)
    void clearOnLogout(HttpServletResponse response);
}
