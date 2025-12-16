package com.multi.runrunbackend.domain.auth.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CustomUser 단위 테스트")
class CustomUserTest {

    @Test
    @DisplayName("CustomUser 생성 - Builder 사용")
    void createCustomUser_WithBuilder() {
        // given
        Collection<GrantedAuthority> authorities = Arrays.asList(
            new SimpleGrantedAuthority("ROLE_USER")
        );

        // when
        CustomUser customUser = CustomUser.builder()
            .loginId("testuser")
            .email("test@example.com")
            .loginPw("encodedPassword")
            .authorities(authorities)
            .build();

        // then
        assertThat(customUser).isNotNull();
        assertThat(customUser.getLoginId()).isEqualTo("testuser");
        assertThat(customUser.getEmail()).isEqualTo("test@example.com");
        assertThat(customUser.getLoginPw()).isEqualTo("encodedPassword");
        assertThat(customUser.getAuthorities()).hasSize(1);
    }

    @Test
    @DisplayName("UserDetails 인터페이스 구현 - getUsername")
    void getUserDetails_GetUsername() {
        // given
        CustomUser customUser = CustomUser.builder()
            .loginId("testuser")
            .loginPw("password")
            .build();

        // when & then
        assertThat(customUser.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("UserDetails 인터페이스 구현 - getPassword")
    void getUserDetails_GetPassword() {
        // given
        CustomUser customUser = CustomUser.builder()
            .loginId("testuser")
            .loginPw("encodedPassword")
            .build();

        // when & then
        assertThat(customUser.getPassword()).isEqualTo("encodedPassword");
    }

    @Test
    @DisplayName("UserDetails 인터페이스 구현 - getAuthorities")
    void getUserDetails_GetAuthorities() {
        // given
        Collection<GrantedAuthority> authorities = Arrays.asList(
            new SimpleGrantedAuthority("ROLE_USER"),
            new SimpleGrantedAuthority("ROLE_ADMIN")
        );

        CustomUser customUser = CustomUser.builder()
            .loginId("adminuser")
            .loginPw("password")
            .authorities(authorities)
            .build();

        // when & then
        assertThat(customUser.getAuthorities()).hasSize(2);
        assertThat(customUser.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("UserDetails 인터페이스 구현 - 계정 상태 확인")
    void getUserDetails_AccountStatus() {
        // given
        CustomUser customUser = CustomUser.builder()
            .loginId("testuser")
            .loginPw("password")
            .build();

        // when & then
        assertThat(customUser.isAccountNonExpired()).isTrue();
        assertThat(customUser.isAccountNonLocked()).isTrue();
        assertThat(customUser.isCredentialsNonExpired()).isTrue();
        assertThat(customUser.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("CustomUser 생성 - 여러 권한")
    void createCustomUser_MultipleAuthorities() {
        // given
        Collection<GrantedAuthority> authorities = Arrays.asList(
            new SimpleGrantedAuthority("ROLE_USER"),
            new SimpleGrantedAuthority("ROLE_ADMIN"),
            new SimpleGrantedAuthority("ROLE_MANAGER")
        );

        // when
        CustomUser customUser = CustomUser.builder()
            .loginId("superuser")
            .email("super@example.com")
            .loginPw("password")
            .authorities(authorities)
            .build();

        // then
        assertThat(customUser.getAuthorities()).hasSize(3);
    }

    @Test
    @DisplayName("CustomUser Setter 테스트")
    void customUser_SetterTests() {
        // given
        CustomUser customUser = new CustomUser();

        // when
        customUser.setLoginId("newuser");
        customUser.setEmail("new@example.com");
        customUser.setLoginPw("newpassword");
        
        Collection<GrantedAuthority> authorities = Arrays.asList(
            new SimpleGrantedAuthority("ROLE_USER")
        );
        customUser.setAuthorities(authorities);

        // then
        assertThat(customUser.getLoginId()).isEqualTo("newuser");
        assertThat(customUser.getEmail()).isEqualTo("new@example.com");
        assertThat(customUser.getLoginPw()).isEqualTo("newpassword");
        assertThat(customUser.getAuthorities()).hasSize(1);
    }
}