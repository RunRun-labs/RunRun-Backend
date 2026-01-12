package com.multi.runrunbackend.domain.coupon.service;

import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.advertisement.constant.StatsRange;
import com.multi.runrunbackend.domain.coupon.dto.res.CouponStatsResDto;
import com.multi.runrunbackend.domain.coupon.dto.res.CouponStatsSeriesItemResDto;
import com.multi.runrunbackend.domain.coupon.dto.res.CouponStatusBreakdownResDto;
import com.multi.runrunbackend.domain.coupon.entity.Coupon;
import com.multi.runrunbackend.domain.coupon.respository.CouponIssueRepository;
import com.multi.runrunbackend.domain.coupon.respository.CouponRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponStatsService {
    
    private final CouponRepository couponRepository;
    private final CouponIssueRepository couponIssueRepository;
    
    private long toLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
    
    @Transactional(readOnly = true)
    public CouponStatsResDto getCouponStats(Long couponId, StatsRange range) {
        StatsRange r = (range == null) ? StatsRange.D30 : range;
        
        Coupon coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.COUPON_NOT_FOUND));
        
        LocalDate from = r.fromDate();
        LocalDate to = r.toDate();
        
        // 기간별 총합
        Object[] totalsRaw = couponIssueRepository.sumTotalsByCouponId(couponId, from, to);
        long totalIssued = totalsRaw != null && totalsRaw.length > 0 ? toLong(totalsRaw[0]) : 0L;
        long totalUsed = totalsRaw != null && totalsRaw.length > 1 ? toLong(totalsRaw[1]) : 0L;
        long totalExpired = totalsRaw != null && totalsRaw.length > 2 ? toLong(totalsRaw[2]) : 0L;
        long totalAvailable = totalsRaw != null && totalsRaw.length > 3 ? toLong(totalsRaw[3]) : 0L;
        
        // 날짜별 추이
        List<Object[]> trendRaw = couponIssueRepository.sumDailyTrendByCouponId(couponId, from, to);
        List<CouponStatsSeriesItemResDto> dailyTrend = trendRaw.stream()
            .map(row -> CouponStatsSeriesItemResDto.of(
                (LocalDate) row[0],
                row.length > 1 ? toLong(row[1]) : 0L,
                row.length > 2 ? toLong(row[2]) : 0L,
                row.length > 3 ? toLong(row[3]) : 0L
            ))
            .toList();
        
        // 상태별 분포 (전체 기간)
        Object[] statusRaw = couponIssueRepository.sumStatusBreakdownByCouponId(couponId);
        CouponStatusBreakdownResDto statusBreakdown = CouponStatusBreakdownResDto.of(
            statusRaw != null && statusRaw.length > 0 ? toLong(statusRaw[0]) : 0L,
            statusRaw != null && statusRaw.length > 1 ? toLong(statusRaw[1]) : 0L,
            statusRaw != null && statusRaw.length > 2 ? toLong(statusRaw[2]) : 0L
        );
        
        return CouponStatsResDto.of(
            coupon.getId(),
            r,
            totalIssued,
            totalUsed,
            totalExpired,
            totalAvailable,
            dailyTrend,
            statusBreakdown
        );
    }
}

