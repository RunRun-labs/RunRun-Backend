package com.multi.runrunbackend.domain.match.repository;

import com.multi.runrunbackend.domain.match.entity.RunningResult;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * @author : kyungsoo
 * @description : 러닝 통계 조회용 Repository
 * @filename : RunningStatsRepository
 * @since : 2026. 1. 15. Wednesday
 */
public interface RunningStatsRepository extends JpaRepository<RunningResult, Long> {

    /**
     * 매칭 성공률, 취소율, 이탈률 계산
     */
    @Query("""
        SELECT 
            COUNT(CASE WHEN r.runStatus IN ('COMPLETED', 'TIME_OUT') THEN 1 END) as successCount,
            COUNT(CASE WHEN r.runStatus = 'CANCELLED' THEN 1 END) as cancelCount,
            COUNT(CASE WHEN r.runStatus = 'GIVE_UP' THEN 1 END) as dropoutCount,
            COUNT(*) as totalCount
        FROM RunningResult r
        WHERE r.isDeleted = false
        """)
    List<Object[]> calculateMatchRates();

    /**
     * 러닝 평균 거리
     */
    @Query("""
        SELECT AVG(r.totalDistance)
        FROM RunningResult r
        WHERE r.isDeleted = false
          AND r.runStatus IN ('COMPLETED', 'TIME_OUT')
        """)
    BigDecimal calculateAvgRunningDistance();

    /**
     * 총 뛴 거리 수
     */
    @Query("""
        SELECT COALESCE(SUM(r.totalDistance), 0)
        FROM RunningResult r
        WHERE r.isDeleted = false
          AND r.runStatus IN ('COMPLETED', 'TIME_OUT')
        """)
    BigDecimal calculateTotalRunningDistance();

    /**
     * 총 러닝 횟수
     */
    @Query("""
        SELECT COUNT(*)
        FROM RunningResult r
        WHERE r.isDeleted = false
          AND r.runStatus IN ('COMPLETED', 'TIME_OUT')
        """)
    long countTotalRunning();

    /**
     * 총 시간 (초)
     */
    @Query("""
        SELECT COALESCE(SUM(r.totalTime), 0)
        FROM RunningResult r
        WHERE r.isDeleted = false
          AND r.runStatus IN ('COMPLETED', 'TIME_OUT')
        """)
    long calculateTotalRunningTime();

    /**
     * 연속 러닝일 (최대)
     */
    @Query(value = """
        WITH daily_runs AS (
            SELECT DISTINCT CAST(r.started_at AS DATE) as run_date
            FROM running_result r
            WHERE r.is_deleted = false
              AND r.run_status IN ('COMPLETED', 'TIME_OUT')
            ORDER BY run_date DESC
        ),
        consecutive_groups AS (
            SELECT 
                run_date,
                run_date - ROW_NUMBER() OVER (ORDER BY run_date) * INTERVAL '1 day' as grp
            FROM daily_runs
        )
        SELECT MAX(cnt) as max_consecutive
        FROM (
            SELECT COUNT(*) as cnt
            FROM consecutive_groups
            GROUP BY grp
        ) sub
        """, nativeQuery = true)
    Long findMaxConsecutiveDays();

