package com.multi.runrunbackend.domain.crew.repository;

import com.multi.runrunbackend.domain.crew.entity.CrewActivityUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author : BoKyung
 * @description : 크루 활동 참여자 Repository
 * @filename : CrewActivityUserRepository
 * @since : 25. 1. 11. 일요일
 */
public interface CrewActivityUserRepository extends JpaRepository<CrewActivityUser, Long> {

    /**
     * @description : 특정 활동의 참여자 목록 조회
     */
    List<CrewActivityUser> findByCrewActivityId(Long activityId);

    /**
     * @description : 특정 활동의 참여자 전체 삭제
     */
    @Modifying
    @Transactional
    void deleteByCrewActivityId(Long activityId);
}
