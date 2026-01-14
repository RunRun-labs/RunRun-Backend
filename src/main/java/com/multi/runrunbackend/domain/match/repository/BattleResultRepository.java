package com.multi.runrunbackend.domain.match.repository;

import com.multi.runrunbackend.common.constant.DistanceType;
import com.multi.runrunbackend.domain.match.dto.res.BattleResultResDto;
import com.multi.runrunbackend.domain.match.entity.BattleResult;
import com.multi.runrunbackend.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * @author : chang
 * @description : 배틀 결과 Repository
 * @filename : BattleResultRepository
 * @since : 2025-12-30
 */
@Repository
public interface BattleResultRepository extends JpaRepository<BattleResult, Long> {

    long countByUserAndDistanceType(User user, DistanceType distanceType);

    boolean existsBySession_IdAndUser_Id(Long sessionId, Long userId);

    @Query("""
                SELECT new com.multi.runrunbackend.domain.match.dto.res.BattleResultResDto(
                    br.id,
                    s.id,
                    br.distanceType,
                    br.createdAt,
                    br.ranking,
                    rr.totalTime,
                    rr.avgPace,
                    rr.totalDistance,
                    br.previousRating,
                    br.currentRating,
                    (br.currentRating-br.previousRating),
                    s.type,
                    s.status,
                    (SELECT COUNT(br2) FROM BattleResult br2 WHERE br2.session.id = s.id)
                )
                FROM BattleResult br
                JOIN br.session s
                JOIN br.runningResult rr
                WHERE br.user.id = :userId
                AND (:#{#distanceType == null} = true OR br.distanceType = :distanceType)
                AND (:#{#from == null} = true OR br.createdAt >= :from)
                AND (:#{#to == null} = true OR br.createdAt <= :to)
                ORDER BY br.createdAt DESC
            """)
    Slice<BattleResultResDto> findMyBattleResults(
            @Param("userId") Long userId,
            @Param("distanceType") DistanceType distanceType,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );


    @Query("""
              select br
              from BattleResult br
              join fetch br.user u
              join fetch br.runningResult rr
              where br.session.id = :sessionId
              order by br.ranking asc
            """)
    List<BattleResult> findSessionBattleResultsEntity(@Param("sessionId") Long sessionId);

    /**
     * 마이페이지 러닝기록 목록(ONLINEBATTLE)의 등수(ranking) 일괄 조회용.
     * - key: runningResultId
     * - value: ranking
     */
    @Query("""
            select br.runningResult.id, br.ranking
            from BattleResult br
            where br.runningResult.id in :runningResultIds
            """)
    List<Object[]> findRankingByRunningResultIds(@Param("runningResultIds") Set<Long> runningResultIds);
}
