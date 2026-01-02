package com.multi.runrunbackend.domain.challenge.entity;

import com.multi.runrunbackend.common.entitiy.BaseTimeEntity;
import com.multi.runrunbackend.domain.challenge.constant.UserChallengeStatus;
import com.multi.runrunbackend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 *
 * @author : kimyongwon
 * @description : * 사용자의 챌린지 참여 정보를 관리하는 엔티티 - 사용자 ↔ 챌린지 매핑 - 챌린지 진행 상태 관리 - 목표 대비 진행도 관리
 * @filename : UserChallenge
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
        uc.progressValue = 0.0;

        if (!challenge.getStartDate().isAfter(LocalDate.now())) {
            uc.status = UserChallengeStatus.IN_PROGRESS;
        } else {
            uc.status = UserChallengeStatus.JOINED;
        }

        return uc;
    }

    public void startProgress() {
        if (this.status == UserChallengeStatus.JOINED) {
            this.status = UserChallengeStatus.IN_PROGRESS;
        }
    }

    public void updateProgress(double value) {
        // 아직 시작하지 않은 챌린지(JOINED)는 업데이트 불가
        if (this.status == UserChallengeStatus.JOINED) {
            return;
        }

        this.progressValue = value;

        Double target = this.challenge.getTargetValue();
        if (target != null && this.progressValue >= target) {
            complete();
        }
    }

    public void complete() {
        // 이미 완료/실패 상태가 아닐 때만 완료 처리
        if (this.status != UserChallengeStatus.COMPLETED && this.status != UserChallengeStatus.FAILED) {
            this.status = UserChallengeStatus.COMPLETED;
            this.completedAt = LocalDateTime.now();
        }
    }

    public void fail() {
        // 이미 완료된 건은 실패 처리하지 않음
        if (this.status != UserChallengeStatus.COMPLETED) {
            this.status = UserChallengeStatus.FAILED;
            this.completedAt = LocalDateTime.now();
        }
    }

}