    /**
     * 티어별 러닝량 (BattleResult의 currentRating 기준) Note: Tier enum은 한글 이름을 사용하므로 결과는 문자열로 반환됨
     */
    @Query(value = """
        SELECT tier_name, total_count, total_distance
        FROM (
            SELECT
                CASE
                    WHEN br.current_rating < 800 THEN '거북이'
                    WHEN br.current_rating < 1200 THEN '토끼'
                    WHEN br.current_rating < 1600 THEN '사슴'
                    WHEN br.current_rating < 2000 THEN '표범'
                    WHEN br.current_rating < 2400 THEN '호랑이'
                    ELSE '장산범'
                END as tier_name,
                COUNT(*) as total_count,
                COALESCE(SUM(rr.total_distance), 0) as total_distance
            FROM battle_result br
            JOIN running_result rr ON br.running_result_id = rr.id
            WHERE rr.is_deleted = false
              AND br.is_deleted = false
            GROUP BY
                CASE
                    WHEN br.current_rating < 800 THEN '거북이'
                    WHEN br.current_rating < 1200 THEN '토끼'
                    WHEN br.current_rating < 1600 THEN '사슴'
                    WHEN br.current_rating < 2000 THEN '표범'
                    WHEN br.current_rating < 2400 THEN '호랑이'
                    ELSE '장산범'
                END
        ) AS subquery
        ORDER BY
            CASE tier_name
                WHEN '거북이' THEN 1
                WHEN '토끼' THEN 2
                WHEN '사슴' THEN 3
                WHEN '표범' THEN 4
                WHEN '호랑이' THEN 5
                WHEN '장산범' THEN 6
            END
        """, nativeQuery = true)
    List<Object[]> sumByTier();

    /**
     * 러닝 타입별 분포
     */
    @Query("""
        SELECT r.runningType, COUNT(*)
        FROM RunningResult r
        WHERE r.isDeleted = false
        GROUP BY r.runningType
        """)
    List<Object[]> countByRunningType();

    /**
     * 시간대별 러닝량 (0-23시)
     */
    @Query(value = """
        SELECT 
            EXTRACT(HOUR FROM started_at)::int as hour,
            COUNT(*) as count,
            COALESCE(SUM(total_distance), 0) as total_distance
        FROM running_result
        WHERE is_deleted = false
        GROUP BY EXTRACT(HOUR FROM started_at)
        ORDER BY hour
        """, nativeQuery = true)
    List<Object[]> sumByHour();

    /**
     * 티어별 평균 페이스 Note: Tier enum은 한글 이름을 사용하므로 결과는 문자열로 반환됨
     */
    @Query(value = """
        SELECT tier_name, avg_pace, sample_count
        FROM (
            SELECT 
                CASE 
                    WHEN br.current_rating < 800 THEN '거북이'
                    WHEN br.current_rating < 1200 THEN '토끼'
                    WHEN br.current_rating < 1600 THEN '사슴'
                    WHEN br.current_rating < 2000 THEN '표범'
                    WHEN br.current_rating < 2400 THEN '호랑이'
                    ELSE '장산범'
                END as tier_name,
                COALESCE(AVG(rr.avg_pace), 0) as avg_pace,
                COUNT(*) as sample_count
            FROM battle_result br
            JOIN running_result rr ON br.running_result_id = rr.id
            WHERE rr.is_deleted = false
              AND br.is_deleted = false
              AND rr.avg_pace IS NOT NULL
            GROUP BY
                CASE 
                    WHEN br.current_rating < 800 THEN '거북이'
                    WHEN br.current_rating < 1200 THEN '토끼'
                    WHEN br.current_rating < 1600 THEN '사슴'
                    WHEN br.current_rating < 2000 THEN '표범'
                    WHEN br.current_rating < 2400 THEN '호랑이'
                    ELSE '장산범'
                END
        ) AS subquery
        ORDER BY 
            CASE tier_name
                WHEN '거북이' THEN 1
                WHEN '토끼' THEN 2
                WHEN '사슴' THEN 3
                WHEN '표범' THEN 4
                WHEN '호랑이' THEN 5
                WHEN '장산범' THEN 6
            END
        """, nativeQuery = true)
    List<Object[]> avgPaceByTier();

    /**
     * 주별 추이 (최근 12주)
     */
    @Query(value = """
        SELECT 
            DATE_TRUNC('week', started_at)::date as week_start,
            COUNT(*) as count,
            COALESCE(SUM(total_distance), 0) as total_distance,
            COALESCE(SUM(total_time), 0) as total_time
        FROM running_result
        WHERE is_deleted = false
          AND run_status IN ('COMPLETED', 'TIME_OUT')
          AND started_at >= CURRENT_DATE - INTERVAL '12 weeks'
        GROUP BY DATE_TRUNC('week', started_at)
        ORDER BY week_start DESC
        LIMIT 12
        """, nativeQuery = true)
    List<Object[]> sumWeeklyTrend();

