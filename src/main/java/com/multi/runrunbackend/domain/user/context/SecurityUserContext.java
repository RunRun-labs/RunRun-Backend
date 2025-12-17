package com.multi.runrunbackend.domain.user.context;

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
public class SecurityUserContext implements UserContext {

    @Override
    public Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(authentication.getName());
    }
}