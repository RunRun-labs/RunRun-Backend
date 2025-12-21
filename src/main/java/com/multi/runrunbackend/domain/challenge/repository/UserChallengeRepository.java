package com.multi.runrunbackend.domain.challenge.repository;

import com.multi.runrunbackend.domain.challenge.entity.UserChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 *
 * @author : kimyongwon
 * @description : 사용자의 참여 정보를 조회하기 위한 리포지토리
 * @filename : UserChallengeRepository
 * @since : 25. 12. 21. 오후 9:20 일요일
 */
public interface UserChallengeRepository extends JpaRepository<UserChallenge, Long> {

    // 특정 사용자의 모든 챌린지 참여 이력 조회
    @Query("SELECT uc FROM UserChallenge uc JOIN FETCH uc.challenge WHERE uc.user.id = :userId")
    List<UserChallenge> findByUserId(@Param("userId") Long userId);
}