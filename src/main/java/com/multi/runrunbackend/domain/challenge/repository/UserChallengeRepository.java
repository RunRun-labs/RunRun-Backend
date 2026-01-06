package com.multi.runrunbackend.domain.challenge.repository;

import com.multi.runrunbackend.domain.challenge.constant.UserChallengeStatus;
import com.multi.runrunbackend.domain.challenge.entity.UserChallenge;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
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
    // UserChallenge와 User를 조인하여, User의 isDeleted가 false인 경우만 카운트합니다.
    @Query("SELECT COUNT(uc) FROM UserChallenge uc JOIN uc.user u " +
            "WHERE uc.challenge.id = :challengeId AND u.isDeleted = false")
    long countByChallengeId(@Param("challengeId") Long challengeId);

    // 종료일이 지났는데 아직 상태가 (JOINED, IN_PROGRESS)인 것들 조회 -> 실패 처리
    @Query("SELECT uc FROM UserChallenge uc JOIN uc.challenge c " +
            "WHERE uc.status IN :statuses AND c.endDate < :criteriaDate")
    List<UserChallenge> findChallengesToFail(
            @Param("statuses") Collection<UserChallengeStatus> statuses,
            @Param("criteriaDate") LocalDate criteriaDate
    );

    // 시작일이 되었는데 아직 상태가 JOINED인 것들 조회 -> 시작(IN_PROGRESS) 처리
    @Query("SELECT uc FROM UserChallenge uc JOIN uc.challenge c " +
            "WHERE uc.status = :status AND c.startDate <= :criteriaDate")
    List<UserChallenge> findChallengesToStart(
            @Param("status") UserChallengeStatus status,
            @Param("criteriaDate") LocalDate criteriaDate
    );
}