    /**
     * 월별 추이 (최근 12개월)
     */
    @Query(value = """
        SELECT 
            DATE_TRUNC('month', started_at)::date as month,
            COUNT(*) as count,
            COALESCE(SUM(total_distance), 0) as total_distance,
            COALESCE(SUM(total_time), 0) as total_time
        FROM running_result
        WHERE is_deleted = false
          AND run_status IN ('COMPLETED', 'TIME_OUT')
          AND started_at >= DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '12 months'
        GROUP BY DATE_TRUNC('month', started_at)
        ORDER BY month DESC
        LIMIT 12
        """, nativeQuery = true)
    List<Object[]> sumMonthlyTrend();

    /**
     * 유저 페이스 분포 (구간별)
     */
    @Query(value = """
        SELECT pace_range, user_count
        FROM (
            SELECT
                CASE
                    WHEN avg_pace < 3.0 THEN '~3:00'
                    WHEN avg_pace < 4.0 THEN '3:00-4:00'
                    WHEN avg_pace < 5.0 THEN '4:00-5:00'
                    WHEN avg_pace < 6.0 THEN '5:00-6:00'
                    WHEN avg_pace < 7.0 THEN '6:00-7:00'
                    WHEN avg_pace < 8.0 THEN '7:00-8:00'
                    WHEN avg_pace < 9.0 THEN '8:00-9:00'
                    WHEN avg_pace < 10.0 THEN '9:00-10:00'
                    ELSE '10:00~'
                END as pace_range,
                COUNT(DISTINCT user_id) as user_count
            FROM running_result
            WHERE is_deleted = false
              AND run_status IN ('COMPLETED', 'TIME_OUT')
              AND avg_pace IS NOT NULL
            GROUP BY
                CASE
                    WHEN avg_pace < 3.0 THEN '~3:00'
                    WHEN avg_pace < 4.0 THEN '3:00-4:00'
                    WHEN avg_pace < 5.0 THEN '4:00-5:00'
                    WHEN avg_pace < 6.0 THEN '5:00-6:00'
                    WHEN avg_pace < 7.0 THEN '6:00-7:00'
                    WHEN avg_pace < 8.0 THEN '7:00-8:00'
                    WHEN avg_pace < 9.0 THEN '8:00-9:00'
                    WHEN avg_pace < 10.0 THEN '9:00-10:00'
                    ELSE '10:00~'
                END
        ) AS subquery
        ORDER BY
            CASE pace_range
                WHEN '~3:00' THEN 1
                WHEN '3:00-4:00' THEN 2
                WHEN '4:00-5:00' THEN 3
                WHEN '5:00-6:00' THEN 4
                WHEN '6:00-7:00' THEN 5
                WHEN '7:00-8:00' THEN 6
                WHEN '8:00-9:00' THEN 7
                WHEN '9:00-10:00' THEN 8
                WHEN '10:00~' THEN 9
            END
        """, nativeQuery = true)
    List<Object[]> distributionByPace();

    /**
     * 코스별 통계 (상위 20개)
     */
    @Query(value = """
        SELECT 
            c.id as course_id,
            c.title as course_name,
            COUNT(*) as usage_count
        FROM running_result rr
        JOIN course c ON rr.course_id = c.id
        WHERE rr.is_deleted = false
          AND rr.run_status IN ('COMPLETED', 'TIME_OUT')
          AND rr.course_id IS NOT NULL
        GROUP BY c.id, c.title
        ORDER BY usage_count DESC
        LIMIT 20
        """, nativeQuery = true)
    List<Object[]> sumByCourse();
}
