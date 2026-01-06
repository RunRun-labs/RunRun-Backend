package com.multi.runrunbackend.domain.friend.dto.res;

import com.multi.runrunbackend.domain.friend.constant.FriendStatus;
import com.multi.runrunbackend.domain.friend.entity.Friend;
import com.multi.runrunbackend.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

/**
 *
 * @author : kimyongwon
 * @description : 친구 관계(Friend 엔티티)를 조회할 때 사용되는 응답 DTO이다.
 * 현재 로그인한 사용자(me)를 기준으로 현재 친구 관계의 상태를 표현하기 위함
 * @filename : FriendResDto
 * @since : 25. 12. 30. 오전 9:39 화요일
 */

@Getter
@Builder
public class FriendResDto {

    /**
     * Friend 관계 ID
     */
    private Long friendId;

    /**
     * 상대 유저 정보
     */
    private FriendUserResDto user;

    /**
     * 친구 상태 (REQUESTED / ACCEPTED / REJECTED)
     */
    private FriendStatus status;

    /**
     * 내가 요청 보낸 건지 여부
     */
    private boolean isRequester;

    public static FriendResDto from(Friend friend, User me) {
        boolean requester = friend.getRequester().getId().equals(me.getId());

        User counterpartUser = requester
                ? friend.getReceiver()
                : friend.getRequester();

        return FriendResDto.builder()
                .friendId(friend.getId())
                .user(FriendUserResDto.from(counterpartUser))
                .status(friend.getStatus())
                .isRequester(requester)
                .build();
    }
}



