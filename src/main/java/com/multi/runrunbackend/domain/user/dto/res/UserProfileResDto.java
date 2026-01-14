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
    private String loginId;
    private String name;
    private String profileImageUrl;

    public static UserProfileResDto from(User user) {
        if (user.getIsDeleted()) {
            return UserProfileResDto.builder()
                .id(user.getId())
                .loginId(user.getLoginId()) // User 엔티티의 로그인 ID getter에 맞게 수정
                .name("탈퇴한 회원입니다")
                .profileImageUrl(null)
                .build();
        }

        return UserProfileResDto.builder()
            .id(user.getId())
            .loginId(user.getLoginId())     // User 엔티티의 필드명이 다르면 여기만 바꾸면 됨
            .name(user.getName())
            .profileImageUrl(user.getProfileImageUrl())
            .build();
    }
}