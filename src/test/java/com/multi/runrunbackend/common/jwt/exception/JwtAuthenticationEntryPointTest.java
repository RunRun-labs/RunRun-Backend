package com.multi.runrunbackend.common.jwt.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.security.core.AuthenticationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationEntryPoint 단위 테스트")
class JwtAuthenticationEntryPointTest {

    private JwtAuthenticationEntryPoint entryPoint;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private AuthenticationException authException;

    @BeforeEach
    void setUp() {
        entryPoint = new JwtAuthenticationEntryPoint();
    }

    @Nested
    @DisplayName("commence 메서드 테스트")
    class CommenceTests {

        @Test
        @DisplayName("인증 실패 시 401 상태 코드 반환")
        void commence_AuthenticationFailed_Returns401() throws IOException, ServletException {
            // given
            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            when(response.getWriter()).thenReturn(writer);
            when(request.getRequestURI()).thenReturn("/api/protected");

            // when
            entryPoint.commence(request, response, authException);

            // then
            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            verify(response).setContentType("application/json;charset=UTF-8");

            writer.flush();
            String jsonResponse = stringWriter.toString();
            assertThat(jsonResponse).contains("인증되지 않은 사용자입니다");
        }

        @Test
        @DisplayName("JSON 형식으로 에러 응답 반환")
        void commence_ReturnsJsonFormat() throws IOException, ServletException {
            // given
            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            when(response.getWriter()).thenReturn(writer);
            when(request.getRequestURI()).thenReturn("/api/users");

            // when
            entryPoint.commence(request, response, authException);

            // then
            writer.flush();
            String jsonResponse = stringWriter.toString();
            assertThat(jsonResponse).startsWith("{");
            assertThat(jsonResponse).endsWith("}");
            assertThat(jsonResponse).contains("error");
        }

        @Test
        @DisplayName("다양한 경로에서 인증 실패 처리")
        void commence_DifferentPaths_SameResponse() throws IOException, ServletException {
            // given
            String[] paths = {"/api/users", "/api/orders", "/api/products"};

            for (String path : paths) {
                StringWriter stringWriter = new StringWriter();
                PrintWriter writer = new PrintWriter(stringWriter);
                when(response.getWriter()).thenReturn(writer);
                when(request.getRequestURI()).thenReturn(path);

                // when
                entryPoint.commence(request, response, authException);

                // then
                verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
        }

        @Test
        @DisplayName("에러 메시지에 로그인 안내 포함")
        void commence_IncludesLoginGuidance() throws IOException, ServletException {
            // given
            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            when(response.getWriter()).thenReturn(writer);
            when(request.getRequestURI()).thenReturn("/api/data");

            // when
            entryPoint.commence(request, response, authException);

            // then
            writer.flush();
            String jsonResponse = stringWriter.toString();
            assertThat(jsonResponse).contains("로그인");
        }
    }
}