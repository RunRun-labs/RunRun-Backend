package com.multi.runrunbackend.domain.friend.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.friend.dto.res.FriendResDto;
import com.multi.runrunbackend.domain.friend.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 *
 * @author : kimyongwon
 * @description : 친구 요청, 수락, 거절, 목록 조회 등을 처리하는 컨트롤러
 * @filename : FriendController
 * @since : 25. 12. 30. 오전 9:43 화요일
 */

@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    /**
     * 친구 요청 보내기
     * REQUESTED 생성
     * POST /friends/{targetUserId}/requests
     */
    @PostMapping("/{targetUserId}/requests")
    public ResponseEntity<ApiResponse<Long>> sendFriendRequest(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable Long targetUserId
    ) {
        Long relationId = friendService.sendRequest(customUser, targetUserId);
        return ResponseEntity.ok(ApiResponse.success("친구 요청 성공", relationId));
    }

    /**
     * 받은 친구 요청 목록 조회
     * REQUESTED & receiver = me
     * GET /friends/requests/received
     */
    @GetMapping("/requests/received")
    public ResponseEntity<ApiResponse<List<FriendResDto>>> getReceivedFriendRequests(
            @AuthenticationPrincipal CustomUser customUser
    ) {
        List<FriendResDto> requests = friendService.getReceivedRequests(customUser);
        return ResponseEntity.ok(ApiResponse.success("받은 친구 요청 목록 조회 성공", requests));
    }

    /**
     * 보낸 친구 요청 목록 조회
     * REQUESTED & sender = me
     * GET /friends/requests/sent
     */
    @GetMapping("/requests/sent")
    public ResponseEntity<ApiResponse<List<FriendResDto>>> getSentFriendRequests(
            @AuthenticationPrincipal CustomUser customUser
    ) {
        List<FriendResDto> requests = friendService.getSentRequests(customUser);
        return ResponseEntity.ok(ApiResponse.success("보낸 친구 요청 목록 조회 성공", requests));
    }

    /**
     * 친구 요청 수락
     * REQUESTED → ACCEPTED
     * POST /friends/requests/{requestId}/accept
     */
    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptFriendRequest(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable Long requestId
    ) {
        friendService.acceptRequest(customUser, requestId);
        return ResponseEntity.ok(ApiResponse.successNoData("친구 요청 수락 성공"));
    }

    /**
     * 친구 요청 거절
     * REQUESTED → REJECTED
     * POST /friends/requests/{requestId}/reject
     */
    @PostMapping("/requests/{requestId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectFriendRequest(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable Long requestId
    ) {
        friendService.rejectRequest(customUser, requestId);
        return ResponseEntity.ok(ApiResponse.successNoData("친구 요청 거절 성공"));
    }

    /**
     * 친구 목록 조회
     * ACCEPTED
     * GET /friends
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Slice<FriendResDto>>> getFriends(
            @AuthenticationPrincipal CustomUser customUser,
            @PageableDefault(size = 5) Pageable pageable
    ) {
        Slice<FriendResDto> friends =
                friendService.getFriends(customUser, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("친구 목록 조회 성공", friends)
        );
    }

    /**
     * 친구 삭제
     * ACCEPTED → 관계 종료
     * DELETE /friends/{friendId}
     */
    @DeleteMapping("/{friendId}")
    public ResponseEntity<ApiResponse<Void>> deleteFriend(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable Long friendId
    ) {
        friendService.deleteFriend(customUser, friendId);
        return ResponseEntity.ok(ApiResponse.successNoData("친구 삭제 성공"));
    }
}