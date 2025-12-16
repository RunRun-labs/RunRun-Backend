package com.multi.runrunbackend.domain.auth.service;

import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String LoginId) throws UsernameNotFoundException {
        User user = userRepository.findByLoginId(LoginId)
            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + LoginId));

        return CustomUser.builder()
            .loginId(user.getLoginId())
            .loginPw(user.getPassword())
            .authorities(
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole())))
            .build();
    }
}
