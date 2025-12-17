package com.multi.runrunbackend.domain.user.entity;

import com.multi.runrunbackend.common.entitiy.BaseCreatedEntity;
import jakarta.persistence.Entity;
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
 * @author : 김용원
 * @description : 특정 사용자를 차단하기 위한 엔티티
 * @filename : UserBlock
 * @since : 25. 12. 17. 오전 11:27 수요일
 */
@Entity
@Table(
    name = "user_block",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "blocked_user_id"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBlock extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_user_id", nullable = false)
    private User blockedUser;

    public static UserBlock block(User blocker, User blockedUser) {
        UserBlock userBlock = new UserBlock();
        userBlock.blocker = blocker;
        userBlock.blockedUser = blockedUser;
        return userBlock;
    }
}