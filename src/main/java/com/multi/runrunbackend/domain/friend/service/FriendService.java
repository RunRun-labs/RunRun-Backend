package com.multi.runrunbackend.domain.friend.service;

/**
 *
 * @author : kimyongwon
 * @description : 친구 관련 비즈니스 로직 처리 서비스
 * 현재 로직은 REJECTED 상태 재요청 불가
 * @filename : FriendService
 * @since : 25. 12. 30. 오전 9:48 화요일
 */

import com.multi.runrunbackend.common.exception.custom.*;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.friend.constant.FriendStatus;
import com.multi.runrunbackend.domain.friend.dto.res.FriendResDto;
import com.multi.runrunbackend.domain.friend.entity.Friend;
import com.multi.runrunbackend.domain.friend.repository.FriendRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    /**
     * 친구 요청 보내기
     * REQUESTED 생성
     */
    @Transactional
    public Long sendRequest(CustomUser principal, Long targetUserId) {
        User requester = getUserByPrincipal(principal);
        User receiver = getUserById(targetUserId);

        // 자기 자신에게 요청
        if (requester.getId().equals(receiver.getId())) {
            throw new InvalidRequestException(ErrorCode.FRIEND_REQUEST_SELF);
        }

        // A → B or B → A 이미 관계/요청 존재 여부 확인
        if (existsBetweenUsers(requester, receiver)) {
            throw new DuplicateException(ErrorCode.ALREADY_FRIEND_REQUEST);
        }

        Friend friend = Friend.request(requester, receiver);
        friendRepository.save(friend);

        return friend.getId();
    }

    /**
     * 받은 친구 요청 목록 조회
     * receiver = me, status = REQUESTED
     */
    @Transactional(readOnly = true)
    public List<FriendResDto> getReceivedRequests(CustomUser principal) {
        User me = getUserByPrincipal(principal);

        return friendRepository
                .findByReceiverAndStatus(me, FriendStatus.REQUESTED)
                .stream()
                .map(friend -> FriendResDto.from(friend, me))
                .toList();
    }

    /**
     * 보낸 친구 요청 목록 조회
     * requester = me, status = REQUESTED
     */
    @Transactional(readOnly = true)
    public List<FriendResDto> getSentRequests(CustomUser principal) {
        User me = getUserByPrincipal(principal);

        return friendRepository
                .findByRequesterAndStatus(me, FriendStatus.REQUESTED)
                .stream()
                .map(friend -> FriendResDto.from(friend, me))
                .toList();
    }

    /**
     * 친구 요청 수락
     * REQUESTED → ACCEPTED
     */
    @Transactional
    public void acceptRequest(CustomUser principal, Long requestId) {
        User me = getUserByPrincipal(principal);
        Friend friend = getFriend(requestId);

        validateReceiver(friend, me);
        validatePending(friend);

        friend.accept();
    }

    /**
     * 친구 요청 거절
     * REQUESTED → REJECTED
     */
    @Transactional
    public void rejectRequest(CustomUser principal, Long requestId) {
        User me = getUserByPrincipal(principal);
        Friend friend = getFriend(requestId);

        validateReceiver(friend, me);
        validatePending(friend);

        friend.reject();
    }

    /**
     * 친구 목록 조회
     * ACCEPTED
     */
    @Transactional(readOnly = true)
    public Page<FriendResDto> getFriends(
            CustomUser principal,
            Pageable pageable
    ) {
        User me = getUserByPrincipal(principal);

        // JPQL 정렬 제거에 따른 기본 정렬(최신순) 적용
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        return friendRepository
                .findFriends(me, FriendStatus.ACCEPTED, pageable)
                .map(friend -> FriendResDto.from(friend, me));
    }

    /**
     * 친구 삭제
     * ACCEPTED → 관계 종료
     */
    @Transactional
    public void deleteFriend(CustomUser principal, Long friendUserId) {
        User me = getUserByPrincipal(principal);
        User target = getUserById(friendUserId);

        Friend friend = findAcceptedBetween(me, target)
                .orElseThrow(() -> new NotFoundException(ErrorCode.FRIEND_NOT_FOUND));

        friendRepository.delete(friend);
    }

    /* =========================
       Private Helper Methods
       ========================= */

    private boolean existsBetweenUsers(User user1, User user2) {
        return friendRepository.existsByRequesterAndReceiver(user1, user2)
                || friendRepository.existsByRequesterAndReceiver(user2, user1);
    }

    private Optional<Friend> findAcceptedBetween(User user1, User user2) {
        return friendRepository.findByRequesterAndReceiver(user1, user2)
                .filter(f -> f.getStatus() == FriendStatus.ACCEPTED)
                .or(() -> friendRepository.findByRequesterAndReceiver(user2, user1)
                        .filter(f -> f.getStatus() == FriendStatus.ACCEPTED));
    }

    private Friend getFriend(Long requestId) {
        return friendRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));
    }

    private void validateReceiver(Friend friend, User me) {
        if (!friend.getReceiver().getId().equals(me.getId())) {
            throw new ForbiddenException(ErrorCode.FRIEND_REQUEST_FORBIDDEN);
        }
    }

    private void validatePending(Friend friend) {
        if (friend.getStatus() != FriendStatus.REQUESTED) {
            throw new InvalidRequestException(ErrorCode.NOT_PENDING_FRIEND_REQUEST);
        }
    }

    private User getUserByPrincipal(CustomUser principal) {
        if (principal == null) {
            throw new TokenException(ErrorCode.UNAUTHORIZED);
        }

        return userRepository.findByLoginId(principal.getLoginId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }
}