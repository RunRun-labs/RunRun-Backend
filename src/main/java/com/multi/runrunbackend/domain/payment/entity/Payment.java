package com.multi.runrunbackend.domain.payment.entity;

import com.multi.runrunbackend.common.entitiy.BaseCreatedEntity;
import com.multi.runrunbackend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * @author : BoKyung
 * @description : 결제 내역 엔티티
 * @filename : Payment
 * @since : 25. 12. 17. 수요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Payment extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "membership_grade", nullable = false, length = 20)
    private String membershipGrade;

    @Column(name = "original_amount", nullable = false)
    private Integer originalAmount;

    @Column(name = "discount_amount", nullable = false)
    private Integer discountAmount;

    @Column(name = "final_amount")
    private Integer finalAmount;

    @Column(name = "payment_method", length = 20)
    private String paymentMethod; // TOSS_PAY

    @Column(name = "payment_status", length = 20)
    private String paymentStatus; // PENDING, COMPLETED, FAILED, CANCELED

    @Column(name = "payment_key", length = 100)
    private String paymentKey;

    @Column(name = "order_id", length = 100)
    private String orderId;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;


    /**
     * @description : toEntity - 엔티티 생성 정적 팩토리 메서드
     * @filename : Payment
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public static Payment toEntity(User user, String membershipGrade, Integer originalAmount,
                                   Integer discountAmount, String orderId) {
        return Payment.builder()
                .user(user)
                .membershipGrade(membershipGrade)
                .originalAmount(originalAmount)
                .discountAmount(discountAmount)
                .finalAmount(originalAmount - discountAmount)
                .paymentMethod("TOSS_PAY")
                .paymentStatus("PENDING")
                .orderId(orderId)
                .build();
    }

    /**
     * @description : completePayment - 결제 완료 처리
     * @filename : Payment
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public void completePayment(String paymentKey) {
        this.paymentStatus = "COMPLETED";
        this.paymentKey = paymentKey;
        this.approvedAt = LocalDateTime.now();
    }

    /**
     * @description : failPayment - 결제 실패 처리
     * @filename : Payment
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public void failPayment() {
        this.paymentStatus = "FAILED";
    }

    /**
     * @description : cancelPayment - 결제 취소 처리
     * @filename : Payment
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public void cancelPayment() {
        this.paymentStatus = "CANCELED";
        this.canceledAt = LocalDateTime.now();
    }
}
