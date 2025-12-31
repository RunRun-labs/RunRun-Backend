package com.multi.runrunbackend.domain.friend.dto.res;

import com.multi.runrunbackend.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

/**
 *
 * @author : kimyongwon
 * @description : 친구의 User 엔티티를 직접 노출하지 않고 필요한 정보만 전달하기 위한 DTO
 * @filename : FriendUserResDto
 * @since : 25. 12. 30. 오전 11:17 화요일
 */

@Getter
@Builder
public class FriendUserResDto {

    private Long userId;
    private String loginId;
    private String name;
    private String profileImageUrl;

    public static FriendUserResDto from(User user) {
        return FriendUserResDto.builder()
                .userId(user.getId())
                .loginId(user.getLoginId())
                .name(user.getName())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }
}