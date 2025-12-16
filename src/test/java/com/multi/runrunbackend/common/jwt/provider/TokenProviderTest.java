package com.multi.runrunbackend.common.jwt.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.multi.runrunbackend.common.exception.custom.TokenException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenProvider 단위 테스트")
class TokenProviderTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private TokenProvider tokenProvider;
    private SecretKey testSecretKey;
    private String testIssuer;

    @BeforeEach
    void setUp() {
        // Create a test secret key (256 bits minimum for HS512)
        String secret = Base64.getEncoder().encodeToString(
            "test-secret-key-for-jwt-token-generation-must-be-long-enough-for-hs512".getBytes()
        );
        testSecretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        testIssuer = "test-issuer";

        when(jwtProvider.getSecretKey()).thenReturn(testSecretKey);
        when(jwtProvider.getIssuer()).thenReturn(testIssuer);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        tokenProvider = new TokenProvider(jwtProvider, redisTemplate, userRepository);
    }

    @Nested
    @DisplayName("generateToken 메서드 테스트")
    class GenerateTokenTests {

        @Test
        @DisplayName("액세스 토큰 생성 성공")
        void generateAccessToken_Success() {
            // given
            String memberId = "test@example.com";
            List<String> roles = Arrays.asList("ROLE_USER");
            String code = "A";
            Long memberNo = 1L;

            // when
            String token = tokenProvider.generateToken(memberId, roles, code, memberNo);

            // then
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();

            Claims claims = Jwts.parserBuilder()
                .setSigningKey(testSecretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

            assertThat(claims.getSubject()).isEqualTo(memberId);
            assertThat(claims.get("memberNo", Long.class)).isEqualTo(memberNo);
            assertThat(claims.get("auth", String.class)).isEqualTo("ROLE_USER");
            assertThat(claims.getIssuer()).isEqualTo(testIssuer);
        }

        @Test
        @DisplayName("리프레시 토큰 생성 성공 및 Redis 저장")
        void generateRefreshToken_Success_AndSavedToRedis() {
            // given
            String memberId = "test@example.com";
            List<String> roles = Arrays.asList("ROLE_USER");
            String code = "R";
            Long memberNo = 1L;

            // when
            String token = tokenProvider.generateToken(memberId, roles, code, memberNo);

            // then
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();

            // Redis에 저장되었는지 검증
            verify(valueOperations).set(eq(memberId), eq(token), any(Duration.class));

            Claims claims = Jwts.parserBuilder()
                .setSigningKey(testSecretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

            assertThat(claims.getSubject()).isEqualTo(memberId);
            assertThat(claims.get("memberNo", Long.class)).isEqualTo(memberNo);
        }

        @Test
        @DisplayName("여러 역할을 가진 토큰 생성")
        void generateToken_WithMultipleRoles() {
            // given
            String memberId = "admin@example.com";
            List<String> roles = Arrays.asList("ROLE_USER", "ROLE_ADMIN");
            String code = "A";
            Long memberNo = 2L;

            // when
            String token = tokenProvider.generateToken(memberId, roles, code, memberNo);

            // then
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(testSecretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

            assertThat(claims.get("auth", String.class)).isEqualTo("ROLE_USER,ROLE_ADMIN");
        }
    }

    @Nested
    @DisplayName("validateToken 메서드 테스트")
    class ValidateTokenTests {

        @Test
        @DisplayName("유효한 토큰 검증 성공")
        void validateToken_ValidToken_ReturnsTrue() {
            // given
            String token = tokenProvider.generateToken("test@example.com",
                Arrays.asList("ROLE_USER"), "A", 1L);

            // when
            boolean result = tokenProvider.validateToken(token);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("잘못된 서명의 토큰 검증 실패")
        void validateToken_InvalidSignature_ThrowsException() {
            // given
            SecretKey wrongKey = Keys.hmacShaKeyFor(
                "wrong-secret-key-for-jwt-token-that-is-long-enough-for-hs512-algorithm".getBytes()
            );
            String invalidToken = Jwts.builder()
                .setSubject("test@example.com")
                .signWith(wrongKey)
                .compact();

            // when & then
            assertThatThrownBy(() -> tokenProvider.validateToken(invalidToken))
                .isInstanceOf(TokenException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);
        }

        @Test
        @DisplayName("만료된 토큰 검증 실패")
        void validateToken_ExpiredToken_ThrowsException() {
            // given
            String expiredToken = Jwts.builder()
                .setSubject("test@example.com")
                .setIssuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60))
                .setExpiration(new Date(System.currentTimeMillis() - 1000 * 60 * 30))
                .signWith(testSecretKey)
                .compact();

            // when & then
            assertThatThrownBy(() -> tokenProvider.validateToken(expiredToken))
                .isInstanceOf(TokenException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXPIRED_TOKEN);
        }

        @Test
        @DisplayName("형식이 잘못된 토큰 검증 실패")
        void validateToken_MalformedToken_ThrowsException() {
            // given
            String malformedToken = "this.is.not.a.valid.token";

            // when & then
            assertThatThrownBy(() -> tokenProvider.validateToken(malformedToken))
                .isInstanceOf(TokenException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);
        }

        @Test
        @DisplayName("null 토큰 검증 실패")
        void validateToken_NullToken_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> tokenProvider.validateToken(null))
                .isInstanceOf(TokenException.class);
        }

        @Test
        @DisplayName("빈 문자열 토큰 검증 실패")
        void validateToken_EmptyToken_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> tokenProvider.validateToken(""))
                .isInstanceOf(TokenException.class);
        }
    }

    @Nested
    @DisplayName("getAuthentication 메서드 테스트")
    class GetAuthenticationTests {

        @Test
        @DisplayName("토큰에서 인증 객체 추출 성공")
        void getAuthentication_ValidToken_ReturnsAuthentication() {
            // given
            String memberId = "test@example.com";
            List<String> roles = Arrays.asList("ROLE_USER");
            String token = tokenProvider.generateToken(memberId, roles, "A", 1L);

            // when
            Authentication authentication = tokenProvider.getAuthentication(token);

            // then
            assertThat(authentication).isNotNull();
            assertThat(authentication.getPrincipal()).isNotNull();
            assertThat(authentication.getAuthorities()).hasSize(1);
            assertThat(authentication.getAuthorities()).extracting("authority")
                .containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("여러 권한을 가진 토큰에서 인증 객체 추출")
        void getAuthentication_MultipleRoles_ReturnsAuthenticationWithAllRoles() {
            // given
            String memberId = "admin@example.com";
            List<String> roles = Arrays.asList("ROLE_USER", "ROLE_ADMIN");
            String token = tokenProvider.generateToken(memberId, roles, "A", 2L);

            // when
            Authentication authentication = tokenProvider.getAuthentication(token);

            // then
            assertThat(authentication.getAuthorities()).hasSize(2);
            assertThat(authentication.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        }

        @Test
        @DisplayName("권한 정보가 없는 토큰 처리 시 예외 발생")
        void getAuthentication_TokenWithoutAuthorities_ThrowsException() {
            // given
            String tokenWithoutAuth = Jwts.builder()
                .setSubject("test@example.com")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 30))
                .signWith(testSecretKey)
                .compact();

            // when & then
            assertThatThrownBy(() -> tokenProvider.getAuthentication(tokenWithoutAuth))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("권한 정보가 없는 토큰");
        }
    }

    @Nested
    @DisplayName("getUserId 메서드 테스트")
    class GetUserIdTests {

        @Test
        @DisplayName("토큰에서 사용자 ID 추출 성공")
        void getUserId_ValidToken_ReturnsUserId() {
            // given
            String expectedUserId = "test@example.com";
            String token = tokenProvider.generateToken(expectedUserId,
                Arrays.asList("ROLE_USER"), "A", 1L);

            // when
            String actualUserId = tokenProvider.getUserId(token);

            // then
            assertThat(actualUserId).isEqualTo(expectedUserId);
        }

        @Test
        @DisplayName("여러 토큰에서 각각 다른 사용자 ID 추출")
        void getUserId_DifferentTokens_ReturnsDifferentUserIds() {
            // given
            String userId1 = "user1@example.com";
            String userId2 = "user2@example.com";
            String token1 = tokenProvider.generateToken(userId1, Arrays.asList("ROLE_USER"), "A",
                1L);
            String token2 = tokenProvider.generateToken(userId2, Arrays.asList("ROLE_USER"), "A",
                2L);

            // when
            String extractedId1 = tokenProvider.getUserId(token1);
            String extractedId2 = tokenProvider.getUserId(token2);

            // then
            assertThat(extractedId1).isEqualTo(userId1);
            assertThat(extractedId2).isEqualTo(userId2);
        }
    }

    @Nested
    @DisplayName("getMemberNo 메서드 테스트")
    class GetMemberNoTests {

        @Test
        @DisplayName("토큰에서 회원 번호 추출 성공")
        void getMemberNo_ValidToken_ReturnsMemberNo() {
            // given
            Long expectedMemberNo = 12345L;
            String token = tokenProvider.generateToken("test@example.com",
                Arrays.asList("ROLE_USER"), "A", expectedMemberNo);

            // when
            Long actualMemberNo = tokenProvider.getMemberNo(token);

            // then
            assertThat(actualMemberNo).isEqualTo(expectedMemberNo);
        }

        @Test
        @DisplayName("다양한 회원 번호 추출")
        void getMemberNo_DifferentNumbers_ReturnsCorrectNumbers() {
            // given
            Long memberNo1 = 1L;
            Long memberNo2 = 999999L;
            String token1 = tokenProvider.generateToken("user1@example.com",
                Arrays.asList("ROLE_USER"), "A", memberNo1);
            String token2 = tokenProvider.generateToken("user2@example.com",
                Arrays.asList("ROLE_USER"), "A", memberNo2);

            // when & then
            assertThat(tokenProvider.getMemberNo(token1)).isEqualTo(memberNo1);
            assertThat(tokenProvider.getMemberNo(token2)).isEqualTo(memberNo2);
        }
    }

    @Nested
    @DisplayName("resolveToken 메서드 테스트")
    class ResolveTokenTests {

        @Test
        @DisplayName("Bearer 접두어가 있는 토큰 파싱")
        void resolveToken_WithBearerPrefix_ReturnsTokenOnly() {
            // given
            String actualToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
            String bearerToken = "Bearer " + actualToken;

            // when
            String resolved = tokenProvider.resolveToken(bearerToken);

            // then
            assertThat(resolved).isEqualTo(actualToken);
        }

        @Test
        @DisplayName("Bearer 접두어가 없는 토큰은 그대로 반환")
        void resolveToken_WithoutBearerPrefix_ReturnsSameToken() {
            // given
            String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";

            // when
            String resolved = tokenProvider.resolveToken(token);

            // then
            assertThat(resolved).isEqualTo(token);
        }

        @Test
        @DisplayName("null 토큰 처리")
        void resolveToken_NullToken_ReturnsNull() {
            // when
            String resolved = tokenProvider.resolveToken(null);

            // then
            assertThat(resolved).isNull();
        }

        @Test
        @DisplayName("빈 문자열 토큰 처리")
        void resolveToken_EmptyToken_ReturnsEmpty() {
            // when
            String resolved = tokenProvider.resolveToken("");

            // then
            assertThat(resolved).isEmpty();
        }
    }

    @Nested
    @DisplayName("extractMemberId 메서드 테스트")
    class ExtractMemberIdTests {

        @Test
        @DisplayName("HTTP 요청에서 회원 ID 추출 성공")
        void extractMemberId_ValidRequest_ReturnsMemberId() {
            // given
            String memberId = "test@example.com";
            String token = tokenProvider.generateToken(memberId, Arrays.asList("ROLE_USER"), "A",
                1L);
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

            // when
            String extractedId = tokenProvider.extractMemberId(request);

            // then
            assertThat(extractedId).isEqualTo(memberId);
        }

        @Test
        @DisplayName("Authorization 헤더가 없는 경우 예외 발생")
        void extractMemberId_NoAuthorizationHeader_ThrowsException() {
            // given
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Authorization")).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> tokenProvider.extractMemberId(request))
                .isInstanceOf(TokenException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                    ErrorCode.MISSING_AUTHORIZATION_HEADER);
        }

        @Test
        @DisplayName("Bearer 접두어가 없는 경우 예외 발생")
        void extractMemberId_NoBearerPrefix_ThrowsException() {
            // given
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Authorization")).thenReturn("InvalidToken");

            // when & then
            assertThatThrownBy(() -> tokenProvider.extractMemberId(request))
                .isInstanceOf(TokenException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                    ErrorCode.MISSING_AUTHORIZATION_HEADER);
        }
    }

    @Nested
    @DisplayName("getRefreshTokenExpiry 메서드 테스트")
    class GetRefreshTokenExpiryTests {

        @Test
        @DisplayName("리프레시 토큰 만료 시간 계산")
        void getRefreshTokenExpiry_ReturnsCorrectExpiry() {
            // given
            LocalDateTime beforeCall = LocalDateTime.now();

            // when
            LocalDateTime expiry = tokenProvider.getRefreshTokenExpiry();

            // then
            LocalDateTime afterCall = LocalDateTime.now();
            assertThat(expiry).isAfter(beforeCall);
            assertThat(expiry).isAfter(afterCall.plusDays(29)); // At least 29 days in the future
        }
    }

    @Nested
    @DisplayName("parseClames 메서드 테스트")
    class ParseClamesTests {

        @Test
        @DisplayName("유효한 토큰 클레임 파싱")
        void parseClames_ValidToken_ReturnsClaims() {
            // given
            String memberId = "test@example.com";
            Long memberNo = 1L;
            String token = tokenProvider.generateToken(memberId, Arrays.asList("ROLE_USER"), "A",
                memberNo);

            // when
            Claims claims = tokenProvider.parseClames(token);

            // then
            assertThat(claims).isNotNull();
            assertThat(claims.getSubject()).isEqualTo(memberId);
            assertThat(claims.get("memberNo", Long.class)).isEqualTo(memberNo);
        }

        @Test
        @DisplayName("만료된 토큰도 클레임 파싱 가능")
        void parseClames_ExpiredToken_ReturnsClaims() {
            // given
            String expiredToken = Jwts.builder()
                .setSubject("test@example.com")
                .claim("memberNo", 1L)
                .setIssuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60))
                .setExpiration(new Date(System.currentTimeMillis() - 1000 * 60 * 30))
                .signWith(testSecretKey)
                .compact();

            // when
            Claims claims = tokenProvider.parseClames(expiredToken);

            // then
            assertThat(claims).isNotNull();
            assertThat(claims.getSubject()).isEqualTo("test@example.com");
        }
    }
}