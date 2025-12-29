package com.multi.runrunbackend.domain.coupon.respository;

import com.multi.runrunbackend.domain.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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


}
