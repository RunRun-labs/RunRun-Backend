package com.multi.runrunbackend.domain.challenge.repository;

import com.multi.runrunbackend.domain.challenge.entity.UserChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 *
 * @author : kimyongwon
 * @description : 사용자의 참여 정보를 조회하기 위한 리포지토리
 * @filename : UserChallengeRepository
 * @since : 25. 12. 21. 오후 9:20 일요일
 */
public interface UserChallengeRepository extends JpaRepository<UserChallenge, Long> {

    // 특정 사용자의 챌린지 참여 이력 조회
    List<UserChallenge> findByUserId(Long userId);

    // 특정 챌린지의 참여자 수 카운트
    long countByChallengeId(Long challengeId);

}