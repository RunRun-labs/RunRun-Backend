package com.multi.runrunbackend.domain.user.dto.res;

import com.multi.runrunbackend.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 *
 * @author : kimyongwon
 * @description : 사용자 프로필 조회 응답 Dto
 * @filename : UserProfileResDto
 * @since : 25. 12. 29. 오후 5:34 월요일
 */
@Getter
@Builder
@AllArgsConstructor
public class UserProfileResDto {

    private Long id;
    private String name;
    private String profileImageUrl;

    public static UserProfileResDto from(User user) {

        if (user.isDeleted()) {
            return UserProfileResDto.builder()
                    .id(user.getId())
                    .name("탈퇴한 회원입니다")
                    .profileImageUrl(null)
                    .build();
        }

        return UserProfileResDto.builder()
                .id(user.getId())
                .name(user.getName())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }
}