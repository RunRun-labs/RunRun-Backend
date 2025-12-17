package com.multi.runrunbackend.domain.user.dto.res;

import com.multi.runrunbackend.domain.user.entity.User;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 *
 * @author : kimyongwon
 * @description : 사용자 계정 정보 조회 응답 DTO
 * - 인증/식별 중심 정보만 포함
 * @filename : AccountResDto
 * @since : 25. 12. 17. 오후 11:36 수요일
 */
@Getter
public class AccountResDto {

    private final String loginId;            // 아이디 (수정 불가)
    private final String email;              // 이메일
    private final String name;               // 이름
    private final LocalDateTime lastLoginAt; // 마지막 로그인 시각

    private AccountResDto(
            String loginId,
            String email,
            String name,
            LocalDateTime lastLoginAt
    ) {
        this.loginId = loginId;
        this.email = email;
        this.name = name;
        this.lastLoginAt = lastLoginAt;
    }

    public static AccountResDto from(User user) {
        return new AccountResDto(
                user.getLoginId(),
                user.getEmail(),
                user.getName(),
                user.getLastLoginAt()
        );
    }
}