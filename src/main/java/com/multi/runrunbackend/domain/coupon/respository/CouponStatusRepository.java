package com.multi.runrunbackend.domain.coupon.respository;


import com.multi.runrunbackend.domain.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CouponStatusRepository
 * @since : 2025. 12. 29. Monday
 */
public interface CouponStatusRepository extends JpaRepository<Coupon, Long> {

    /**
     * endAt 지났으면 EXPIRED - DELETED는 건드리지 않음 - 이미 EXPIRED인 것도 제외
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE coupon
           SET status = 'EXPIRED'
         WHERE status <> 'DELETED'
           AND status <> 'EXPIRED'
           AND end_at < now()
        """, nativeQuery = true)
    int bulkExpire();

    /**
     * 수량 제한( quantity > 0 ) 쿠폰이 issuedCount >= quantity면 SOLD_OUT - 무제한(quantity=0)은 절대 SOLD_OUT 되면
     * 안 됨 - DELETED / EXPIRED 제외
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE coupon
           SET status = 'SOLD_OUT'
         WHERE status <> 'DELETED'
           AND status <> 'EXPIRED'
           AND quantity > 0
           AND issued_count >= quantity
        """, nativeQuery = true)
    int bulkSoldOut();

    /**
     * DRAFT -> ACTIVE (시작일 도달 + 아직 만료 전) - 만료(endAt < now())는 위에서 EXPIRED가 되므로 여기선 end_at >= now()
     * 조건만. - 수량 제한 쿠폰은 남아있는 경우에만 ACTIVE로 (이미 소진이면 SOLD_OUT이 우선)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE coupon
           SET status = 'ACTIVE'
         WHERE status = 'DRAFT'
           AND start_at <= now()
           AND end_at >= now()
           AND (quantity = 0 OR issued_count < quantity)
        """, nativeQuery = true)
    int bulkActivate();
}
