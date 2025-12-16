package com.multi.runrunbackend.common.jwt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.multi.runrunbackend.common.exception.custom.RefreshTokenException;
import com.multi.runrunbackend.common.exception.custom.TokenException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.jwt.dto.TokenDto;
import com.multi.runrunbackend.common.jwt.provider.TokenProvider;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService 단위 테스트")
class TokenServiceTest {

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenService tokenService;

    private User testUser;
    private String testAccessToken;
    private String testRefreshToken;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .email("test@example.com")
            .password("encodedPassword")
            .username("testUser")
            .build();

        testAccessToken = "test.access.token";
        testRefreshToken = "test.refresh.token";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("generateToken 메서드 테스트")
    class GenerateTokenTests {

        @Test
        @DisplayName("사용자로부터 토큰 생성 성공")
        void generateToken_ValidUser_ReturnsTokenDto() {
            // given
            when(tokenProvider.generateToken(anyString(), any(), eq("A"), anyLong()))
                .thenReturn(testAccessToken);
            when(tokenProvider.generateToken(anyString(), any(), eq("R"), anyLong()))
                .thenReturn(testRefreshToken);

            // when
            TokenDto result = tokenService.generateToken(testUser);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getGrantType()).isEqualTo("Bearer");
            assertThat(result.getAccessToken()).isEqualTo(testAccessToken);
            assertThat(result.getRefreshToken()).isEqualTo(testRefreshToken);

            verify(tokenProvider).generateToken(eq(testUser.getEmail()), any(), eq("A"),
                eq(testUser.getId()));
            verify(tokenProvider).generateToken(eq(testUser.getEmail()), any(), eq("R"),
                eq(testUser.getId()));
        }

