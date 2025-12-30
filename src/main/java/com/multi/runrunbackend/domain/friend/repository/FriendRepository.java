package com.multi.runrunbackend.domain.friend.repository;

import com.multi.runrunbackend.domain.friend.constant.FriendStatus;
import com.multi.runrunbackend.domain.friend.entity.Friend;
import com.multi.runrunbackend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 *
 * @author : kimyongwon
 * @description : 친구 관계 조회 및 중복 확인 메소드 정의
 * @filename : FriendRepository
 * @since : 25. 12. 30. 오전 9:22 화요일
 */
public interface FriendRepository extends JpaRepository<Friend, Long> {

    // 받은 친구 요청 (REQUESTED)
    List<Friend> findByReceiverAndStatus(User receiver, FriendStatus status);

    // 보낸 친구 요청 (REQUESTED)
    List<Friend> findByRequesterAndStatus(User requester, FriendStatus status);

    // requester → receiver 단방향 조회
    Optional<Friend> findByRequesterAndReceiver(User requester, User receiver);

    // requester → receiver 단방향 존재 여부
    boolean existsByRequesterAndReceiver(User requester, User receiver);
}