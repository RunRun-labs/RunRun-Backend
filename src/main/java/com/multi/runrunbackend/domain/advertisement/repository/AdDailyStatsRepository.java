package com.multi.runrunbackend.domain.advertisement.repository;

import com.multi.runrunbackend.domain.advertisement.entity.AdDailyStats;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdDailyStatsRepository
 * @since : 2026. 1. 11. Sunday
 */
@Repository
public interface AdDailyStatsRepository extends JpaRepository<AdDailyStats, Long> {

    Page<AdDailyStats> findByPlacement_Id(Long placementId, Pageable pageable);

    // placement 상세에서 목록(날짜 범위) - 페이징 없음
    List<AdDailyStats> findByPlacementIdAndStatDateBetween(Long placementId, LocalDate from,
        LocalDate to, Sort sort);

    // placement 상세에서 목록(날짜 범위) - 페이징 지원
    Page<AdDailyStats> findByPlacementIdAndStatDateBetween(Long placementId, LocalDate from,
        LocalDate to, Pageable pageable);

    // 광고 통계: 날짜별 시리즈 (실제 노출 가능한 기간만)
    @Query("""
            select ds.statDate, coalesce(sum(ds.impressions), 0), coalesce(sum(ds.clicks), 0)
            from AdDailyStats ds
            join ds.placement p
            join p.ad a
            where a.id = :adId
              and ds.statDate between :from and :to
              and cast(p.startAt as date) <= ds.statDate
              and a.isDeleted = false
            group by ds.statDate
            order by ds.statDate asc
        """)
    List<Object[]> sumAdSeries(Long adId, LocalDate from, LocalDate to);

    // 광고 통계: 슬롯별 breakdown (실제 노출 가능한 기간만)
    @Query("""
            select s.slotType, coalesce(sum(ds.impressions), 0), coalesce(sum(ds.clicks), 0)
            from AdDailyStats ds
            join ds.placement p
            join p.slot s
            join p.ad a
            where a.id = :adId
              and ds.statDate between :from and :to
              and cast(p.startAt as date) <= ds.statDate
              and a.isDeleted = false
            group by s.slotType
            order by sum(ds.impressions) desc
        """)
    List<Object[]> sumAdBySlot(Long adId, LocalDate from, LocalDate to);

    // 광고 통계: 기간 합계 (실제 노출 가능한 기간만, COALESCE 추가)
    @Query("""
            select 
                coalesce(sum(ds.impressions), 0),
                coalesce(sum(ds.clicks), 0)
            from AdDailyStats ds
            join ds.placement p
            join p.ad a
            where a.id = :adId
              and ds.statDate between :from and :to
              and cast(p.startAt as date) <= ds.statDate
              and a.isDeleted = false
        """)
    Object[] sumAdTotals(Long adId, LocalDate from, LocalDate to);

    // daily upsert (PostgreSQL)
    @Modifying
    @Query(value = """
            INSERT INTO ad_daily_stats (stat_date, placement_id, impressions, clicks, created_at, updated_at)
            VALUES (:statDate, :placementId,
                    CASE WHEN :isImpression = 1 THEN 1 ELSE 0 END,
                    CASE WHEN :isClick = 1 THEN 1 ELSE 0 END,
                    NOW(), NOW())
            ON CONFLICT (stat_date, placement_id)
            DO UPDATE SET
                impressions = ad_daily_stats.impressions + CASE WHEN :isImpression = 1 THEN 1 ELSE 0 END,
                clicks = ad_daily_stats.clicks + CASE WHEN :isClick = 1 THEN 1 ELSE 0 END,
                updated_at = NOW()
        """, nativeQuery = true)
    int upsertDaily(
        @Param("placementId") Long placementId,
        @Param("statDate") LocalDate statDate,
        @Param("isImpression") int isImpression,
        @Param("isClick") int isClick);
}
