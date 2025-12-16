package com.multi.runrunbackend.domain.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.multi.runrunbackend.common.exception.custom.DuplicateUsernameException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.jwt.dto.TokenDto;
import com.multi.runrunbackend.common.jwt.service.TokenService;
import com.multi.runrunbackend.domain.auth.service.AuthService;
import com.multi.runrunbackend.domain.user.dto.req.UserSignInDto;
import com.multi.runrunbackend.domain.user.dto.req.UserSignUpDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController 단위 테스트")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private TokenService tokenService;

    @Nested
    @DisplayName("회원가입 API 테스트")
    class SignupTests {

        @Test
        @DisplayName("회원가입 성공 - 201 Created 반환")
        void signup_Success() throws Exception {
            // given
            UserSignUpDto signUpDto = createValidSignUpDto();
            doNothing().when(authService).signUp(any(UserSignUpDto.class));

            // when & then
            mockMvc.perform(post("/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signUpDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원가입 성공"));

            verify(authService).signUp(any(UserSignUpDto.class));
        }

        @Test
        @DisplayName("회원가입 실패 - 중복된 아이디")
        void signup_Fail_DuplicateUsername() throws Exception {
            // given
            UserSignUpDto signUpDto = createValidSignUpDto();
            doThrow(new DuplicateUsernameException(ErrorCode.DUPLICATE_USER))
                .when(authService).signUp(any(UserSignUpDto.class));

            // when & then
            mockMvc.perform(post("/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signUpDto)))
                .andDo(print())
                .andExpect(status().isConflict());

            verify(authService).signUp(any(UserSignUpDto.class));
        }

        @Test
        @DisplayName("회원가입 실패 - 유효성 검증 실패 (아이디 길이)")
        void signup_Fail_ValidationError_LoginIdLength() throws Exception {
            // given
            UserSignUpDto signUpDto = createValidSignUpDto();
            signUpDto.setLoginId("test"); // 5자 미만

            // when & then
            mockMvc.perform(post("/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signUpDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());

            verify(authService, never()).signUp(any(UserSignUpDto.class));
        }

        @Test
        @DisplayName("회원가입 실패 - 유효성 검증 실패 (이메일 형식)")
        void signup_Fail_ValidationError_EmailFormat() throws Exception {
            // given
            UserSignUpDto signUpDto = createValidSignUpDto();
            signUpDto.setUserEmail("invalid-email"); // 잘못된 이메일 형식

            // when & then
            mockMvc.perform(post("/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signUpDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());

            verify(authService, never()).signUp(any(UserSignUpDto.class));
        }

        @Test
        @DisplayName("회원가입 실패 - 유효성 검증 실패 (비밀번호 길이)")
        void signup_Fail_ValidationError_PasswordLength() throws Exception {
            // given
            UserSignUpDto signUpDto = createValidSignUpDto();
            signUpDto.setUserPassword("short"); // 8자 미만

            // when & then
            mockMvc.perform(post("/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signUpDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());

            verify(authService, never()).signUp(any(UserSignUpDto.class));
        }
    }

    @Nested
    @DisplayName("로그인 API 테스트")
    class LoginTests {

        @Test
        @DisplayName("로그인 성공 - 200 OK와 토큰 반환")
        void login_Success() throws Exception {
            // given
            UserSignInDto signInDto = UserSignInDto.builder()
                .loginId("testuser")
                .loginPw("password123")
                .build();

            TokenDto tokenDto = TokenDto.builder()
                .grantType("Bearer")
                .accessToken("accessToken")
                .refreshToken("refreshToken")
                .build();

            when(authService.login(any(UserSignInDto.class))).thenReturn(tokenDto);

            // when & then
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signInDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그인 성공"))
                .andExpect(jsonPath("$.data.accessToken").value("accessToken"))
                .andExpect(jsonPath("$.data.refreshToken").value("refreshToken"))
                .andExpect(jsonPath("$.data.grantType").value("Bearer"));

            verify(authService).login(any(UserSignInDto.class));
        }

        @Test
        @DisplayName("로그인 실패 - 비밀번호 불일치")
        void login_Fail_InvalidPassword() throws Exception {
            // given
            UserSignInDto signInDto = UserSignInDto.builder()
                .loginId("testuser")
                .loginPw("wrongpassword")
                .build();

            when(authService.login(any(UserSignInDto.class)))
                .thenThrow(new BadCredentialsException("비밀번호가 일치하지 않습니다"));

            // when & then
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signInDto)))
                .andDo(print())
                .andExpect(status().is5xxServerError()); // BadCredentialsException은 500으로 처리됨

            verify(authService).login(any(UserSignInDto.class));
        }

        @Test
        @DisplayName("로그인 실패 - 사용자를 찾을 수 없음")
        void login_Fail_UserNotFound() throws Exception {
            // given
            UserSignInDto signInDto = UserSignInDto.builder()
                .loginId("nonexistent")
                .loginPw("password123")
                .build();

            when(authService.login(any(UserSignInDto.class)))
                .thenThrow(new NotFoundException(ErrorCode.USER_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signInDto)))
                .andDo(print())
                .andExpect(status().isNotFound());

            verify(authService).login(any(UserSignInDto.class));
        }
    }

    @Nested
    @DisplayName("토큰 리프레시 API 테스트")
    class RefreshTokenTests {

        @Test
        @DisplayName("토큰 리프레시 성공")
        void refresh_Success() throws Exception {
            // given
            String refreshToken = "Bearer validRefreshToken";
            TokenDto newTokenDto = TokenDto.builder()
                .grantType("Bearer")
                .accessToken("newAccessToken")
                .refreshToken("newRefreshToken")
                .build();

            when(tokenService.createToken(anyString())).thenReturn(newTokenDto);

            // when & then
            mockMvc.perform(post("/auth/refresh")
                    .header("Authorization", refreshToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("newAccessToken"))
                .andExpect(jsonPath("$.data.refreshToken").value("newRefreshToken"));

            verify(tokenService).createToken(refreshToken);
        }

        @Test
        @DisplayName("토큰 리프레시 실패 - Authorization 헤더 없음")
        void refresh_Fail_NoAuthorizationHeader() throws Exception {
            // when & then
            mockMvc.perform(post("/auth/refresh"))
                .andDo(print())
                .andExpect(status().isBadRequest());

            verify(tokenService, never()).createToken(anyString());
        }
    }

    @Nested
    @DisplayName("로그아웃 API 테스트")
    class LogoutTests {

        @Test
        @DisplayName("로그아웃 성공")
        void logout_Success() throws Exception {
            // given
            String accessToken = "Bearer validAccessToken";
            doNothing().when(tokenService).registBlackList(accessToken);

            // when & then
            mockMvc.perform(get("/auth/logout")
                    .header("Authorization", accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그아웃 성공"));

            verify(tokenService).registBlackList(accessToken);
        }

        @Test
        @DisplayName("로그아웃 실패 - Authorization 헤더 없음")
        void logout_Fail_NoAuthorizationHeader() throws Exception {
            // when & then
            mockMvc.perform(get("/auth/logout"))
                .andDo(print())
                .andExpect(status().isBadRequest());

            verify(tokenService, never()).registBlackList(anyString());
        }
    }

    // Helper methods
    private UserSignUpDto createValidSignUpDto() {
        return UserSignUpDto.builder()
            .loginId("testuser")
            .userPassword("Password123!")
            .userName("Test")
            .userEmail("test@example.com")
            .birthDate(LocalDate.of(1990, 1, 1))
            .gender("M")
            .heightCm(175)
            .weightKg(70)
            .build();
    }
}