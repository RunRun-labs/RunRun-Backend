package com.multi.runrunbackend.domain.match.repository;

import com.multi.runrunbackend.domain.match.entity.SessionUser;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author : KIMGWANGHO
 * @description : SessionUser 엔티티의 데이터베이스 접근 및 관리를 담당하는 리포지토리
 * @filename : SessionUserRepository
 * @since : 2025-12-21 일요일
 */
public interface SessionUserRepository extends JpaRepository<SessionUser, Long> {

}