        @Test
        @DisplayName("null 사용자로 토큰 생성 시 예외 발생")
        void generateToken_NullUser_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> tokenService.generateToken(null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("reissueToken 메서드 테스트")
    class ReissueTokenTests {

        @Test
        @DisplayName("유효한 리프레시 토큰으로 재발급 성공")
        void reissueToken_ValidRefreshToken_ReturnsNewTokenDto() {
            // given
            String memberId = "test@example.com";
            String newAccessToken = "new.access.token";
            String newRefreshToken = "new.refresh.token";

            when(tokenProvider.validateToken(testRefreshToken)).thenReturn(true);
            when(tokenProvider.getUserId(testRefreshToken)).thenReturn(memberId);
            when(valueOperations.get(memberId)).thenReturn(testRefreshToken);
            when(userRepository.findByEmail(memberId)).thenReturn(Optional.of(testUser));
            when(tokenProvider.generateToken(anyString(), any(), eq("A"), anyLong()))
                .thenReturn(newAccessToken);
            when(tokenProvider.generateToken(anyString(), any(), eq("R"), anyLong()))
                .thenReturn(newRefreshToken);

            // when
            TokenDto result = tokenService.reissueToken(testRefreshToken);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo(newAccessToken);
            assertThat(result.getRefreshToken()).isEqualTo(newRefreshToken);

            verify(redisTemplate).delete(memberId);
        }

        @Test
        @DisplayName("만료된 리프레시 토큰으로 재발급 시 예외 발생")
        void reissueToken_ExpiredToken_ThrowsException() {
            // given
            when(tokenProvider.validateToken(testRefreshToken))
                .thenThrow(new TokenException(ErrorCode.EXPIRED_TOKEN));

            // when & then
            assertThatThrownBy(() -> tokenService.reissueToken(testRefreshToken))
                .isInstanceOf(TokenException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXPIRED_TOKEN);
        }

        @Test
        @DisplayName("Redis에 저장된 토큰과 다른 경우 예외 발생")
        void reissueToken_TokenMismatch_ThrowsException() {
            // given
            String memberId = "test@example.com";
            String storedToken = "different.refresh.token";

            when(tokenProvider.validateToken(testRefreshToken)).thenReturn(true);
            when(tokenProvider.getUserId(testRefreshToken)).thenReturn(memberId);
            when(valueOperations.get(memberId)).thenReturn(storedToken);

            // when & then
            assertThatThrownBy(() -> tokenService.reissueToken(testRefreshToken))
                .isInstanceOf(RefreshTokenException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("Redis에 리프레시 토큰이 없는 경우 예외 발생")
        void reissueToken_NoTokenInRedis_ThrowsException() {
            // given
            String memberId = "test@example.com";

            when(tokenProvider.validateToken(testRefreshToken)).thenReturn(true);
            when(tokenProvider.getUserId(testRefreshToken)).thenReturn(memberId);
            when(valueOperations.get(memberId)).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> tokenService.reissueToken(testRefreshToken))
                .isInstanceOf(RefreshTokenException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        @Test
        @DisplayName("사용자를 찾을 수 없는 경우 예외 발생")
        void reissueToken_UserNotFound_ThrowsException() {
            // given
            String memberId = "test@example.com";

            when(tokenProvider.validateToken(testRefreshToken)).thenReturn(true);
            when(tokenProvider.getUserId(testRefreshToken)).thenReturn(memberId);
            when(valueOperations.get(memberId)).thenReturn(testRefreshToken);
            when(userRepository.findByEmail(memberId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> tokenService.reissueToken(testRefreshToken))
                .isInstanceOf(RefreshTokenException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    @Nested
    @DisplayName("logout 메서드 테스트")
    class LogoutTests {

        @Test
        @DisplayName("로그아웃 성공 - 액세스 토큰 블랙리스트 추가")
        void logout_ValidToken_AddsToBlacklist() {
            // given
            HttpServletRequest request = mock(HttpServletRequest.class);
            String memberId = "test@example.com";
            long expirationTime = System.currentTimeMillis() + 1000 * 60 * 30;

            when(request.getHeader("Authorization")).thenReturn("Bearer " + testAccessToken);
            when(tokenProvider.extractMemberId(request)).thenReturn(memberId);
            when(tokenProvider.resolveToken("Bearer " + testAccessToken))
                .thenReturn(testAccessToken);

            Claims mockClaims = Jwts.claims().setSubject(memberId);
            mockClaims.setExpiration(new Date(expirationTime));
            when(tokenProvider.parseClames(testAccessToken)).thenReturn(mockClaims);

            // when
            tokenService.logout(request);

            // then
            verify(valueOperations).set(eq("blacklist:" + testAccessToken), eq("logout"),
                any(Duration.class));
            verify(redisTemplate).delete(memberId);
        }

        @Test
        @DisplayName("Authorization 헤더가 없는 경우 예외 발생")
        void logout_NoAuthorizationHeader_ThrowsException() {
            // given
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Authorization")).thenReturn(null);
            when(tokenProvider.extractMemberId(request))
                .thenThrow(new TokenException(ErrorCode.MISSING_AUTHORIZATION_HEADER));

            // when & then
            assertThatThrownBy(() -> tokenService.logout(request))
                .isInstanceOf(TokenException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                    ErrorCode.MISSING_AUTHORIZATION_HEADER);
        }
    }

    @Nested
    @DisplayName("isTokenBlacklisted 메서드 테스트")
    class IsTokenBlacklistedTests {

        @Test
        @DisplayName("블랙리스트에 등록된 토큰 확인")
        void isTokenBlacklisted_BlacklistedToken_ReturnsTrue() {
            // given
            String blacklistedToken = "blacklisted.token";
            when(redisTemplate.hasKey("blacklist:" + blacklistedToken)).thenReturn(true);

            // when
            boolean result = tokenService.isTokenBlacklisted(blacklistedToken);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("블랙리스트에 없는 토큰 확인")
        void isTokenBlacklisted_ValidToken_ReturnsFalse() {
            // given
            String validToken = "valid.token";
            when(redisTemplate.hasKey("blacklist:" + validToken)).thenReturn(false);

            // when
            boolean result = tokenService.isTokenBlacklisted(validToken);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("null 토큰 확인")
        void isTokenBlacklisted_NullToken_ReturnsFalse() {
            // given
            when(redisTemplate.hasKey("blacklist:null")).thenReturn(false);

            // when
            boolean result = tokenService.isTokenBlacklisted(null);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("extractTokenFromRequest 메서드 테스트")
    class ExtractTokenFromRequestTests {

        @Test
        @DisplayName("요청에서 토큰 추출 성공")
        void extractTokenFromRequest_ValidRequest_ReturnsToken() {
            // given
            HttpServletRequest request = mock(HttpServletRequest.class);
            String bearerToken = "Bearer " + testAccessToken;

            when(request.getHeader("Authorization")).thenReturn(bearerToken);
            when(tokenProvider.resolveToken(bearerToken)).thenReturn(testAccessToken);

            // when
            String result = tokenService.extractTokenFromRequest(request);

            // then
            assertThat(result).isEqualTo(testAccessToken);
        }

        @Test
        @DisplayName("Authorization 헤더가 없는 경우 null 반환")
        void extractTokenFromRequest_NoHeader_ReturnsNull() {
            // given
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Authorization")).thenReturn(null);
            when(tokenProvider.resolveToken(null)).thenReturn(null);

            // when
            String result = tokenService.extractTokenFromRequest(request);

            // then
            assertThat(result).isNull();
        }
    }
}