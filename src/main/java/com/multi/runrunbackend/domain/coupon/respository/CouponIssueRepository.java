package com.multi.runrunbackend.domain.coupon.respository;

import com.multi.runrunbackend.domain.coupon.entity.Coupon;
import com.multi.runrunbackend.domain.coupon.entity.CouponIssue;
import com.multi.runrunbackend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

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

    Optional<CouponIssue> findByCouponAndUser(Coupon coupon, User user);
}

