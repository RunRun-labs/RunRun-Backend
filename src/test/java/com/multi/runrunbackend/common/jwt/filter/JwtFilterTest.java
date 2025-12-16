package com.multi.runrunbackend.common.jwt.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.multi.runrunbackend.common.exception.custom.TokenException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.jwt.provider.TokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtFilter 단위 테스트")
class JwtFilterTest {

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() {
        jwtFilter = new JwtFilter(tokenProvider, redisTemplate);
        SecurityContextHolder.clearContext();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("필터 제외 경로 테스트")
    class ExcludedPathTests {

        @Test
        @DisplayName("/auth/** 경로는 필터를 건너뜀")
        void authPaths_SkipFilter() throws ServletException, IOException {
            // given
            when(request.getRequestURI()).thenReturn("/auth/login");

            // when
            jwtFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
            verify(tokenProvider, never()).validateToken(anyString());
        }

        @Test
        @DisplayName("/public/** 경로는 필터를 건너뜀")
        void publicPaths_SkipFilter() throws ServletException, IOException {
            // given
            when(request.getRequestURI()).thenReturn("/public/health");

            // when
            jwtFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
            verify(tokenProvider, never()).validateToken(anyString());
        }

        @Test
        @DisplayName("/swagger-ui/** 경로는 필터를 건너뜀")
        void swaggerPaths_SkipFilter() throws ServletException, IOException {
            // given
            when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

            // when
            jwtFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("정적 리소스 경로는 필터를 건너뜀")
        void staticResourcePaths_SkipFilter() throws ServletException, IOException {
            // given
            String[] staticPaths = {"/css/style.css", "/js/app.js", "/favicon.ico"};

            for (String path : staticPaths) {
                when(request.getRequestURI()).thenReturn(path);

                // when
                jwtFilter.doFilterInternal(request, response, filterChain);

                // then
                verify(filterChain).doFilter(request, response);
            }
        }
    }

    @Nested
    @DisplayName("JWT 토큰 검증 테스트")
    class TokenValidationTests {

        @Test
        @DisplayName("유효한 JWT 토큰으로 인증 성공")
        void validToken_AuthenticationSuccess() throws ServletException, IOException {
            // given
            String validToken = "valid.jwt.token";
            when(request.getRequestURI()).thenReturn("/api/users");
            when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
            when(tokenProvider.validateToken(validToken)).thenReturn(true);
            when(redisTemplate.hasKey("blacklist:" + validToken)).thenReturn(false);

            Authentication mockAuth = mock(Authentication.class);
            when(tokenProvider.getAuthentication(validToken)).thenReturn(mockAuth);

            // when
            jwtFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(tokenProvider).validateToken(validToken);
            verify(tokenProvider).getAuthentication(validToken);
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(mockAuth);
        }

        @Test
        @DisplayName("Authorization 헤더가 없는 경우 인증 없이 진행")
        void noAuthorizationHeader_ProceedsWithoutAuth() throws ServletException, IOException {
            // given
            when(request.getRequestURI()).thenReturn("/api/users");
            when(request.getHeader("Authorization")).thenReturn(null);

            // when
            jwtFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(tokenProvider, never()).validateToken(anyString());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Bearer 접두어가 없는 토큰은 무시")
        void tokenWithoutBearerPrefix_Ignored() throws ServletException, IOException {
            // given
            when(request.getRequestURI()).thenReturn("/api/users");
            when(request.getHeader("Authorization")).thenReturn("InvalidToken");

            // when
            jwtFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(tokenProvider, never()).validateToken(anyString());
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("블랙리스트 토큰 테스트")
    class BlacklistTokenTests {

        @Test
        @DisplayName("블랙리스트에 등록된 토큰은 거부")
        void blacklistedToken_Rejected() throws ServletException, IOException {
            // given
            String blacklistedToken = "blacklisted.jwt.token";
            when(request.getRequestURI()).thenReturn("/api/users");
            when(request.getHeader("Authorization")).thenReturn("Bearer " + blacklistedToken);
            when(redisTemplate.hasKey("blacklist:" + blacklistedToken)).thenReturn(true);

            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            when(response.getWriter()).thenReturn(writer);

            // when
            jwtFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            verify(filterChain, never()).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("토큰 예외 처리 테스트")
    class TokenExceptionTests {

        @Test
        @DisplayName("만료된 토큰 예외 처리")
        void expiredToken_ExceptionHandled() throws ServletException, IOException {
            // given
            String expiredToken = "expired.jwt.token";
            when(request.getRequestURI()).thenReturn("/api/users");
            when(request.getHeader("Authorization")).thenReturn("Bearer " + expiredToken);
            when(redisTemplate.hasKey(anyString())).thenReturn(false);
            when(tokenProvider.validateToken(expiredToken))
                .thenThrow(new TokenException(ErrorCode.EXPIRED_TOKEN));

            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            when(response.getWriter()).thenReturn(writer);

            // when
            jwtFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("유효하지 않은 토큰 예외 처리")
        void invalidToken_ExceptionHandled() throws ServletException, IOException {
            // given
            String invalidToken = "invalid.jwt.token";
            when(request.getRequestURI()).thenReturn("/api/users");
            when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
            when(redisTemplate.hasKey(anyString())).thenReturn(false);
            when(tokenProvider.validateToken(invalidToken))
                .thenThrow(new TokenException(ErrorCode.INVALID_TOKEN));

            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            when(response.getWriter()).thenReturn(writer);

            // when
            jwtFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("다양한 시나리오 테스트")
    class VariousScenarioTests {

        @Test
        @DisplayName("여러 제외 경로 패턴 테스트")
        void multipleExcludedPaths_AllSkipFilter() throws ServletException, IOException {
            // given
            String[] excludedPaths = {
                "/auth/login",
                "/auth/signup",
                "/public/api",
                "/swagger-ui/",
                "/static/image.png",
                "/css/style.css",
                "/js/app.js"
            };

            for (String path : excludedPaths) {
                SecurityContextHolder.clearContext();
                when(request.getRequestURI()).thenReturn(path);

                // when
                jwtFilter.doFilterInternal(request, response, filterChain);

                // then
                verify(filterChain).doFilter(request, response);
            }
        }
    }
}