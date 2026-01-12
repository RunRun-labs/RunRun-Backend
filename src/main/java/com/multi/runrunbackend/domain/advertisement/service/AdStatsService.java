package com.multi.runrunbackend.domain.advertisement.service;

import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.advertisement.constant.AdDailySort;
import com.multi.runrunbackend.domain.advertisement.constant.SortDir;
import com.multi.runrunbackend.domain.advertisement.constant.StatsRange;
import com.multi.runrunbackend.domain.advertisement.dto.res.adplacement.PlacementDailyStatsItemResDto;
import com.multi.runrunbackend.domain.advertisement.dto.res.adslot.AdSlotBreakdownItemResDto;
import com.multi.runrunbackend.domain.advertisement.dto.res.adstats.AdStatsResDto;
import com.multi.runrunbackend.domain.advertisement.dto.res.adstats.AdStatsSeriesItemResDto;
import com.multi.runrunbackend.domain.advertisement.entity.Ad;
import com.multi.runrunbackend.domain.advertisement.entity.AdPlacement;
import com.multi.runrunbackend.domain.advertisement.repository.AdDailyStatsRepository;
import com.multi.runrunbackend.domain.advertisement.repository.AdPlacementRepository;
import com.multi.runrunbackend.domain.advertisement.repository.AdRepository;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdStatsService
 * @since : 2026. 1. 11. Sunday
 */
@Service
@RequiredArgsConstructor
public class AdStatsService {

    private final AdRepository adRepository;
    private final AdDailyStatsRepository dailyStatsRepository;
    private final AdPlacementRepository placementRepository;

    /**
     * Object를 long으로 안전하게 변환
     */
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
    public AdStatsResDto getAdStats(Long adId, StatsRange range) {
        StatsRange r = (range == null) ? StatsRange.D30 : range;

        Ad ad = adRepository.findById(adId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.AD_NOT_FOUND));

        LocalDate from = r.fromDate();
        LocalDate to = r.toDate();
        
        System.out.println("========== 광고 통계 조회 ==========");
        System.out.println("adId: " + adId);
        System.out.println("기간: " + from + " ~ " + to);
        System.out.println("range: " + r);

        List<Object[]> seriesRaw = dailyStatsRepository.sumAdSeries(adId, from, to);
        System.out.println("sumAdSeries 결과 개수: " + seriesRaw.size());
        if (!seriesRaw.isEmpty()) {
            System.out.println("첫 번째 series: " + Arrays.toString(seriesRaw.get(0)));
        }
        List<AdStatsSeriesItemResDto> series = seriesRaw.stream()
            .map(row -> AdStatsSeriesItemResDto.of(
                (LocalDate) row[0],
                row.length > 1 ? toLong(row[1]) : 0L,
                row.length > 2 ? toLong(row[2]) : 0L
            ))
            .toList();

        // 기간별 통계: dailySeries의 합계 계산 (sumAdTotals 대신 사용)
        long totalImp = series.stream().mapToLong(AdStatsSeriesItemResDto::getImpressions).sum();
        long totalClk = series.stream().mapToLong(AdStatsSeriesItemResDto::getClicks).sum();
        System.out.println("계산된 값 - totalImp: " + totalImp + ", totalClk: " + totalClk);

        List<Object[]> breakdownRaw = dailyStatsRepository.sumAdBySlot(adId, from, to);
        System.out.println("sumAdBySlot 결과 개수: " + breakdownRaw.size());
        if (!breakdownRaw.isEmpty()) {
            System.out.println("첫 번째 breakdown: " + Arrays.toString(breakdownRaw.get(0)));
        }
        List<AdSlotBreakdownItemResDto> breakdown = breakdownRaw.stream()
            .map(row -> AdSlotBreakdownItemResDto.of(
                (com.multi.runrunbackend.domain.advertisement.constant.AdSlotType) row[0],
                row.length > 1 ? toLong(row[1]) : 0L,
                row.length > 2 ? toLong(row[2]) : 0L
            ))
            .toList();
        
        System.out.println("==================================================");

        return AdStatsResDto.of(
            ad.getId(),
            r,
            totalImp,
            totalClk,
            series,
            breakdown
        );
    }

    @Transactional(readOnly = true)
    public List<PlacementDailyStatsItemResDto> list(
        Long placementId,
        LocalDate from,
        LocalDate to,
        AdDailySort sort,
        SortDir dir
    ) {
        AdPlacement p = placementRepository.findById(placementId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.AD_PLACEMENT_NOT_FOUND));

        LocalDate f = (from == null) ? LocalDate.now().minusDays(30) : from;
        LocalDate t = (to == null) ? LocalDate.now() : to;

        AdDailySort s = (sort == null) ? AdDailySort.DATE : sort;
        SortDir d = (dir == null) ? SortDir.DESC : dir;

        Sort.Direction direction = (d == SortDir.ASC) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortObj = switch (s) {
            case IMPRESSIONS -> Sort.by(direction, "impressions");
            case CLICKS -> Sort.by(direction, "clicks");
            case DATE -> Sort.by(direction, "statDate");
        };

        return dailyStatsRepository.findByPlacementIdAndStatDateBetween(placementId, f, t, sortObj)
            .stream()
            .map(ds -> PlacementDailyStatsItemResDto.of(ds.getStatDate(), ds.getImpressions(),
                ds.getClicks()))
            .toList();
    }

}
