package com.multi.runrunbackend.domain.advertisement.repository;

import com.multi.runrunbackend.domain.advertisement.constant.AdSlotType;
import com.multi.runrunbackend.domain.advertisement.dto.req.adplacement.AdPlacementAdminListItemProjection;
import com.multi.runrunbackend.domain.advertisement.entity.AdPlacement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdPlacementRepository
 * @since : 2026. 1. 11. Sunday
 */
@Repository
public interface AdPlacementRepository extends JpaRepository<AdPlacement, Long>,
    JpaSpecificationExecutor<AdPlacement> {

    Page<AdPlacement> findByIsActive(boolean isActive, Pageable pageable);

    boolean existsByAdId(Long adId);

    boolean existsBySlotId(Long slotId);

    // =========================
    // Admin List (Projection)
    // =========================
    @Query("""
        select
            p.id as placementId,
        
            s.id as slotId,
            s.name as slotName,
            s.slotType as slotType,
        
            a.id as adId,
            a.name as adName,
            p.weight as weight,
        
            p.startAt as startAt,
            p.endAt as endAt,
        
            p.isActive as isActive,
        
            p.totalImpressions as totalImpressions,
            p.totalClicks as totalClicks
        from AdPlacement p
        join p.slot s
        join p.ad a
        where a.isDeleted = false
          and (:isActive is null or p.isActive = :isActive)
          and (:slotType is null or s.slotType = :slotType)
          and (
                :keyword is null
             or a.name like concat('%', :keyword, '%')
             or s.name like concat('%', :keyword, '%')
          )
        """)
    Page<AdPlacementAdminListItemProjection> searchAdminList(
        @Param("isActive") Boolean isActive,
        @Param("slotType") AdSlotType slotType,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    @Query("""
            select p
            from AdPlacement p
            join fetch p.ad a
            join fetch p.slot s
            where s.slotType = :slotType
              and s.status = com.multi.runrunbackend.domain.advertisement.constant.AdSlotStatus.ENABLED
              and p.isActive = true
              and :now >= p.startAt and :now <= p.endAt
              and a.isDeleted = false
        """)
    List<AdPlacement> findServeCandidates(AdSlotType slotType, LocalDateTime now);

    @Modifying
    @Query("""
            update AdPlacement p
            set p.totalImpressions = p.totalImpressions + 1
            where p.id = :placementId
        """)
    int increaseTotalImpression(Long placementId);

    @Modifying
    @Query("""
            update AdPlacement p
            set p.totalClicks = p.totalClicks + 1
            where p.id = :placementId
        """)
    int increaseTotalClick(Long placementId);

    @Modifying
    @Query("""
            update AdPlacement p
            set p.isActive = false
            where p.isActive = true and p.endAt < :now
        """)
    int disableExpired(LocalDateTime now);
    
    // 활성화/비활성화 광고 배치 카운트
    @Query(value = """
        SELECT 
            COUNT(CASE WHEN is_active = true THEN 1 END) as active_count,
            COUNT(*) as total_count
        FROM ad_placement p
        JOIN ad a ON p.ad_id = a.id
        WHERE a.is_deleted = false
        """, nativeQuery = true)
    Object[] countActivePlacements();

    @Query("""
            select p
            from AdPlacement p
            join fetch p.slot
            join fetch p.ad
            where p.id = :placementId
        """)
    Optional<AdPlacement> findDetailById(Long placementId);

    // 광고의 모든 placement 총합 (Ad 삭제 여부 확인)
    @Query("""
            select 
                count(p.id),
                coalesce(sum(p.totalImpressions), 0),
                coalesce(sum(p.totalClicks), 0)
            from AdPlacement p
            where p.ad.id = :adId
              and p.ad.isDeleted = false
        """)
    Object[] sumPlacementTotalsByAdId(@Param("adId") Long adId);
}