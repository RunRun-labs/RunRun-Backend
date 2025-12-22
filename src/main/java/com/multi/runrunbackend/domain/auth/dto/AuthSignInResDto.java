package com.multi.runrunbackend.domain.auth.dto;

import com.multi.runrunbackend.common.jwt.dto.TokenDto;
import com.multi.runrunbackend.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AuthSignInResDto
 * @since : 2025. 12. 20. Saturday
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthSignInResDto {

    private TokenDto token;
    private Long userId;

    public static AuthSignInResDto from(TokenDto token, User user) {
        return AuthSignInResDto.builder()
            .token(token)
            .userId(user.getId())
            .build();
    }

}
