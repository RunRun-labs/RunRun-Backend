package com.multi.runrunbackend.domain.coupon.respository;

import com.multi.runrunbackend.domain.coupon.entity.CouponIssue;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CouponIssueRepository
 * @since : 2025. 12. 29. Monday
 */
public interface CouponIssueRepository extends JpaRepository<CouponIssue, Long>,
    CouponIssueRepositoryCustom {

    boolean existsByCouponIdAndUserId(Long couponId, Long userId);
    
    long countByUserId(Long userId);
}

