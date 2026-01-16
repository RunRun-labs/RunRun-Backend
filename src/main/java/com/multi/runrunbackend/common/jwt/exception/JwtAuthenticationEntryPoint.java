package com.multi.runrunbackend.common.jwt.exception;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException authException) throws IOException, ServletException {
    log.warn("인증 실패: 인증되지 않은 사용자가 {} 경로에 접근하려고 했습니다. 401 Unauthorized 반환.",
        request.getRequestURI());
    if (response.isCommitted()) {
      return; // :흰색_확인_표시: 이미 커밋됐으면 아무 것도 쓰지 않음
    }

    // HTML 페이지 요청인지 확인 (Accept 헤더 또는 Content-Type 확인)
    String acceptHeader = request.getHeader("Accept");
    boolean isHtmlRequest = acceptHeader != null && acceptHeader.contains("text/html");
    
    // HTML 요청이면 login으로 리다이렉트
    if (isHtmlRequest && !request.getRequestURI().startsWith("/api")) {
      response.sendRedirect("/login");
      return;
    }

    // API 요청이면 JSON 응답
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write("{\"error\": \"인증되지 않은 사용자입니다. 로그인 후 다시 시도해 주세요.\"}");
    response.getWriter().flush();
  }
}
