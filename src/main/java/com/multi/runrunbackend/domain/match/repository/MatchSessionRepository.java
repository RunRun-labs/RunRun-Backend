package com.multi.runrunbackend.domain.match.repository;

import com.multi.runrunbackend.domain.match.entity.MatchSession;
import com.multi.runrunbackend.domain.recruit.entity.Recruit;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author : KIMGWANGHO
 * @description : 매치 세션(MatchSession) 엔티티의 데이터베이스 접근 및 관리를 담당하는 리포지토리
 * @filename : MatchSessionRepository
 * @since : 2025-12-21 일요일
 */
public interface MatchSessionRepository extends JpaRepository<MatchSession, Long> {

  boolean existsByRecruit(Recruit recruit);

  Optional<MatchSession> findByRecruit(Recruit recruit);
}
