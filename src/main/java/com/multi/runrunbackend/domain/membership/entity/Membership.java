package com.multi.runrunbackend.domain.membership.entity;

import com.multi.runrunbackend.common.entitiy.BaseEntity;
import com.multi.runrunbackend.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(name = "membership_grade", nullable = false, length = 20)
    private String membershipGrade; // FREE, PREMIUM

    @Column(name = "membership_status", nullable = false, length = 20)
    private String membershipStatus; // ACTIVE, CANCELED, EXPIRED

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "next_billing_date")
    private LocalDateTime nextBillingDate;

    /**
     * @description : toEntity - 엔티티 생성 정적 팩토리 메서드 (무료 회원)
     * @filename : Membership
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public static Membership toEntity(User user) {
        LocalDateTime now = LocalDateTime.now();
        return Membership.builder()
            .user(user)
            .membershipGrade("FREE")
            .membershipStatus("ACTIVE")
            .startDate(now)
            .build();
    }

    /**
     * @description : upgradeToPremium - 프리미엄 멤버십 업그레이드
     * @filename : Membership
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public void upgradeToPremium() {
        LocalDateTime now = LocalDateTime.now();
        this.membershipGrade = "PREMIUM";
        this.membershipStatus = "ACTIVE";
        this.nextBillingDate = now.plusMonths(1); // 1개월 후
    }

    /**
     * @description : cancelMembership - 멤버십 해지
     * @filename : Membership
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public void cancelMembership() {

        this.membershipStatus = "CANCELED";
    }

    /**
     * @description : expireMembership - 멤버십 만료 처리
     * @filename : Membership
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public void expireMembership() {
        this.membershipGrade = "FREE";
        this.membershipStatus = "EXPIRED";
        this.endDate = LocalDateTime.now();
        this.nextBillingDate = null;
    }

    /**
     * @description : renewMembership - 멤버십 갱신
     * @filename : Membership
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public void renewMembership() {
        this.nextBillingDate = this.nextBillingDate.plusMonths(1);
    }
}
