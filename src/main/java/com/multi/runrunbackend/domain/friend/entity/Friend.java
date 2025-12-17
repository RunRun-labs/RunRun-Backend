package com.multi.runrunbackend.domain.friend.entity;

import com.multi.runrunbackend.common.entitiy.BaseTimeEntity;
import com.multi.runrunbackend.domain.friend.constant.FriendStatus;
import com.multi.runrunbackend.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 *
 * @author : kimyongwon
 * @description : 사용자 간 친구 관계를 관리하는 엔티티
 * @filename : Friend
 * @since : 25. 12. 17. 오전 11:20 수요일
 */
@Entity
@Table(
    name = "friend",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"requester_user_id", "receiver_user_id"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Friend extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 친구 요청을 보낸 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_user_id", nullable = false)
    private User requester;

    /**
     * 친구 요청을 받은 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_user_id", nullable = false)
    private User receiver;

    /**
     * 친구 상태 REQUESTED / ACCEPTED / REJECTED
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FriendStatus status;

    /* =========================
       생성 메서드
       ========================= */

    public static Friend request(User requester, User receiver) {
        Friend friend = new Friend();
        friend.requester = requester;
        friend.receiver = receiver;
        friend.status = FriendStatus.REQUESTED;
        return friend;
    }

    /* =========================
       비즈니스 메서드
       ========================= */

    public void accept() {
        this.status = FriendStatus.ACCEPTED;
    }

    public void reject() {
        this.status = FriendStatus.REJECTED;
    }

    public boolean isAccepted() {
        return this.status == FriendStatus.ACCEPTED;
    }
}