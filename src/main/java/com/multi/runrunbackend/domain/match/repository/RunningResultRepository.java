package com.multi.runrunbackend.domain.match.repository;

import com.multi.runrunbackend.domain.match.constant.RunStatus;
import com.multi.runrunbackend.domain.match.entity.RunningResult;
import com.multi.runrunbackend.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 런닝 결과 Repository
 *
 * @author : chang
 * @since : 2024-12-23
 */
public interface RunningResultRepository extends JpaRepository<RunningResult, Long> {

    /* ===================== BASIC ===================== */

    /**
     * 최신 러닝 결과 (결과 모달용)
     */
    Optional<RunningResult> findTopByUserOrderByCreatedAtDesc(User user);

    /**
     * 소프트 삭제 제외 단건 조회
     */
    Optional<RunningResult> findByIdAndIsDeletedFalse(Long id);

    /**
     * 본인 러닝 결과 단건 조회 (공유 검증용)
     */
    Optional<RunningResult> findByIdAndUserIdAndIsDeletedFalse(Long id, Long userId);


    /**
     * 여러 상태의 러닝 기록 조회 (마이페이지 등)
     */
    Slice<RunningResult> findByUserAndRunStatusInAndIsDeletedFalse(
            User user,
            List<RunStatus> runStatuses,
            Pageable pageable
    );

    /* ===================== GHOST RUN ===================== */

    /**
     * 고스트런 후보 기록 조회
     */
    @Query("""
                SELECT r FROM RunningResult r
                WHERE r.course.id = :courseId
                  AND r.runStatus = :runStatus
                  AND r.isDeleted = false
                ORDER BY r.createdAt DESC
            """)
    List<RunningResult> findGhostCandidates(
            @Param("courseId") Long courseId,
            @Param("runStatus") RunStatus runStatus
    );

    /**
     * 고스트런용 완료 기록 전체 조회
     */
    @Query("""
                SELECT r FROM RunningResult r
                WHERE r.user.id = :userId
                  AND r.runStatus = 'COMPLETED'
                  AND r.isDeleted = false
                ORDER BY r.createdAt DESC
            """)
    List<RunningResult> findCompletedRecordsByUserId(@Param("userId") Long userId);

    /* ===================== FILTER / SEARCH ===================== */

    /**
     * 거리 필터 기반 솔로 러닝 조회
     */
    @Query("""
                SELECT r FROM RunningResult r
                WHERE r.user.id = :userId
                  AND r.runStatus = :runStatus
                  AND r.isDeleted = false
                  AND (:minDistance IS NULL OR r.totalDistance > :minDistance)
                  AND (:maxDistance IS NULL OR r.totalDistance <= :maxDistance)
                ORDER BY r.createdAt DESC
            """)
    Slice<RunningResult> findMySoloRecordsByDistance(
            @Param("userId") Long userId,
            @Param("runStatus") RunStatus runStatus,
            @Param("minDistance") BigDecimal minDistance,
            @Param("maxDistance") BigDecimal maxDistance,
            Pageable pageable
    );

    /**
     * 상태 + 거리 + 기간 복합 조건 조회
     */
    @Query("""
                SELECT r FROM RunningResult r
                WHERE r.user.id = :userId
                  AND r.runStatus IN :runStatuses
                  AND r.isDeleted = false
                  AND (:minDistance IS NULL OR r.totalDistance > :minDistance)
                  AND (:maxDistance IS NULL OR r.totalDistance <= :maxDistance)
                  AND (:startDate IS NULL OR r.startedAt >= :startDate)
                  AND (:endDate IS NULL OR r.startedAt <= :endDate)
            """)
    Slice<RunningResult> findMyRecordsByStatuses(
            @Param("userId") Long userId,
            @Param("runStatuses") List<RunStatus> runStatuses,
            @Param("minDistance") BigDecimal minDistance,
            @Param("maxDistance") BigDecimal maxDistance,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /* ===================== SUMMARY ===================== */

    /**
     * 오늘 러닝 요약
     */
    @Query("""
                SELECT
                    COALESCE(SUM(r.totalDistance), 0),
                    COALESCE(SUM(r.totalTime), 0)
                FROM RunningResult r
                WHERE r.user.id = :userId
                  AND r.runStatus = 'COMPLETED'
                  AND r.startedAt >= :start
                  AND r.startedAt < :end
            """)
    List<Object[]> findTodaySummary(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /**
     * 주간 러닝 요약
     */
    @Query("""
                SELECT
                    FUNCTION('date_part', 'dow', r.startedAt),
                    SUM(r.totalDistance),
                    SUM(r.totalTime)
                FROM RunningResult r
                WHERE r.user.id = :userId
                  AND r.runStatus = 'COMPLETED'
                  AND r.startedAt BETWEEN :start AND :end
                GROUP BY FUNCTION('date_part', 'dow', r.startedAt)
            """)
    List<Object[]> findWeeklySummary(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /* ===================== FEED ===================== */

    /**
     * 아직 피드에 공유되지 않은 완료 기록
     */
    @Query("""
                SELECT r FROM RunningResult r
                WHERE r.user = :user
                  AND r.runStatus = :runStatus
                  AND r.isDeleted = false
                  AND NOT EXISTS (
                      SELECT 1 FROM FeedPost fp
                      WHERE fp.runningResult.id = r.id
                        AND fp.isDeleted = false
                  )
                ORDER BY r.startedAt DESC
            """)
    Slice<RunningResult> findUnsharedCompletedRecords(
            @Param("user") User user,
            @Param("runStatus") RunStatus runStatus,
            Pageable pageable
    );

    /* ===================== STAT ===================== */

    /**
     * 평균 페이스 계산용 최근 5개 완주 기록
     */
    @Query("""
                SELECT r FROM RunningResult r
                WHERE r.user.id = :userId
                  AND r.runStatus = 'COMPLETED'
                  AND r.avgPace IS NOT NULL
                  AND r.isDeleted = false
                ORDER BY r.createdAt DESC
            """)
    List<RunningResult> findTop5ByUserIdForAverage(
            @Param("userId") Long userId,
            Pageable pageable
    );
}
