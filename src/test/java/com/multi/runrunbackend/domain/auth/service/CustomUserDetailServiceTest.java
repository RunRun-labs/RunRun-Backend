package com.multi.runrunbackend.domain.auth.service;

import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailService 단위 테스트")
class CustomUserDetailServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailService customUserDetailService;

    @Test
    @DisplayName("사용자 로드 성공")
    void loadUserByUsername_Success() {
        // given
        String loginId = "testuser";
        User user = User.builder()
            .id(1L)
            .loginId(loginId)
            .password("encodedPassword")
            .email("test@example.com")
            .role("ROLE_USER")
            .name("Test User")
            .gender("M")
            .birthDate(LocalDate.of(1990, 1, 1))
            .build();

        when(userRepository.findByLoginId(loginId)).thenReturn(Optional.of(user));

        // when
        UserDetails userDetails = customUserDetailService.loadUserByUsername(loginId);

        // then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(loginId);
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword");
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("사용자 로드 실패 - 사용자를 찾을 수 없음")
    void loadUserByUsername_UserNotFound() {
        // given
        String loginId = "nonexistent";
        when(userRepository.findByLoginId(loginId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> customUserDetailService.loadUserByUsername(loginId))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("관리자 권한을 가진 사용자 로드")
    void loadUserByUsername_AdminUser() {
        // given
        String loginId = "adminuser";
        User user = User.builder()
            .id(1L)
            .loginId(loginId)
            .password("encodedPassword")
            .email("admin@example.com")
            .role("ROLE_ADMIN")
            .name("Admin User")
            .gender("M")
            .birthDate(LocalDate.of(1990, 1, 1))
            .build();

        when(userRepository.findByLoginId(loginId)).thenReturn(Optional.of(user));

        // when
        UserDetails userDetails = customUserDetailService.loadUserByUsername(loginId);

        // then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
    }
}