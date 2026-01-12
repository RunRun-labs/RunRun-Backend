package com.multi.runrunbackend.domain.admin.service;

import com.multi.runrunbackend.domain.admin.dto.res.DashboardStatsResDto;
import com.multi.runrunbackend.domain.admin.dto.res.TopAdResDto;
import com.multi.runrunbackend.domain.admin.dto.res.TopCouponResDto;
import com.multi.runrunbackend.domain.advertisement.repository.AdDailyStatsRepository;
import com.multi.runrunbackend.domain.advertisement.repository.AdPlacementRepository;
import com.multi.runrunbackend.domain.coupon.respository.CouponIssueRepository;
import com.multi.runrunbackend.domain.membership.repository.MembershipRepository;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final AdDailyStatsRepository adDailyStatsRepository;
    private final AdPlacementRepository adPlacementRepository;
    private final CouponIssueRepository couponIssueRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;

    private long toLong(Object obj) {
        if (obj == null) {
            return 0L;
        }
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    @Transactional(readOnly = true)
    public DashboardStatsResDto getDashboardStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = LocalDateTime.of(today, LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(today, LocalTime.MAX);

        log.info("========== 대시보드 통계 조회 시작 ==========");
        log.info("오늘 날짜: {}", today);
        log.info("오늘 시작: {}, 오늘 종료: {}", todayStart, todayEnd);

        // 오늘의 광고 클릭수
        Object[] todayAdStatsRaw = adDailyStatsRepository.sumTodayAdStats(today);
        log.info("sumTodayAdStats 원본 결과: {}",
            todayAdStatsRaw != null ? Arrays.toString(todayAdStatsRaw) : "null");
        
        // JPA가 배열의 배열로 반환하는 경우 처리
        Object[] todayAdStats;
        if (todayAdStatsRaw != null && todayAdStatsRaw.length > 0
            && todayAdStatsRaw[0] != null && todayAdStatsRaw[0].getClass().isArray()) {
            // 배열의 배열인 경우: 첫 번째 요소가 실제 데이터
            todayAdStats = (Object[]) todayAdStatsRaw[0];
            log.info("  ⚠️ 배열의 배열이 감지되어 첫 번째 요소를 사용합니다.");
        } else {
            // 일반 배열인 경우
            todayAdStats = todayAdStatsRaw;
        }
        
        log.info("sumTodayAdStats 처리된 결과: {}",
            todayAdStats != null ? Arrays.toString(todayAdStats) : "null");
        if (todayAdStats != null) {
            log.info("  배열 길이: {}, [0]={}, [1]={}", todayAdStats.length, todayAdStats[0],
                todayAdStats.length > 1 ? todayAdStats[1] : "없음");
        }
        long todayAdClicks =
            todayAdStats != null && todayAdStats.length > 1 ? toLong(todayAdStats[1]) : 0L;
        long todayAdImpressions =
            todayAdStats != null && todayAdStats.length > 0 ? toLong(todayAdStats[0]) : 0L;
        log.info("계산된 값 - todayAdImpressions: {}, todayAdClicks: {}", todayAdImpressions,
            todayAdClicks);

        // 활성화 광고 퍼센트
        Object[] activeAdStatsRaw = adPlacementRepository.countActivePlacements();
        log.info("countActivePlacements 원본 결과: {}",
            activeAdStatsRaw != null ? Arrays.toString(activeAdStatsRaw) : "null");
        
        // JPA가 배열의 배열로 반환하는 경우 처리
        Object[] activeAdStats;
        if (activeAdStatsRaw != null && activeAdStatsRaw.length > 0
            && activeAdStatsRaw[0] != null && activeAdStatsRaw[0].getClass().isArray()) {
            // 배열의 배열인 경우: 첫 번째 요소가 실제 데이터
            activeAdStats = (Object[]) activeAdStatsRaw[0];
            log.info("  ⚠️ 배열의 배열이 감지되어 첫 번째 요소를 사용합니다.");
        } else {
            // 일반 배열인 경우
            activeAdStats = activeAdStatsRaw;
        }
        
        log.info("countActivePlacements 처리된 결과: {}",
            activeAdStats != null ? Arrays.toString(activeAdStats) : "null");
        long activeCount =
            activeAdStats != null && activeAdStats.length > 0 ? toLong(activeAdStats[0]) : 0L;
        long totalCount =
            activeAdStats != null && activeAdStats.length > 1 ? toLong(activeAdStats[1]) : 0L;
        double activeAdPercentage = totalCount > 0 ? (activeCount * 100.0) / totalCount : 0.0;
        log.info("계산된 값 - activeCount: {}, totalCount: {}, activeAdPercentage: {}%", activeCount,
            totalCount, activeAdPercentage);

        // 새로운 멤버십 회원 (오늘 생성된 Membership)
        long newMembershipUsers = membershipRepository.countByCreatedAtBetween(todayStart,
            todayEnd);
        log.info("newMembershipUsers: {}", newMembershipUsers);

        // 오늘의 쿠폰 발급 수
        Object[] todayCouponStatsRaw = couponIssueRepository.sumTodayCouponStats(today);
        log.info("sumTodayCouponStats 원본 결과: {}",
            todayCouponStatsRaw != null ? Arrays.toString(todayCouponStatsRaw) : "null");
        
        // JPA가 배열의 배열로 반환하는 경우 처리
        Object[] todayCouponStats;
        if (todayCouponStatsRaw != null && todayCouponStatsRaw.length > 0
            && todayCouponStatsRaw[0] != null && todayCouponStatsRaw[0].getClass().isArray()) {
            // 배열의 배열인 경우: 첫 번째 요소가 실제 데이터
            todayCouponStats = (Object[]) todayCouponStatsRaw[0];
            log.info("  ⚠️ 배열의 배열이 감지되어 첫 번째 요소를 사용합니다.");
        } else {
            // 일반 배열인 경우
            todayCouponStats = todayCouponStatsRaw;
        }
        
        log.info("sumTodayCouponStats 처리된 결과: {}",
            todayCouponStats != null ? Arrays.toString(todayCouponStats) : "null");
        if (todayCouponStats != null) {
            log.info("  배열 길이: {}, [0]={}, [1]={}", todayCouponStats.length, todayCouponStats[0],
                todayCouponStats.length > 1 ? todayCouponStats[1] : "없음");
        }
        long todayCouponIssued =
            todayCouponStats != null && todayCouponStats.length > 0 ? toLong(todayCouponStats[0])
                : 0L;
        long todayCouponUsed =
            todayCouponStats != null && todayCouponStats.length > 1 ? toLong(todayCouponStats[1])
                : 0L;
        double todayCouponUsageRate =
            todayCouponIssued > 0 ? (todayCouponUsed * 100.0) / todayCouponIssued : 0.0;
        log.info("계산된 값 - todayCouponIssued: {}, todayCouponUsed: {}, todayCouponUsageRate: {}%",
            todayCouponIssued, todayCouponUsed, todayCouponUsageRate);

        // 새로운 회원 (오늘 생성된 User)
        long newUsers = userRepository.countByCreatedAtBetween(todayStart, todayEnd);
        log.info("newUsers: {}", newUsers);

        // Top 5 광고 (오늘 클릭수 기준)
        List<Object[]> topAdsRaw = adDailyStatsRepository.findTop5AdsByClicks(today);
        log.info("findTop5AdsByClicks 결과 개수: {}", topAdsRaw.size());
        if (!topAdsRaw.isEmpty()) {
            log.info("  첫 번째 광고: {}", Arrays.toString(topAdsRaw.get(0)));
        }
        List<TopAdResDto> topAds = topAdsRaw.stream()
            .map(row -> TopAdResDto.of(
                row.length > 0 ? toLong(row[0]) : 0L,
                row.length > 1 ? (String) row[1] : "-",
                row.length > 2 ? toLong(row[2]) : 0L
            ))
            .toList();

        // Top 5 쿠폰 (오늘 발급 수 기준)
        List<Object[]> topCouponsRaw = couponIssueRepository.findTop5CouponsByIssued(today);
        log.info("findTop5CouponsByIssued 결과 개수: {}", topCouponsRaw.size());
        if (!topCouponsRaw.isEmpty()) {
            log.info("  첫 번째 쿠폰: {}", Arrays.toString(topCouponsRaw.get(0)));
        }
        List<TopCouponResDto> topCoupons = topCouponsRaw.stream()
            .map(row -> TopCouponResDto.of(
                row.length > 0 ? toLong(row[0]) : 0L,
                row.length > 1 ? (String) row[1] : "-",
                row.length > 2 ? toLong(row[2]) : 0L
            ))
            .toList();

        log.info("==================================================");

        return DashboardStatsResDto.of(
            todayAdClicks,
            activeAdPercentage,
            todayAdImpressions,
            newMembershipUsers,
            todayCouponIssued,
            todayCouponUsageRate,
            todayCouponUsed,
            newUsers,
            topAds,
            topCoupons
        );
    }
}