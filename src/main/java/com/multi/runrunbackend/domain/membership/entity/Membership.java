package com.multi.runrunbackend.domain.membership.entity;

import com.multi.runrunbackend.common.entitiy.BaseEntity;
import com.multi.runrunbackend.domain.membership.constant.MembershipGrade;
import com.multi.runrunbackend.domain.membership.constant.MembershipStatus;
import com.multi.runrunbackend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * @author : BoKyung
 * @description : 멤버십 정보 엔티티
 * @filename : Membership
 * @since : 25. 12. 17. 수요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Membership extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_grade", nullable = false, length = 20)
    private MembershipGrade membershipGrade; // FREE, PREMIUM

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_status", nullable = false, length = 20)
    private MembershipStatus membershipStatus; // ACTIVE, CANCELED, EXPIRED

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "next_billing_date")
    private LocalDateTime nextBillingDate;

    /**
     * @description : 엔티티 생성 전 기본값 설정
     * @author : BoKyung
     * @since : 25. 12. 30. 월요일
     */
    @PrePersist
    public void prePersist() {
        if (this.membershipGrade == null) {
            this.membershipGrade = MembershipGrade.FREE;
        }
        if (this.membershipStatus == null) {
            this.membershipStatus = MembershipStatus.ACTIVE;
        }
    }

    /**
     * @description : 무료 멤버십 생성 정적 팩토리 메서드
     * @author : BoKyung
     * @since : 25. 12. 30. 월요일
     */
    public static Membership create(User user) {
        Membership membership = new Membership();
        membership.user = user;
        membership.membershipGrade = MembershipGrade.FREE;
        membership.membershipStatus = MembershipStatus.ACTIVE;
        membership.startDate = LocalDateTime.now();
        return membership;
    }

    /**
     * @description : 프리미엄 멤버십 업그레이드
     * @author : BoKyung
     * @since : 25. 12. 30. 월요일
     */
    public void upgradeToPremium() {
        LocalDateTime now = LocalDateTime.now();
        this.membershipGrade = MembershipGrade.PREMIUM;
        this.membershipStatus = MembershipStatus.ACTIVE;
        this.nextBillingDate = now.plusMonths(1);
    }

    /**
     * @description : 멤버십 해지 신청
     * @author : BoKyung
     * @since : 25. 12. 30. 월요일
     */
    public void cancel() {
        this.membershipStatus = MembershipStatus.CANCELED;
    }

    /**
     * @description : 멤버십 만료 처리
     * @author : BoKyung
     * @since : 25. 12. 30. 월요일
     */
    public void expire() {
        this.membershipGrade = MembershipGrade.FREE;
        this.membershipStatus = MembershipStatus.EXPIRED;
        this.endDate = LocalDateTime.now();
        this.nextBillingDate = null;
    }

    /**
     * @description : 멤버십 갱신
     * @author : BoKyung
     * @since : 25. 12. 30. 월요일
     */
    public void renew() {
        // null이면 (FREE 회원이 PREMIUM 구독 시작)
        if (this.nextBillingDate == null) {
            this.nextBillingDate = LocalDateTime.now().plusMonths(1);
            this.membershipGrade = MembershipGrade.PREMIUM;
        }
        // 이미 있으면 (기존 PREMIUM 회원 갱신)
        else {
            this.nextBillingDate = this.nextBillingDate.plusMonths(1);
        }

        // 상태는 무조건 ACTIVE
        this.membershipStatus = MembershipStatus.ACTIVE;
    }

    /**
     * @description : 종료일 설정
     * @author : BoKyung
     * @since : 25. 12. 30. 월요일
     */
    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
}