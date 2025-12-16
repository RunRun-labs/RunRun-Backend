package com.multi.runrunbackend.domain.user.dto.req;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserSignInDto 단위 테스트")
class UserSignInDtoTest {

    @Test
    @DisplayName("Builder를 사용한 UserSignInDto 생성")
    void createUserSignInDto_WithBuilder() {
        // given & when
        UserSignInDto dto = UserSignInDto.builder()
            .loginId("testuser")
            .loginPw("password123")
            .build();

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getLoginId()).isEqualTo("testuser");
        assertThat(dto.getLoginPw()).isEqualTo("password123");
    }

    @Test
    @DisplayName("NoArgsConstructor를 사용한 생성")
    void createUserSignInDto_NoArgsConstructor() {
        // when
        UserSignInDto dto = new UserSignInDto();

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getLoginId()).isNull();
        assertThat(dto.getLoginPw()).isNull();
    }

    @Test
    @DisplayName("AllArgsConstructor를 사용한 생성")
    void createUserSignInDto_AllArgsConstructor() {
        // when
        UserSignInDto dto = new UserSignInDto("testuser", "password123");

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getLoginId()).isEqualTo("testuser");
        assertThat(dto.getLoginPw()).isEqualTo("password123");
    }

    @Test
    @DisplayName("Setter 테스트")
    void setters_WorkCorrectly() {
        // given
        UserSignInDto dto = new UserSignInDto();

        // when
        dto.setLoginId("newuser");
        dto.setLoginPw("newpassword");

        // then
        assertThat(dto.getLoginId()).isEqualTo("newuser");
        assertThat(dto.getLoginPw()).isEqualTo("newpassword");
    }

    @Test
    @DisplayName("equals와 hashCode 테스트")
    void equalsAndHashCode() {
        // given
        UserSignInDto dto1 = UserSignInDto.builder()
            .loginId("testuser")
            .loginPw("password123")
            .build();

        UserSignInDto dto2 = UserSignInDto.builder()
            .loginId("testuser")
            .loginPw("password123")
            .build();

        UserSignInDto dto3 = UserSignInDto.builder()
            .loginId("different")
            .loginPw("different")
            .build();

        // then
        assertThat(dto1).isEqualTo(dto2);
        assertThat(dto1).isNotEqualTo(dto3);
        assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
    }
}