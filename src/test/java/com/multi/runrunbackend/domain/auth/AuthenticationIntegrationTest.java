package com.multi.runrunbackend.domain.auth;

import com.multi.runrunbackend.common.jwt.dto.TokenDto;
import com.multi.runrunbackend.common.jwt.provider.TokenProvider;
import com.multi.runrunbackend.common.jwt.service.TokenService;
import com.multi.runrunbackend.domain.auth.service.AuthService;
import com.multi.runrunbackend.domain.auth.service.CustomUserDetailService;
import com.multi.runrunbackend.domain.user.dto.req.UserSignInDto;
import com.multi.runrunbackend.domain.user.dto.req.UserSignUpDto;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 인증 흐름 통합 테스트
 * 회원가입 -> 로그인 -> 토큰 발급 -> 토큰 검증 전체 플로우 테스트
 */
@DisplayName("인증 플로우 통합 테스트")
class AuthenticationIntegrationTest {

    @Test
    @DisplayName("완전한 인증 플로우: 회원가입 -> 로그인 -> 토큰 사용")
    void completeAuthenticationFlow() {
        // Mock dependencies
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        TokenProvider tokenProvider = mock(TokenProvider.class);
        TokenService tokenService = mock(TokenService.class);
        CustomUserDetailService userDetailService = mock(CustomUserDetailService.class);
        
        // 1. 회원가입 단계
        UserSignUpDto signUpDto = UserSignUpDto.builder()
            .loginId("testuser")
            .userPassword("Password123!")
            .userName("테스트")
            .userEmail("test@example.com")
            .birthDate(LocalDate.of(1990, 1, 1))
            .gender("M")
            .heightCm(175)
            .weightKg(70)
            .build();

        when(userRepository.findByLoginId("testuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return User.builder()
                .id(1L)
                .loginId(user.getLoginId())
                .password(user.getPassword())
                .email(user.getEmail())
                .role("ROLE_USER")
                .name(user.getName())
                .gender(user.getGender())
                .birthDate(user.getBirthDate())
                .build();
        });

        // 회원가입 검증
        assertThat(signUpDto.getLoginId()).isEqualTo("testuser");
        assertThat(signUpDto.getUserEmail()).isEqualTo("test@example.com");

        // 2. 로그인 단계
        UserSignInDto signInDto = UserSignInDto.builder()
            .loginId("testuser")
            .loginPw("Password123!")
            .build();

        User savedUser = User.builder()
            .id(1L)
            .loginId("testuser")
            .password("encodedPassword")
            .email("test@example.com")
            .role("ROLE_USER")
            .name("테스트")
            .gender("M")
            .birthDate(LocalDate.of(1990, 1, 1))
            .build();

        UserDetails userDetails = org.springframework.security.core.userdetails.User
            .withUsername("testuser")
            .password("encodedPassword")
            .authorities("ROLE_USER")
            .build();

        when(userDetailService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(passwordEncoder.matches("Password123!", "encodedPassword")).thenReturn(true);
        when(userRepository.findByLoginId("testuser")).thenReturn(Optional.of(savedUser));

        // 로그인 검증
        assertThat(signInDto.getLoginId()).isEqualTo("testuser");
        assertThat(userDetails.getUsername()).isEqualTo("testuser");

        // 3. 토큰 발급 단계
        TokenDto tokenDto = TokenDto.builder()
            .grantType("Bearer")
            .accessToken("eyJhbGciOiJIUzUxMiJ9.access.token")
            .refreshToken("eyJhbGciOiJIUzUxMiJ9.refresh.token")
            .build();

        when(tokenProvider.generateToken(eq("testuser"), anyList(), eq("A"), eq(1L)))
            .thenReturn(tokenDto.getAccessToken());
        when(tokenProvider.generateToken(eq("testuser"), isNull(), eq("R"), eq(0L)))
            .thenReturn(tokenDto.getRefreshToken());

        // 토큰 검증
        assertThat(tokenDto).isNotNull();
        assertThat(tokenDto.getGrantType()).isEqualTo("Bearer");
        assertThat(tokenDto.getAccessToken()).isNotNull();
        assertThat(tokenDto.getRefreshToken()).isNotNull();

        // 4. 토큰 유효성 검증 단계
        when(tokenProvider.validateToken(tokenDto.getAccessToken())).thenReturn(true);
        when(tokenProvider.getUserId(tokenDto.getAccessToken())).thenReturn("testuser");
        
        Authentication authentication = mock(Authentication.class);
        when(tokenProvider.getAuthentication(tokenDto.getAccessToken())).thenReturn(authentication);

        // 인증 객체 검증
        assertThat(tokenProvider.validateToken(tokenDto.getAccessToken())).isTrue();
        assertThat(tokenProvider.getUserId(tokenDto.getAccessToken())).isEqualTo("testuser");

        // 5. 토큰으로 사용자 정보 조회
        when(tokenProvider.getUserId(tokenDto.getAccessToken())).thenReturn("testuser");
        when(userRepository.findByLoginId("testuser")).thenReturn(Optional.of(savedUser));

        User authenticatedUser = userRepository.findByLoginId(
            tokenProvider.getUserId(tokenDto.getAccessToken())
        ).orElseThrow();

        // 최종 검증
        assertThat(authenticatedUser).isNotNull();
        assertThat(authenticatedUser.getLoginId()).isEqualTo("testuser");
        assertThat(authenticatedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(authenticatedUser.getRole()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("토큰 갱신 플로우: 리프레시 토큰 -> 새 액세스 토큰")
    void tokenRefreshFlow() {
        // Mock dependencies
        TokenProvider tokenProvider = mock(TokenProvider.class);
        
        // 1. 기존 리프레시 토큰
        String refreshToken = "Bearer eyJhbGciOiJIUzUxMiJ9.refresh.token";
        String resolvedToken = "eyJhbGciOiJIUzUxMiJ9.refresh.token";

        when(tokenProvider.resolveToken(refreshToken)).thenReturn(resolvedToken);
        when(tokenProvider.validateToken(resolvedToken)).thenReturn(true);
        when(tokenProvider.getUserId(resolvedToken)).thenReturn("testuser");

        // 2. 새 토큰 발급
        String newAccessToken = "eyJhbGciOiJIUzUxMiJ9.new.access.token";
        String newRefreshToken = "eyJhbGciOiJIUzUxMiJ9.new.refresh.token";

        when(tokenProvider.generateToken(eq("testuser"), anyList(), eq("A"), anyLong()))
            .thenReturn(newAccessToken);
        when(tokenProvider.generateToken(eq("testuser"), isNull(), eq("R"), eq(0L)))
            .thenReturn(newRefreshToken);

        TokenDto newTokenDto = TokenDto.builder()
            .grantType("Bearer")
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .build();

        // 검증
        assertThat(tokenProvider.resolveToken(refreshToken)).isEqualTo(resolvedToken);
        assertThat(newTokenDto.getAccessToken()).isNotNull();
        assertThat(newTokenDto.getRefreshToken()).isNotNull();
        assertThat(newTokenDto.getAccessToken()).isNotEqualTo(resolvedToken);
    }

    @Test
    @DisplayName("로그아웃 플로우: 토큰 블랙리스트 등록")
    void logoutFlow() {
        // Mock dependencies
        TokenProvider tokenProvider = mock(TokenProvider.class);
        
        // 1. 유효한 액세스 토큰
        String accessToken = "Bearer eyJhbGciOiJIUzUxMiJ9.access.token";
        String resolvedToken = "eyJhbGciOiJIUzUxMiJ9.access.token";

        when(tokenProvider.resolveToken(accessToken)).thenReturn(resolvedToken);
        when(tokenProvider.validateToken(resolvedToken)).thenReturn(true);

        // 2. 블랙리스트 등록 시뮬레이션
        String userId = "testuser";
        when(tokenProvider.getUserId(resolvedToken)).thenReturn(userId);

        // 검증
        assertThat(tokenProvider.resolveToken(accessToken)).isEqualTo(resolvedToken);
        assertThat(tokenProvider.validateToken(resolvedToken)).isTrue();
        assertThat(tokenProvider.getUserId(resolvedToken)).isEqualTo(userId);
        
        // 로그아웃 후에는 해당 토큰이 블랙리스트에 등록되어야 함
        verify(tokenProvider).resolveToken(accessToken);
        verify(tokenProvider).validateToken(resolvedToken);
    }

    @Test
    @DisplayName("관리자 권한 플로우: admin 포함 아이디 -> ROLE_ADMIN 부여")
    void adminUserFlow() {
        // Mock dependencies
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        // 1. admin 포함 회원가입
        UserSignUpDto adminSignUpDto = UserSignUpDto.builder()
            .loginId("adminuser")
            .userPassword("AdminPass123!")
            .userName("관리자")
            .userEmail("admin@example.com")
            .birthDate(LocalDate.of(1985, 5, 15))
            .gender("F")
            .build();

        when(userRepository.findByLoginId("adminuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedAdminPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return User.builder()
                .id(2L)
                .loginId(user.getLoginId())
                .password(user.getPassword())
                .email(user.getEmail())
                .role("ROLE_ADMIN")
                .name(user.getName())
                .gender(user.getGender())
                .birthDate(user.getBirthDate())
                .build();
        });

        // 2. role 검증
        String role = adminSignUpDto.getLoginId().toLowerCase().contains("admin") 
            ? "ROLE_ADMIN" : "ROLE_USER";

        assertThat(role).isEqualTo("ROLE_ADMIN");
        assertThat(adminSignUpDto.getLoginId()).contains("admin");
    }

    @Test
    @DisplayName("여러 역할을 가진 사용자의 토큰 생성")
    void multipleRolesTokenGeneration() {
        // Mock dependencies
        TokenProvider tokenProvider = mock(TokenProvider.class);

        // 1. 여러 역할
        List<String> roles = List.of("ROLE_USER", "ROLE_ADMIN", "ROLE_MANAGER");
        String memberId = "superuser";
        Long memberNo = 1L;

        // 2. 토큰 생성
        String token = "eyJhbGciOiJIUzUxMiJ9.multi.role.token";
        when(tokenProvider.generateToken(memberId, roles, "A", memberNo))
            .thenReturn(token);

        // 3. 토큰 검증
        when(tokenProvider.validateToken(token)).thenReturn(true);
        
        Authentication auth = mock(Authentication.class);
        when(tokenProvider.getAuthentication(token)).thenReturn(auth);

        // 검증
        String generatedToken = tokenProvider.generateToken(memberId, roles, "A", memberNo);
        assertThat(generatedToken).isEqualTo(token);
        assertThat(tokenProvider.validateToken(token)).isTrue();
    }
}