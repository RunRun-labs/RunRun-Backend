package com.multi.runrunbackend.domain.challenge.entity;

import com.multi.runrunbackend.common.entitiy.BaseTimeEntity;
import com.multi.runrunbackend.domain.challenge.constant.UserChallengeStatus;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 *
 * @author : kimyongwon
 * @description : * 사용자의 챌린지 참여 정보를 관리하는 엔티티 - 사용자 ↔ 챌린지 매핑 - 챌린지 진행 상태 관리 - 목표 대비 진행도 관리
 * @filename : UserSetting
 * @since : 25. 12. 17. 오전 11:46 수요일
 */
@Entity
@Table(
    name = "user_challenge",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"challenge_id", "user_id"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserChallenge extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserChallengeStatus status;

    @Column(name = "progress_value", nullable = false)
    private Double progressValue;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public static UserChallenge join(User user, Challenge challenge) {
        UserChallenge uc = new UserChallenge();
        uc.user = user;
        uc.challenge = challenge;
        uc.status = UserChallengeStatus.JOINED;
        uc.progressValue = 0.0;
        return uc;
    }

    public void updateProgress(double value) {
        this.progressValue = value;
        if (this.status == UserChallengeStatus.JOINED) {
            this.status = UserChallengeStatus.IN_PROGRESS;
        }
    }

    public void complete() {
        this.status = UserChallengeStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void fail() {
        this.status = UserChallengeStatus.FAILED;
        this.completedAt = LocalDateTime.now();
    }

}
