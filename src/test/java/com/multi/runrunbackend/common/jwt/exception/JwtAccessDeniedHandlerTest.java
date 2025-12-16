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
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAccessDeniedHandler 단위 테스트")
class JwtAccessDeniedHandlerTest {

    private JwtAccessDeniedHandler handler;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private AccessDeniedException accessDeniedException;

    @BeforeEach
    void setUp() {
        handler = new JwtAccessDeniedHandler();
    }

    @Nested
    @DisplayName("handle 메서드 테스트")
    class HandleTests {

        @Test
        @DisplayName("접근 거부 시 403 상태 코드 반환")
        void handle_AccessDenied_Returns403() throws IOException, ServletException {
            // given
            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            when(response.getWriter()).thenReturn(writer);
            when(request.getRequestURI()).thenReturn("/api/admin");

            // when
            handler.handle(request, response, accessDeniedException);

            // then
            verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
            verify(response).setContentType("application/json;charset=UTF-8");

            writer.flush();
            String jsonResponse = stringWriter.toString();
            assertThat(jsonResponse).contains("접근 권한이 없습니다");
        }

        @Test
        @DisplayName("JSON 형식으로 에러 응답 반환")
        void handle_ReturnsJsonFormat() throws IOException, ServletException {
            // given
            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            when(response.getWriter()).thenReturn(writer);
            when(request.getRequestURI()).thenReturn("/api/users");

            // when
            handler.handle(request, response, accessDeniedException);

            // then
            writer.flush();
            String jsonResponse = stringWriter.toString();
            assertThat(jsonResponse).startsWith("{");
            assertThat(jsonResponse).endsWith("}");
            assertThat(jsonResponse).contains("error");
        }

        @Test
        @DisplayName("다양한 경로에서 접근 거부 처리")
        void handle_DifferentPaths_SameResponse() throws IOException, ServletException {
            // given
            String[] paths = {"/api/admin", "/api/users/1/delete", "/api/config"};

            for (String path : paths) {
                StringWriter stringWriter = new StringWriter();
                PrintWriter writer = new PrintWriter(stringWriter);
                when(response.getWriter()).thenReturn(writer);
                when(request.getRequestURI()).thenReturn(path);

                // when
                handler.handle(request, response, accessDeniedException);

                // then
                verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
            }
        }
    }
}