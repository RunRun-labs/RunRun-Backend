package com.multi.runrunbackend.domain.coupon.respository;

import com.multi.runrunbackend.domain.coupon.constant.CouponTriggerEvent;
import com.multi.runrunbackend.domain.coupon.entity.CouponRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CouponRoleRepository
 * @since : 2025. 12. 29. Monday
 */
@Repository
public interface CouponRoleRepository extends JpaRepository<CouponRole, Long> {

    boolean existsByTriggerEventAndIsActiveTrueAndConditionValueIsNull(
        CouponTriggerEvent triggerEvent);

    boolean existsByTriggerEventAndIsActiveTrueAndConditionValue(CouponTriggerEvent triggerEvent,
        Integer conditionValue);
}
