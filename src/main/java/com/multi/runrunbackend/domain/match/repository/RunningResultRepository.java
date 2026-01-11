package com.multi.runrunbackend.domain.match.repository;

import com.multi.runrunbackend.domain.match.constant.RunStatus;
import com.multi.runrunbackend.domain.match.entity.RunningResult;
import com.multi.runrunbackend.domain.user.entity.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


/**
 * 런닝 결과 Repository
 *
 * @author : chang
 * @since : 2024-12-23
 */
public interface RunningResultRepository extends JpaRepository<RunningResult, Long> {

//  Optional<RunningResult> findTopByUserOrderByCreatedAtDesc(User user);

  Optional<RunningResult> findByIdAndIsDeletedFalse(Long id);

  Optional<RunningResult> findByIdAndUserIdAndIsDeletedFalse(Long id, Long userId);

  Slice<RunningResult> findByUserAndRunStatusAndIsDeletedFalse(
      User user,
      RunStatus runStatus,
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
  List<Object[]> findTodaySummary(Long userId, LocalDateTime start, LocalDateTime end);

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
  List<Object[]> findWeeklySummary(Long userId, LocalDateTime start, LocalDateTime end);

  /* ===================== FEED ===================== */

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
      User user,
      RunStatus runStatus,
      Pageable pageable
  );


  @Query("""
          SELECT r FROM RunningResult r
          WHERE r.user.id = :userId
            AND r.runStatus = 'COMPLETED'
            AND r.avgPace IS NOT NULL
            AND r.isDeleted = false
          ORDER BY r.createdAt DESC
      """)
  List<RunningResult> findTop5ByUserIdForAverage(@Param("userId") Long userId, Pageable pageable);
}