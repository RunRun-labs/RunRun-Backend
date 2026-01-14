package com.multi.runrunbackend.common.jwt.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.multi.runrunbackend.common.exception.custom.TokenException;
import com.multi.runrunbackend.common.exception.dto.ApiExceptionDto;
import com.multi.runrunbackend.common.jwt.provider.TokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
public class JwtFilter extends OncePerRequestFilter {

  public static final String AUTHORIZATION_HEADER = "Authorization";
  public static final String BEARER_PREFIX = "Bearer ";

  private final TokenProvider tokenProvider;
  private final RedisTemplate<String, String> redisTemplate;

  public JwtFilter(TokenProvider tokenProvider, RedisTemplate<String, String> redisTemplate) {
    this.tokenProvider = tokenProvider;
    this.redisTemplate = redisTemplate;
  }

  private static final String[] EXACT_PATHS = {
      "/health-check"
  };

  private static final String[] WILDCARD_PATHS = {
      "/auth/**",
      "/public/**",
      "/swagger-ui/**",
      "/static/**",
      "/css/**",
      "/js/**",
      "/test/**",
      "/favicon/**"

  };

  @Override
  protected boolean shouldNotFilterAsyncDispatch() {
    return false;
  }

  @Override
  protected boolean shouldNotFilterErrorDispatch() {
    return false; // :흰색_확인_표시: ERROR에서도 필터 실행
  }


  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String requestURI = request.getRequestURI();

    try {
      // 정확히 일치하는 경로는 필터를 건너뜀
      for (String exactPath : EXACT_PATHS) {
        if (requestURI.equals(exactPath)) {
          filterChain.doFilter(request, response);
          return;
        }
      }

      // 와일드카드 경로 매칭
      for (String wildcardPath : WILDCARD_PATHS) {
        if (PatternMatchUtils.simpleMatch(wildcardPath, requestURI)) {
          filterChain.doFilter(request, response);
          return;
        }
      }

      String jwt = resolveToken(request);
      String key = "blacklist:" + jwt;
      if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
        log.warn("[JwtFilter] 블랙리스트 토큰 감지 -> 요청 거부: {}", requestURI);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ApiExceptionDto errorResponse = new ApiExceptionDto(
            HttpStatus.UNAUTHORIZED,
            "로그아웃된 토큰입니다 (블랙리스트 등록됨)"
        );

        response.getWriter().write(convertObjectToJson(errorResponse));
        response.getWriter().flush();
        return;
      }

      if (StringUtils.hasText(jwt)) {
        if (tokenProvider.validateToken(jwt)) {
          Authentication authentication = tokenProvider.getAuthentication(jwt);
          SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
          log.warn("[JwtFilter] JWT 토큰이 유효하지 않습니다: {}", requestURI);
        }
      }

      // 필터 체인 계속 진행
      filterChain.doFilter(request, response);

    } catch (TokenException e) {
      log.error("[JwtFilter] 필터 처리 중 예외 발생: {}", e.getMessage(), e);

      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");

      ApiExceptionDto errorResponse = new ApiExceptionDto(HttpStatus.UNAUTHORIZED,
          e.getMessage());

      response.setStatus(HttpStatus.UNAUTHORIZED.value());
      response.getWriter().write(convertObjectToJson(errorResponse));
      response.getWriter().flush();
    }
  }

  private String resolveToken(HttpServletRequest request) {
    String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
    ///Header에서 Bearer 부분 이하로 붙은 token을 파싱한다.
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
      return bearerToken.substring(7);
    }
    return null;
  }

  public String convertObjectToJson(Object object) throws JsonProcessingException {
    if (object == null) {
      return null;
    }
    ObjectMapper mapper = new ObjectMapper();
    return mapper.writeValueAsString(object);
  }

}
