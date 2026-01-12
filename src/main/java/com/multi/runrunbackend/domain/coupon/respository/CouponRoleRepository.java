package com.multi.runrunbackend.domain.coupon.respository;

import com.multi.runrunbackend.domain.coupon.constant.CouponTriggerEvent;
import com.multi.runrunbackend.domain.coupon.entity.CouponRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CouponRoleRepository
 * @since : 2025. 12. 29. Monday
 */
@Repository
public interface CouponRoleRepository extends JpaRepository<CouponRole, Long>,
    JpaSpecificationExecutor<CouponRole> {

    boolean existsByTriggerEventAndIsActiveTrueAndConditionValueIsNull(
        CouponTriggerEvent triggerEvent);

    boolean existsByTriggerEventAndIsActiveTrueAndConditionValue(CouponTriggerEvent triggerEvent,
        Integer conditionValue);

    @Query("""
        select r
        from CouponRole r
        join fetch r.coupon c
        where r.isActive = true
          and r.triggerEvent = :event
          and (
                (:cond is null and r.conditionValue is null)
                or (:cond is not null and r.conditionValue = :cond)
          )
        """)
    List<CouponRole> findActiveRoles(@Param("event") CouponTriggerEvent event,
        @Param("cond") Integer cond);

    /**
     * 특정 이벤트의 모든 활성 역할 조회 (누적 거리 쿠폰 발급용)
     */
    @Query("""
        select r
        from CouponRole r
        join fetch r.coupon c
        where r.isActive = true
          and r.triggerEvent = :event
          and r.conditionValue is not null
        """)
    List<CouponRole> findAllActiveRolesByEvent(@Param("event") CouponTriggerEvent event);
}
