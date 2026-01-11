package com.multi.runrunbackend.domain.coupon.respository;

import com.multi.runrunbackend.domain.coupon.entity.Coupon;
import com.multi.runrunbackend.domain.coupon.entity.CouponIssue;
import com.multi.runrunbackend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
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
    
    // 날짜별 발급/사용/만료 집계 (발급 날짜 기준)
    @Query(value = """
        SELECT 
            cast(created_at as date) as stat_date,
            COUNT(*) as issued_count,
            COUNT(CASE WHEN status = 'USED' AND used_at IS NOT NULL 
                AND cast(used_at as date) = cast(created_at as date) THEN 1 END) as used_count,
            COUNT(CASE WHEN status = 'EXPIRED' AND expiry_at IS NOT NULL 
                AND cast(expiry_at as date) = cast(created_at as date) THEN 1 END) as expired_count
        FROM coupon_issue
        WHERE coupon_id = :couponId
          AND cast(created_at as date) BETWEEN :from AND :to
        GROUP BY cast(created_at as date)
        ORDER BY stat_date ASC
        """, nativeQuery = true)
    List<Object[]> sumDailyTrendByCouponId(
        @Param("couponId") Long couponId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );
    
    // 상태별 분포 (전체)
    @Query(value = """
        SELECT 
            COUNT(CASE WHEN status = 'AVAILABLE' THEN 1 END) as available_count,
            COUNT(CASE WHEN status = 'USED' THEN 1 END) as used_count,
            COUNT(CASE WHEN status = 'EXPIRED' THEN 1 END) as expired_count
        FROM coupon_issue
        WHERE coupon_id = :couponId
        """, nativeQuery = true)
    Object[] sumStatusBreakdownByCouponId(@Param("couponId") Long couponId);
    
    // 기간별 총합
    @Query(value = """
        SELECT 
            COUNT(*) as total_issued,
            COUNT(CASE WHEN status = 'USED' THEN 1 END) as total_used,
            COUNT(CASE WHEN status = 'EXPIRED' THEN 1 END) as total_expired,
            COUNT(CASE WHEN status = 'AVAILABLE' THEN 1 END) as total_available
        FROM coupon_issue
        WHERE coupon_id = :couponId
          AND cast(created_at as date) BETWEEN :from AND :to
        """, nativeQuery = true)
    Object[] sumTotalsByCouponId(
        @Param("couponId") Long couponId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );
    
    // 오늘의 쿠폰 통계 (발급 수, 사용 수)
    @Query(value = """
        SELECT 
            COUNT(*) as total_issued,
            COUNT(CASE WHEN status = 'USED' AND cast(used_at as date) = :today THEN 1 END) as total_used
        FROM coupon_issue
        WHERE cast(created_at as date) = :today
        """, nativeQuery = true)
    Object[] sumTodayCouponStats(@Param("today") LocalDate today);
    
    // Top 5 쿠폰 (오늘 발급 수 기준)
    @Query(value = """
        SELECT 
            c.id as coupon_id,
            c.name as coupon_name,
            COUNT(ci.id) as issued_count
        FROM coupon_issue ci
        JOIN coupon c ON ci.coupon_id = c.id
        WHERE cast(ci.created_at as date) = :today
          AND c.status <> 'DELETED'
        GROUP BY c.id, c.name
        ORDER BY issued_count DESC
        LIMIT 5
        """, nativeQuery = true)
    List<Object[]> findTop5CouponsByIssued(@Param("today") LocalDate today);
}

