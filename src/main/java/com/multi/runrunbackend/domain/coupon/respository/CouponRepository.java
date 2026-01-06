package com.multi.runrunbackend.domain.coupon.respository;

import com.multi.runrunbackend.domain.coupon.entity.Coupon;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CouponRepository
 * @since : 2025. 12. 27. Saturday
 */
@Repository
public interface CouponRepository extends
    JpaRepository<Coupon, Long>, JpaSpecificationExecutor<Coupon> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Coupon c
               set c.issuedCount = c.issuedCount + 1
             where c.id = :couponId
               and (c.quantity is null or c.quantity = 0 or c.issuedCount < c.quantity)
        """)
    int increaseIssuedCount(@Param("couponId") Long couponId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Coupon c
               set c.issuedCount = c.issuedCount + 1,
                   c.status = case
                                when (c.quantity is not null and c.quantity > 0 and c.issuedCount + 1 >= c.quantity)
                                  then com.multi.runrunbackend.domain.coupon.constant.CouponStatus.SOLD_OUT
                                else c.status
                              end
             where c.id = :couponId
               and c.status = com.multi.runrunbackend.domain.coupon.constant.CouponStatus.ACTIVE
               and (c.quantity is null or c.quantity = 0 or c.issuedCount < c.quantity)
        """)
    int increaseIssuedCountAndMaybeSoldOut(@Param("couponId") Long couponId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coupon c where c.id = :id")
    Optional<Coupon> findByIdForUpdate(@Param("id") Long id);

    Optional<Coupon> findByCodeIgnoreCase(String code);
}
