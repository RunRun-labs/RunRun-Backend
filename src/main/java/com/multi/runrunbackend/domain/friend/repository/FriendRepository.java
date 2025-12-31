package com.multi.runrunbackend.domain.friend.repository;

import com.multi.runrunbackend.domain.friend.constant.FriendStatus;
import com.multi.runrunbackend.domain.friend.entity.Friend;
import com.multi.runrunbackend.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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


    // 내가 requester인 친구 (ACCEPTED)
    Slice<Friend> findByRequesterAndStatus(
            User requester,
            FriendStatus status,
            Pageable pageable
    );

    // 내가 receiver인 친구 (ACCEPTED)
    Slice<Friend> findByReceiverAndStatus(
            User receiver,
            FriendStatus status,
            Pageable pageable
    );

    // 양방향 친구 관계 조회 ( 차단시 친구관계 삭제 등을 위해 )
    @Query("""
                select f from Friend f
                where (f.requester = :u1 and f.receiver = :u2)
                   or (f.requester = :u2 and f.receiver = :u1)
            """)
    Optional<Friend> findBetweenUsers(@Param("u1") User u1, @Param("u2") User u2);

    /*
     * 단건 조회, 중복 체크
     *  */

    Optional<Friend> findByRequesterAndReceiver(User requester, User receiver);

    boolean existsByRequesterAndReceiver(User requester, User receiver);
}