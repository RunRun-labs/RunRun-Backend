package com.multi.runrunbackend.domain.point.repository;

import com.multi.runrunbackend.domain.point.entity.UserPoint;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author : BoKyung
 * @description : 사용자 포인트 Repository
 * @filename : UserPointRepository
 * @since : 2026. 01. 02. 금요일
 */
@Repository
public interface UserPointRepository extends JpaRepository<UserPoint, Long> {
    Optional<UserPoint> findByUserId(Long userId);

    // 동시성 제어 - 비관적 락 (포인트 계산 오류 방지용)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT up FROM UserPoint up WHERE up.user.id = :userId")
    Optional<UserPoint> findByUserIdWithLock(@Param("userId") Long userId);

    @Query("SELECT COALESCE(up.totalPoint, 0) FROM UserPoint up WHERE up.user.id = :userId")
    Integer getTotalPointByUserId(@Param("userId") Long userId);
}
