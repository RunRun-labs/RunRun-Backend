package com.multi.runrunbackend.common.jwt.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TokenDto 단위 테스트")
class TokenDtoTest {

    @Test
    @DisplayName("Builder를 사용한 TokenDto 생성")
    void createTokenDto_WithBuilder() {
        // given & when
        TokenDto dto = TokenDto.builder()
            .grantType("Bearer")
            .accessToken("accessToken123")
            .refreshToken("refreshToken456")
            .build();

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getGrantType()).isEqualTo("Bearer");
        assertThat(dto.getAccessToken()).isEqualTo("accessToken123");
        assertThat(dto.getRefreshToken()).isEqualTo("refreshToken456");
    }

    @Test
    @DisplayName("Builder 기본값 테스트 - grantType은 Bearer")
    void builder_DefaultGrantType() {
        // given & when
        TokenDto dto = TokenDto.builder()
            .accessToken("accessToken")
            .refreshToken("refreshToken")
            .build();

        // then
        assertThat(dto.getGrantType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("NoArgsConstructor 테스트")
    void createTokenDto_NoArgsConstructor() {
        // when
        TokenDto dto = new TokenDto();

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getGrantType()).isNull();
        assertThat(dto.getAccessToken()).isNull();
        assertThat(dto.getRefreshToken()).isNull();
    }

    @Test
    @DisplayName("AllArgsConstructor 테스트")
    void createTokenDto_AllArgsConstructor() {
        // when
        TokenDto dto = new TokenDto("Bearer", "access", "refresh");

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getGrantType()).isEqualTo("Bearer");
        assertThat(dto.getAccessToken()).isEqualTo("access");
        assertThat(dto.getRefreshToken()).isEqualTo("refresh");
    }

    @Test
    @DisplayName("Setter 테스트")
    void setters_WorkCorrectly() {
        // given
        TokenDto dto = new TokenDto();

        // when
        dto.setGrantType("Bearer");
        dto.setAccessToken("newAccessToken");
        dto.setRefreshToken("newRefreshToken");

        // then
        assertThat(dto.getGrantType()).isEqualTo("Bearer");
        assertThat(dto.getAccessToken()).isEqualTo("newAccessToken");
        assertThat(dto.getRefreshToken()).isEqualTo("newRefreshToken");
    }

    @Test
    @DisplayName("equals와 hashCode 테스트")
    void equalsAndHashCode() {
        // given
        TokenDto dto1 = TokenDto.builder()
            .grantType("Bearer")
            .accessToken("access")
            .refreshToken("refresh")
            .build();

        TokenDto dto2 = TokenDto.builder()
            .grantType("Bearer")
            .accessToken("access")
            .refreshToken("refresh")
            .build();

        TokenDto dto3 = TokenDto.builder()
            .grantType("Bearer")
            .accessToken("different")
            .refreshToken("different")
            .build();

        // then
        assertThat(dto1).isEqualTo(dto2);
        assertThat(dto1).isNotEqualTo(dto3);
        assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
    }

    @Test
    @DisplayName("커스텀 grantType 설정")
    void customGrantType() {
        // given & when
        TokenDto dto = TokenDto.builder()
            .grantType("CustomBearer")
            .accessToken("access")
            .refreshToken("refresh")
            .build();

        // then
        assertThat(dto.getGrantType()).isEqualTo("CustomBearer");
    }
}