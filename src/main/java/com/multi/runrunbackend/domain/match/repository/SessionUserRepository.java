package com.multi.runrunbackend.domain.match.repository;

import com.multi.runrunbackend.domain.match.entity.SessionUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * @author : changwoo
 * @description : Please explain the class!!!
 * @filename : SessionUserRepository
 * @since : 2025-12-20 토요일
 */
public interface SessionUserRepository extends JpaRepository<SessionUser, Long> {

  @Query("SELECT su FROM SessionUser su " +
      "JOIN FETCH su.user " +
      "WHERE su.matchSession.id = :sessionId AND su.isDeleted = false")
  List<SessionUser> findActiveUsersBySessionId(@Param("sessionId") Long sessionId);

  @Query("SELECT su FROM SessionUser su " +
      "WHERE su.matchSession.id = :sessionId AND su.user.id = :userId AND su.isDeleted = false")
  Optional<SessionUser> findBySessionIdAndUserId(
      @Param("sessionId") Long sessionId,
      @Param("userId") Long userId);

  @Query("SELECT COUNT(su) FROM SessionUser su " +
      "WHERE su.matchSession.id = :sessionId AND su.isDeleted = false")
  Long countActiveUsersBySessionId(@Param("sessionId") Long sessionId);

  @Query("SELECT COUNT(su) FROM SessionUser su " +
      "WHERE su.matchSession.id = :sessionId AND su.isDeleted = false AND su.isReady = true")
  Long countReadyUsersBySessionId(@Param("sessionId") Long sessionId);

  @Modifying
  @Query("UPDATE SessionUser su SET su.isReady = :isReady " +
      "WHERE su.matchSession.id = :sessionId AND su.user.id = :userId")
  int updateReadyStatus(
      @Param("sessionId") Long sessionId,
      @Param("userId") Long userId,
      @Param("isReady") boolean isReady);

  @Modifying
  @Query("UPDATE SessionUser su SET su.isDeleted = true " +
      "WHERE su.matchSession.id = :sessionId AND su.user.id = :userId")
  int softDeleteBySessionIdAndUserId(
      @Param("sessionId") Long sessionId,
      @Param("userId") Long userId);

  /**
   * 마지막 읽은 시간 업데이트
   */
  @Modifying
  @Query("UPDATE SessionUser su SET su.lastReadAt = :lastReadAt " +
      "WHERE su.matchSession.id = :sessionId AND su.user.id = :userId")
  int updateLastReadAt(
      @Param("sessionId") Long sessionId,
      @Param("userId") Long userId,
      @Param("lastReadAt") java.time.LocalDateTime lastReadAt);

  /**
   * 유저가 참여 중인 세션 목록 조회 (오프라인 세션만)
   */
  @Query("SELECT su FROM SessionUser su " +
      "JOIN FETCH su.matchSession ms " +
      "LEFT JOIN FETCH ms.recruit r " +
      "WHERE su.user.id = :userId " +
      "AND su.isDeleted = false " +
      "AND ms.type = 'OFFLINE' " +
      "ORDER BY COALESCE(r.meetingAt, ms.createdAt) ASC")
  List<SessionUser> findMyOfflineSessions(@Param("userId") Long userId);
}
