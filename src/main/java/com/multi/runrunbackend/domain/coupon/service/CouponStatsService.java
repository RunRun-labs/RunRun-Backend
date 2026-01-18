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
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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
        
        log.info("========== 쿠폰 통계 조회 ==========");
        log.info("couponId: {}", couponId);
        log.info("기간: {} ~ {}", from, to);
        
        // 기간별 총합
        Object[] totalsRaw = couponIssueRepository.sumTotalsByCouponId(couponId, from, to);
        log.info("sumTotalsByCouponId 원본 결과: {}", 
            totalsRaw != null ? Arrays.toString(totalsRaw) : "null");
        
        // JPA가 배열의 배열로 반환하는 경우 처리
        Object[] totals;
        if (totalsRaw != null && totalsRaw.length > 0 
            && totalsRaw[0] != null && totalsRaw[0].getClass().isArray()) {
            // 배열의 배열인 경우: 첫 번째 요소가 실제 데이터
            totals = (Object[]) totalsRaw[0];
            log.info("  ⚠️ 배열의 배열이 감지되어 첫 번째 요소를 사용합니다.");
        } else {
            // 일반 배열인 경우
            totals = totalsRaw;
        }
        
        log.info("sumTotalsByCouponId 처리된 결과: {}", 
            totals != null ? Arrays.toString(totals) : "null");
        if (totals != null) {
            log.info("  배열 길이: {}, [0]={}, [1]={}, [2]={}, [3]={}", 
                totals.length, 
                totals.length > 0 ? totals[0] : "없음",
                totals.length > 1 ? totals[1] : "없음",
                totals.length > 2 ? totals[2] : "없음",
                totals.length > 3 ? totals[3] : "없음");
        }
        
        long totalIssued = totals != null && totals.length > 0 ? toLong(totals[0]) : 0L;
        long totalUsed = totals != null && totals.length > 1 ? toLong(totals[1]) : 0L;
        long totalExpired = totals != null && totals.length > 2 ? toLong(totals[2]) : 0L;
        long totalAvailable = totals != null && totals.length > 3 ? toLong(totals[3]) : 0L;
        
        log.info("계산된 값 - totalIssued: {}, totalUsed: {}, totalExpired: {}, totalAvailable: {}", 
            totalIssued, totalUsed, totalExpired, totalAvailable);
        
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
        log.info("sumStatusBreakdownByCouponId 원본 결과: {}", 
            statusRaw != null ? Arrays.toString(statusRaw) : "null");
        
        // JPA가 배열의 배열로 반환하는 경우 처리
        Object[] status;
        if (statusRaw != null && statusRaw.length > 0 
            && statusRaw[0] != null && statusRaw[0].getClass().isArray()) {
            // 배열의 배열인 경우: 첫 번째 요소가 실제 데이터
            status = (Object[]) statusRaw[0];
            log.info("  ⚠️ 배열의 배열이 감지되어 첫 번째 요소를 사용합니다.");
        } else {
            // 일반 배열인 경우
            status = statusRaw;
        }
        
        log.info("sumStatusBreakdownByCouponId 처리된 결과: {}", 
            status != null ? Arrays.toString(status) : "null");
        
        CouponStatusBreakdownResDto statusBreakdown = CouponStatusBreakdownResDto.of(
            status != null && status.length > 0 ? toLong(status[0]) : 0L,
            status != null && status.length > 1 ? toLong(status[1]) : 0L,
            status != null && status.length > 2 ? toLong(status[2]) : 0L
        );
        
        log.info("상태별 분포 - available: {}, used: {}, expired: {}", 
            statusBreakdown.getAvailable(), statusBreakdown.getUsed(), statusBreakdown.getExpired());
        
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

