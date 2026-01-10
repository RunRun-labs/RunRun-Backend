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

    @Query("SELECT r FROM RunningResult r WHERE r.course.id = :courseId AND r.runStatus = :runStatus AND r.isDeleted = false ORDER BY r.createdAt DESC")
    List<RunningResult> findGhostCandidates(@Param("courseId") Long courseId,
                                            @Param("runStatus") RunStatus runStatus);

    /**
     * 여러 상태의 러닝 기록 조회 (마이페이지 등)
     */
    Slice<RunningResult> findByUserAndRunStatusInAndIsDeletedFalse(
            User user,
            List<RunStatus> runStatuses,
            Pageable pageable
    );





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

    @Query("SELECT r FROM RunningResult r " +
            "WHERE r.user.id = :userId " +
            "AND r.runStatus IN :runStatuses " +
            "AND r.isDeleted = false " +
            "AND (:minDistance IS NULL OR r.totalDistance > :minDistance) " +
            "AND (:maxDistance IS NULL OR r.totalDistance <= :maxDistance) " +
            "AND (CAST(:startDate AS timestamp) IS NULL OR r.startedAt >= :startDate) " +
            "AND (CAST(:endDate AS timestamp) IS NULL OR r.startedAt <= :endDate)"
    )
    Slice<RunningResult> findMyRecordsByStatuses(
            @Param("userId") Long userId,
            @Param("runStatuses") List<RunStatus> runStatuses, // List로 변경
            @Param("minDistance") BigDecimal minDistance,
            @Param("maxDistance") BigDecimal maxDistance,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * 고스트런용: 사용자의 완료된 기록 조회
     *
     * @param userId 사용자 ID
     * @return 완료된 런닝 기록 리스트
     */
    @Query("SELECT r FROM RunningResult r " +
            "WHERE r.user.id = :userId " +
            "AND r.runStatus = 'COMPLETED' " +
            "AND r.isDeleted = false " +
            "ORDER BY r.createdAt DESC")
    List<RunningResult> findCompletedRecordsByUserId(@Param("userId") Long userId);

    /* ===================== SUMMARY ===================== */

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
            Long userId,
            LocalDateTime start,
            LocalDateTime end
    );

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
            Long userId,
            LocalDateTime start,
            LocalDateTime end
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


    /**
     * 사용자의 완료된 러닝 기록 조회
     */
    Slice<RunningResult> findCompletedByUser(
            User user,
            Pageable pageable
    );

    /**
     * 러닝 기록 조회 (상태 지정 가능) - 마이페이지 등에서 사용
     */
    @Query("SELECT r FROM RunningResult r WHERE r.user = :user AND r.runStatus IN :statuses AND r.isDeleted = false")
    Slice<RunningResult> findByUserAndRunStatusIn(
            @Param("user") User user,
            @Param("statuses") List<RunStatus> statuses,
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
