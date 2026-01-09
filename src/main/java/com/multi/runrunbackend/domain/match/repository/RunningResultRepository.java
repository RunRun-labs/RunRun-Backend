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

  /**
   * 사용자의 최신 런닝 결과 조회 - 런닝 종료 후 결과 모달 표시용
   *
   * @param user 사용자 엔티티
   * @return 최신 런닝 결과
   */
  Optional<RunningResult> findTopByUserOrderByCreatedAtDesc(User user);


  @Query("SELECT r FROM RunningResult r WHERE r.course.id = :courseId AND r.runStatus = :runStatus AND r.isDeleted = false ORDER BY r.createdAt DESC")
  List<RunningResult> findGhostCandidates(@Param("courseId") Long courseId,
      @Param("runStatus") RunStatus runStatus);


  @Query("SELECT r FROM RunningResult r " +
      "WHERE r.user.id = :userId " +
      "AND r.runStatus = :runStatus " +
      "AND r.isDeleted = false " +
      "AND (:minDistance IS NULL OR r.totalDistance > :minDistance) " +
      "AND (:maxDistance IS NULL OR r.totalDistance <= :maxDistance) " +
      "ORDER BY r.createdAt DESC")
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

  /*
   * 오늘/주간 러닝 요약 정보 조회
   *   */

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

  /**
   * 사용자의 완료된 러닝 기록 조회
   */
  Slice<RunningResult> findCompletedByUser(
      User user,
      Pageable pageable
  );

  /**
   * 평균 페이스 계산을 위한 최근 5개 완주 기록 조회
   * - 완주한 기록만 (COMPLETED)
   * - avgPace가 null이 아닌 기록만
   * - 삭제되지 않은 기록만
   * - 최신순 정렬
   *
   * @param userId 사용자 ID
   * @param pageable 페이지 정보 (size=5)
   * @return 최근 5개 완주 기록
   */
  @Query("SELECT r FROM RunningResult r " +
      "WHERE r.user.id = :userId " +
      "AND r.runStatus = 'COMPLETED' " +
      "AND r.avgPace IS NOT NULL " +
      "AND r.isDeleted = false " +
      "ORDER BY r.createdAt DESC")
  List<RunningResult> findTop5ByUserIdForAverage(
      @Param("userId") Long userId,
      Pageable pageable
  );

}
