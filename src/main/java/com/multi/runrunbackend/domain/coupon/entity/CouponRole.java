package com.multi.runrunbackend.domain.coupon.entity;

import com.multi.runrunbackend.common.entitiy.BaseEntity;
import com.multi.runrunbackend.domain.coupon.constant.CouponTriggerEvent;
import com.multi.runrunbackend.domain.coupon.dto.req.CouponRoleCreateReqDto;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : kyungsoo
 * @description : 쿠폰 자동 발급 조건을 정의하는 엔티티. 특정 이벤트 발생 시 쿠폰을 자동으로 발급하기 위한 규칙을 관리한다. 트리거
 * 이벤트(CouponTriggerEvent)와 조건값(conditionValue)을 통해 러닝 횟수, 거리 달성 등 다양한 자동 발급 시나리오를 지원한다.
 * @filename : CouponRole
 * @since : 2025. 12. 17. Wednesday
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "coupon_role")
public class CouponRole extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_event", nullable = false)
    private CouponTriggerEvent triggerEvent;

    private Integer conditionValue;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @PrePersist
    public void prePersist() {
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    public static CouponRole create(CouponRoleCreateReqDto req, Coupon coupon) {
        CouponRole role = new CouponRole();
        role.name = req.getName();
        role.coupon = coupon;
        role.triggerEvent = req.getTriggerEvent();
        role.conditionValue = req.getConditionValue();

        return role;
    }
}
