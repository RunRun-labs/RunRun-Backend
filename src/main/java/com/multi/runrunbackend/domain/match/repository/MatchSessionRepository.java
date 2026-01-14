package com.multi.runrunbackend.domain.match.repository;

import com.multi.runrunbackend.domain.match.constant.SessionStatus;
import com.multi.runrunbackend.domain.match.entity.MatchSession;
import com.multi.runrunbackend.domain.recruit.entity.Recruit;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * @author : KIMGWANGHO
 * @description : 매치 세션(MatchSession) 엔티티의 데이터베이스 접근 및 관리를 담당하는 리포지토리
 * @filename : MatchSessionRepository
 * @since : 2025-12-21 일요일
 */
public interface MatchSessionRepository extends JpaRepository<MatchSession, Long> {

    boolean existsByRecruit(Recruit recruit);

    Optional<MatchSession> findByRecruit(Recruit recruit);

    @Modifying
    @Query("UPDATE MatchSession ms SET ms.status = :status WHERE ms.id = :sessionId")
    int updateStatus(@Param("sessionId") Long sessionId, @Param("status") SessionStatus status);

    @Query("SELECT ms FROM MatchSession ms LEFT JOIN FETCH ms.runningResult WHERE ms.id = :sessionId")
    Optional<MatchSession> findByIdWithRunningResult(@Param("sessionId") Long sessionId);

    @Query("""
        SELECT AVG(CAST(ms.duration AS DOUBLE))
        FROM MatchSession ms
        WHERE ms.isDeleted = false
          AND ms.status != 'CANCELLED'
          AND ms.duration > 0
        """)
    Double calculateAvgMatchDuration();
}
