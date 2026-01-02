package com.multi.runrunbackend.domain.membership.entity;

import com.multi.runrunbackend.common.entitiy.BaseEntity;
import com.multi.runrunbackend.common.exception.custom.BusinessException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
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
    @Column(name = "membership_status", nullable = false, length = 20)
    private MembershipStatus membershipStatus; // ACTIVE, CANCELED, EXPIRED

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "next_billing_date")
    private LocalDateTime nextBillingDate;

    /**
     * @description : 무료 멤버십 생성 정적 팩토리 메서드
     */
    public static Membership create(User user) {
        Membership membership = new Membership();
        membership.user = user;
        membership.membershipStatus = MembershipStatus.ACTIVE;
        membership.startDate = LocalDateTime.now();
        membership.nextBillingDate = LocalDateTime.now().plusMonths(1);
        return membership;
    }

    /**
     * @description : 멤버십 재활성화
     */
    public void reactivate() {
        this.membershipStatus = MembershipStatus.ACTIVE;
        this.startDate = LocalDateTime.now();
        this.nextBillingDate = LocalDateTime.now().plusMonths(1);
        this.endDate = null;
    }

    /**
     * @description : 멤버십 갱신 (자동 결제 성공 시)
     */
    public void renew() {
        // 다음 결제일 연장
        if (this.nextBillingDate == null) {
            this.nextBillingDate = LocalDateTime.now().plusMonths(1);
        } else {
            this.nextBillingDate = this.nextBillingDate.plusMonths(1);
        }

        // 상태 활성화
        this.membershipStatus = MembershipStatus.ACTIVE;
        this.endDate = null;
    }

    /**
     * @description : 멤버십 해지 신청
     */
    public void cancel() {
        this.membershipStatus = MembershipStatus.CANCELED;
    }

    /**
     * @description : 해지 취소 (다시 구독)
     */
    public void cancelCancellation() {
        if (this.membershipStatus != MembershipStatus.CANCELED) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_NOT_CANCELED);
        }

        this.membershipStatus = MembershipStatus.ACTIVE;
        this.endDate = null;  // nextBillingDate는 유지 (기존 결제일 그대로)
    }

    /**
     * @description : 멤버십 만료 처리
     * @author : BoKyung
     * @since : 25. 12. 30. 월요일
     */
    public void expire() {
        this.membershipStatus = MembershipStatus.EXPIRED;
        this.endDate = LocalDateTime.now();
        this.nextBillingDate = null;
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