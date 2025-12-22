package com.multi.runrunbackend.domain.match.repository;

import com.multi.runrunbackend.domain.match.constant.SessionStatus;
import com.multi.runrunbackend.domain.match.entity.MatchSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * @author : changwoo
 * @description : Please explain the class!!!
 * @filename : MatchSessionRepository
 * @since : 2025-12-20 토요일
 */
public interface MatchSessionRepository extends JpaRepository<MatchSession, Long> {

  @Modifying
  @Query("UPDATE MatchSession ms SET ms.status = :status WHERE ms.id = :sessionId")
  int updateStatus(@Param("sessionId") Long sessionId, @Param("status") SessionStatus status);
}
