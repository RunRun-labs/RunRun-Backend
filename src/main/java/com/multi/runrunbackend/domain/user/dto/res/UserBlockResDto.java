package com.multi.runrunbackend.domain.user.dto.res;

import com.multi.runrunbackend.domain.user.entity.UserBlock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 *
 * @author : kimyongwon
 * @description : 차단 목록 조회시 반환될 DTO
 * @filename : UserBlockResDto
 * @since : 25. 12. 29. 오전 12:11 월요일
 */
@Getter
@Builder
@AllArgsConstructor
public class UserBlockResDto {
    private Long blockId;        // 차단 관계 ID (해제 시 필요할 수도 있음)
    private Long blockedUserId;  // 차단된 유저 ID
    private String loginId;
    private String name;
    private String profileImageUrl;

    public static UserBlockResDto from(UserBlock userBlock) {
        return UserBlockResDto.builder()
                .blockId(userBlock.getId())
                .blockedUserId(userBlock.getBlockedUser().getId())
                .loginId(userBlock.getBlockedUser().getLoginId())
                .name(userBlock.getBlockedUser().getName())
                .profileImageUrl(userBlock.getBlockedUser().getProfileImageUrl())
                .build();
    }
}