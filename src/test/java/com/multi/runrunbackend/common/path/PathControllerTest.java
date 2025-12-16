package com.multi.runrunbackend.common.path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PathController.class)
@DisplayName("PathController 단위 테스트")
class PathControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("로그인 페이지 라우팅 테스트")
    class LoginPageTests {

        @Test
        @DisplayName("GET /login - 로그인 페이지 반환")
        void login_ReturnsLoginView() throws Exception {
            mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
        }
    }

    @Nested
    @DisplayName("회원가입 페이지 라우팅 테스트")
    class SignupPageTests {

        @Test
        @DisplayName("GET /signup - 회원가입 페이지 반환")
        void signup_ReturnsSignupView() throws Exception {
            mockMvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/signup"));
        }
    }

    @Nested
    @DisplayName("다양한 경로 테스트")
    class VariousPathTests {

        @Test
        @DisplayName("로그인 페이지는 인증 없이 접근 가능")
        void login_AccessibleWithoutAuth() throws Exception {
            mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("회원가입 페이지는 인증 없이 접근 가능")
        void signup_AccessibleWithoutAuth() throws Exception {
            mockMvc.perform(get("/signup"))
                .andExpect(status().isOk());
        }
    }
}