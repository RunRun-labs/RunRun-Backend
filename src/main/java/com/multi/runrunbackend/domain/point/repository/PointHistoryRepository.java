package com.multi.runrunbackend.domain.point.repository;

import com.multi.runrunbackend.domain.point.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * @author : BoKyung
 * @description : 포인트 변동 내역 Repository
 * @filename : PointHistoryRepository
 * @since : 2026. 01. 02. 금요일
 */
@Repository
public interface PointHistoryRepository extends JpaRepository<PointHistory, Long>, PointHistoryRepositoryCustom {

    // 오늘 적립한 포인트 합계
    @Query("SELECT COALESCE(SUM(ph.changeAmount), 0) FROM PointHistory ph " +
            "WHERE ph.user.id = :userId " +
            "AND ph.pointType = 'EARN' " +
            "AND ph.createdAt >= :startOfDay " +
            "AND ph.createdAt < :endOfDay")
    Integer getTodayEarnedPoints(
            @Param("userId") Long userId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    // 적립/사용 총합 조회
    @Query("SELECT COALESCE(SUM(ph.changeAmount), 0) FROM PointHistory ph " +
            "WHERE ph.user.id = :userId AND ph.pointType = :pointType")
    Integer getTotalPointsByType(@Param("userId") Long userId, @Param("pointType") String pointType);

}
