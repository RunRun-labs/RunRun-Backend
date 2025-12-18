package com.multi.runrunbackend.domain.user.context;

import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 *
 * @author : kimyongwon
 * @description : Please explain the class!!!
 * @filename : SecurityUserContext
 * @since : 25. 12. 17. 오후 11:19 수요일
 */
@Component
@RequiredArgsConstructor
public class SecurityUserContext implements UserContext {

    private final UserRepository userRepository;

    @Override
    public Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUser customUser)) {
            throw new IllegalStateException("인증된 사용자 정보가 없습니다.");
        }

        return userRepository.findByLoginId(customUser.getEmail())
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 사용자입니다."))
                .getId();
    }
}