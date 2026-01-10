package com.multi.runrunbackend.domain.point.repository;

import com.multi.runrunbackend.domain.point.entity.PointExpiration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author : BoKyung
 * @description : 포인트 유효기간 Repository
 * @filename : PointExpirationRepository
 * @since : 2026. 01. 02. 금요일
 */
@Repository
public interface PointExpirationRepository extends JpaRepository<PointExpiration, Long> {

    // FIFO - 가장 오래된 활성 포인트부터 조회
    @Query("SELECT pe FROM PointExpiration pe " +
            "WHERE pe.user.id = :userId " +
            "AND pe.expirationStatus = 'ACTIVE' " +
            "AND pe.remainingPoint > 0 " +
            "ORDER BY pe.earnedAt ASC")
    List<PointExpiration> findActivePointsByUserIdOrderByEarnedAt(@Param("userId") Long userId);

    // 만료 예정 포인트 조회 (만료일 순으로 정렬 )
    @Query("SELECT pe FROM PointExpiration pe " +
            "WHERE pe.user.id = :userId " +
            "AND pe.expirationStatus = 'ACTIVE' " +
            "AND pe.remainingPoint > 0 " +
            "ORDER BY pe.expiresAt ASC")
    List<PointExpiration> findActivePointsByUserIdOrderByExpiresAt(@Param("userId") Long userId);

    // 만료 처리할 포인트 조회 (스케줄러용)
    @Query("SELECT pe FROM PointExpiration pe " +
            "WHERE pe.expirationStatus = 'ACTIVE' " +
            "AND pe.expiresAt <= :now")
    List<PointExpiration> findExpiredPoints(@Param("now") LocalDateTime now);

    /**
     * @description : 만료일이 하루 후인 활성 포인트 조회 (만료 전 알림용)
     */
    @Query("SELECT pe FROM PointExpiration pe " +
            "WHERE pe.expirationStatus = 'ACTIVE' " +
            "AND pe.remainingPoint > 0 " +
            "AND pe.expiresAt BETWEEN :startDateTime AND :endDateTime")
    List<PointExpiration> findPointsExpiringTomorrow(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

}
