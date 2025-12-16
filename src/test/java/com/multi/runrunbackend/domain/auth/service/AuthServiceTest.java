package com.multi.runrunbackend.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.multi.runrunbackend.common.exception.custom.DuplicateUsernameException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.jwt.dto.TokenDto;
import com.multi.runrunbackend.common.jwt.service.TokenService;
import com.multi.runrunbackend.domain.user.dto.req.UserSignInDto;
import com.multi.runrunbackend.domain.user.dto.req.UserSignUpDto;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private CustomUserDetailService customUserDetailService;

    @Mock
    private TokenService tokenService;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    private UserSignUpDto signUpDto;
    private UserSignInDto signInDto;
    private User testUser;

    @BeforeEach
    void setUp() {
        signUpDto = UserSignUpDto.builder()
            .loginId("testuser")
            .userPassword("password123")
            .userName("Test User")
            .userEmail("test@example.com")
            .birthDate(LocalDate.of(1990, 1, 1))
            .gender("M")
            .heightCm(175)
            .weightKg(70)
            .build();

        signInDto = UserSignInDto.builder()
            .loginId("testuser")
            .loginPw("password123")
            .build();

        testUser = User.builder()
            .id(1L)
            .loginId("testuser")
            .password("encodedPassword")
            .email("test@example.com")
            .name("Test User")
            .role("ROLE_USER")
            .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("signUp 메서드 테스트")
    class SignUpTests {

        @Test
        @DisplayName("일반 사용자 회원가입 성공")
        void signUp_RegularUser_Success() {
            // given
            when(userRepository.findByLoginId(signUpDto.getLoginId())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(signUpDto.getUserPassword())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(valueOperations.get(anyString())).thenReturn("ROLE_USER");

            // when
            authService.signUp(signUpDto);

            // then
            verify(userRepository).findByLoginId(signUpDto.getLoginId());
            verify(passwordEncoder).encode(signUpDto.getUserPassword());
            verify(userRepository).save(any(User.class));
            verify(valueOperations).set(eq("ROLE:" + signUpDto.getLoginId()), eq("ROLE_USER"),
                any(Duration.class));
        }

        @Test
        @DisplayName("관리자 회원가입 - loginId에 admin 포함")
        void signUp_AdminUser_Success() {
            // given
            UserSignUpDto adminDto = UserSignUpDto.builder()
                .loginId("admin123")
                .userPassword("adminpass")
                .userName("Admin")
                .userEmail("admin@example.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .gender("M")
                .build();

            User adminUser = User.builder()
                .id(2L)
                .loginId("admin123")
                .role("ROLE_ADMIN")
                .build();

            when(userRepository.findByLoginId(adminDto.getLoginId())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenReturn(adminUser);
            when(valueOperations.get(anyString())).thenReturn("ROLE_ADMIN");

            // when
            authService.signUp(adminDto);

            // then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getRole()).isEqualTo("ROLE_ADMIN");
        }

        @Test
        @DisplayName("중복된 loginId로 회원가입 시 예외 발생")
        void signUp_DuplicateLoginId_ThrowsException() {
            // given
            when(userRepository.findByLoginId(signUpDto.getLoginId())).thenReturn(
                Optional.of(testUser));

            // when & then
            assertThatThrownBy(() -> authService.signUp(signUpDto))
                .isInstanceOf(DuplicateUsernameException.class);

            verify(userRepository).findByLoginId(signUpDto.getLoginId());
            verify(userRepository, times(0)).save(any(User.class));
        }

        @Test
        @DisplayName("비밀번호 암호화 확인")
        void signUp_Password_IsEncoded() {
            // given
            when(userRepository.findByLoginId(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword123");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(valueOperations.get(anyString())).thenReturn("ROLE_USER");

            // when
            authService.signUp(signUpDto);

            // then
            verify(passwordEncoder).encode("password123");
        }

        @Test
        @DisplayName("Redis에 역할 저장 확인")
        void signUp_Role_SavedToRedis() {
            // given
            when(userRepository.findByLoginId(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(valueOperations.get(anyString())).thenReturn("ROLE_USER");

            // when
            authService.signUp(signUpDto);

            // then
            verify(valueOperations).set(
                eq("ROLE:" + signUpDto.getLoginId()),
                eq("ROLE_USER"),
                any(Duration.class)
            );
            verify(valueOperations).get("ROLE:" + signUpDto.getLoginId());
        }

        @Test
        @DisplayName("Redis 저장 실패 시 예외 발생")
        void signUp_RedisFailure_ThrowsException() {
            // given
            when(userRepository.findByLoginId(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(valueOperations.get(anyString())).thenReturn(null); // Redis 저장 실패 시뮬레이션

            // when & then
            assertThatThrownBy(() -> authService.signUp(signUpDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Redis에 저장 실패");
        }

        @Test
        @DisplayName("DB 저장 실패 시 예외 발생")
        void signUp_DatabaseFailure_ThrowsException() {
            // given
            when(userRepository.findByLoginId(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> authService.signUp(signUpDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("회원가입에 실패");
        }
    }

    @Nested
    @DisplayName("login 메서드 테스트")
    class LoginTests {

        private UserDetails userDetails;

        @BeforeEach
        void setUp() {
            userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("testuser")
                .password("encodedPassword")
                .authorities("ROLE_USER")
                .build();
        }

        @Test
        @DisplayName("로그인 성공")
        void login_ValidCredentials_ReturnsTokenDto() {
            // given
            TokenDto expectedToken = TokenDto.builder()
                .accessToken("access.token")
                .refreshToken("refresh.token")
                .build();

            when(customUserDetailService.loadUserByUsername(signInDto.getLoginId()))
                .thenReturn(userDetails);
            when(passwordEncoder.matches(signInDto.getLoginPw(), userDetails.getPassword()))
                .thenReturn(true);
            when(userRepository.findByLoginId(signInDto.getLoginId()))
                .thenReturn(Optional.of(testUser));
            when(tokenService.createToken(any(Map.class))).thenReturn(expectedToken);

            // when
            TokenDto result = authService.login(signInDto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("access.token");
            assertThat(result.getRefreshToken()).isEqualTo("refresh.token");

            verify(customUserDetailService).loadUserByUsername(signInDto.getLoginId());
            verify(passwordEncoder).matches(signInDto.getLoginPw(), userDetails.getPassword());
            verify(tokenService).createToken(any(Map.class));
        }

        @Test
        @DisplayName("잘못된 비밀번호로 로그인 시 예외 발생")
        void login_InvalidPassword_ThrowsException() {
            // given
            when(customUserDetailService.loadUserByUsername(signInDto.getLoginId()))
                .thenReturn(userDetails);
            when(passwordEncoder.matches(signInDto.getLoginPw(), userDetails.getPassword()))
                .thenReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.login(signInDto))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("비밀번호가 일치하지 않습니다");

            verify(passwordEncoder).matches(signInDto.getLoginPw(), userDetails.getPassword());
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 로그인 시 예외 발생")
        void login_UserNotFound_ThrowsException() {
            // given
            when(customUserDetailService.loadUserByUsername(signInDto.getLoginId()))
                .thenReturn(userDetails);
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(userRepository.findByLoginId(signInDto.getLoginId()))
                .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.login(signInDto))
                .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("로그인 시 마지막 로그인 시간 업데이트")
        void login_UpdatesLastLoginTime() {
            // given
            TokenDto expectedToken = TokenDto.builder().build();

            when(customUserDetailService.loadUserByUsername(anyString())).thenReturn(userDetails);
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(userRepository.findByLoginId(anyString())).thenReturn(Optional.of(testUser));
            when(tokenService.createToken(any(Map.class))).thenReturn(expectedToken);

            // when
            authService.login(signInDto);

            // then
            assertThat(testUser.getLastLoginAt()).isNotNull();
        }

        @Test
        @DisplayName("여러 역할을 가진 사용자 로그인")
        void login_UserWithMultipleRoles_Success() {
            // given
            UserDetails multiRoleUser = org.springframework.security.core.userdetails.User.builder()
                .username("admin")
                .password("encodedPassword")
                .authorities("ROLE_USER", "ROLE_ADMIN")
                .build();

            TokenDto expectedToken = TokenDto.builder().build();

            when(customUserDetailService.loadUserByUsername(anyString()))
                .thenReturn(multiRoleUser);
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(userRepository.findByLoginId(anyString())).thenReturn(Optional.of(testUser));
            when(tokenService.createToken(any(Map.class))).thenReturn(expectedToken);

            // when
            TokenDto result = authService.login(signInDto);

            // then
            assertThat(result).isNotNull();
            ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);
            verify(tokenService).createToken(mapCaptor.capture());

            Map<String, Object> capturedMap = mapCaptor.getValue();
            List<String> roles = (List<String>) capturedMap.get("roles");
            assertThat(roles).hasSize(2);
            assertThat(roles).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        }
    }

    @Nested
    @DisplayName("통합 시나리오 테스트")
    class IntegrationScenarioTests {

        @Test
        @DisplayName("회원가입 후 로그인 시나리오")
        void signUpThenLogin_Success() {
            // given - Sign Up
            when(userRepository.findByLoginId(signUpDto.getLoginId()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(testUser));
            when(passwordEncoder.encode(signUpDto.getUserPassword())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(valueOperations.get(anyString())).thenReturn("ROLE_USER");

            // when - Sign Up
            authService.signUp(signUpDto);

            // given - Login
            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("testuser")
                .password("encodedPassword")
                .authorities("ROLE_USER")
                .build();

            when(customUserDetailService.loadUserByUsername(signInDto.getLoginId()))
                .thenReturn(userDetails);
            when(passwordEncoder.matches(signInDto.getLoginPw(), "encodedPassword"))
                .thenReturn(true);
            when(tokenService.createToken(any(Map.class))).thenReturn(
                TokenDto.builder().accessToken("token").build());

            // when - Login
            TokenDto result = authService.login(signInDto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("token");
        }
    }
}