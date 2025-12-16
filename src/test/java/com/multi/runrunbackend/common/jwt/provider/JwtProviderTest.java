package com.multi.runrunbackend.common.jwt.provider;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.security.Keys;
import java.util.Base64;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("JwtProvider 단위 테스트")
class JwtProviderTest {

    private JwtProvider jwtProvider;
    private String testSecretKey;
    private String testIssuer;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider();
        testSecretKey = Base64.getEncoder().encodeToString(
            "test-secret-key-for-jwt-token-generation-must-be-long-enough-for-hs512".getBytes()
        );
        testIssuer = "test-issuer";

        ReflectionTestUtils.setField(jwtProvider, "secretKey", testSecretKey);
        ReflectionTestUtils.setField(jwtProvider, "issuer", testIssuer);
    }

    @Nested
    @DisplayName("getIssuer 메서드 테스트")
    class GetIssuerTests {

        @Test
        @DisplayName("Issuer 반환")
        void getIssuer_ReturnsCorrectIssuer() {
            // when
            String result = jwtProvider.getIssuer();

            // then
            assertThat(result).isEqualTo(testIssuer);
        }

        @Test
        @DisplayName("Issuer가 null이 아님")
        void getIssuer_IsNotNull() {
            // when
            String result = jwtProvider.getIssuer();

            // then
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("getSecretKey 메서드 테스트")
    class GetSecretKeyTests {

        @Test
        @DisplayName("SecretKey 반환")
        void getSecretKey_ReturnsValidKey() {
            // when
            SecretKey result = jwtProvider.getSecretKey();

            // then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("SecretKey가 올바른 형식")
        void getSecretKey_HasCorrectFormat() {
            // when
            SecretKey result = jwtProvider.getSecretKey();

            // then
            assertThat(result.getAlgorithm()).isEqualTo("HmacSHA512");
        }

        @Test
        @DisplayName("SecretKey가 매번 동일한 인스턴스 반환")
        void getSecretKey_ReturnsSameInstance() {
            // when
            SecretKey key1 = jwtProvider.getSecretKey();
            SecretKey key2 = jwtProvider.getSecretKey();

            // then
            assertThat(key1.getEncoded()).isEqualTo(key2.getEncoded());
        }

        @Test
        @DisplayName("SecretKey의 길이가 충분함")
        void getSecretKey_HasSufficientLength() {
            // when
            SecretKey result = jwtProvider.getSecretKey();

            // then
            // HS512 requires at least 512 bits (64 bytes)
            assertThat(result.getEncoded().length).isGreaterThanOrEqualTo(64);
        }
    }

    @Nested
    @DisplayName("통합 테스트")
    class IntegrationTests {

        @Test
        @DisplayName("JwtProvider 초기화 성공")
        void initialization_Success() {
            // given
            JwtProvider provider = new JwtProvider();
            ReflectionTestUtils.setField(provider, "secretKey", testSecretKey);
            ReflectionTestUtils.setField(provider, "issuer", "test");

            // when
            String issuer = provider.getIssuer();
            SecretKey key = provider.getSecretKey();

            // then
            assertThat(issuer).isNotNull();
            assertThat(key).isNotNull();
        }

        @Test
        @DisplayName("다양한 secret key 길이 테스트")
        void variousSecretKeyLengths_Work() {
            // given - Create keys of different lengths (all valid for HS512)
            String[] secretKeys = {
                Base64.getEncoder().encodeToString(new byte[64]), // Minimum for HS512
                Base64.getEncoder().encodeToString(new byte[128]),
                Base64.getEncoder().encodeToString(new byte[256])
            };

            // when & then
            for (String secret : secretKeys) {
                JwtProvider provider = new JwtProvider();
                ReflectionTestUtils.setField(provider, "secretKey", secret);
                ReflectionTestUtils.setField(provider, "issuer", "test");

                SecretKey key = provider.getSecretKey();
                assertThat(key).isNotNull();
                assertThat(key.getEncoded().length).isGreaterThanOrEqualTo(64);
            }
        }
    }

    @Nested
    @DisplayName("경계값 테스트")
    class EdgeCaseTests {

        @Test
        @DisplayName("매우 긴 issuer 문자열 처리")
        void veryLongIssuer_Works() {
            // given
            String longIssuer = "a".repeat(1000);
            ReflectionTestUtils.setField(jwtProvider, "issuer", longIssuer);

            // when
            String result = jwtProvider.getIssuer();

            // then
            assertThat(result).isEqualTo(longIssuer);
            assertThat(result.length()).isEqualTo(1000);
        }

        @Test
        @DisplayName("특수 문자가 포함된 issuer 처리")
        void specialCharactersInIssuer_Works() {
            // given
            String specialIssuer = "test-issuer@#$%^&*()_+{}|:<>?";
            ReflectionTestUtils.setField(jwtProvider, "issuer", specialIssuer);

            // when
            String result = jwtProvider.getIssuer();

            // then
            assertThat(result).isEqualTo(specialIssuer);
        }
    }
}