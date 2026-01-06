package com.multi.runrunbackend.domain.challenge.repository;

import com.multi.runrunbackend.domain.challenge.entity.Challenge;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

/**
 *
 * @author : kimyongwon
 * @description : 챌린지 엔티티에 대한 처리를 담당하는 Repository 인터페이스
 * @filename : ChallengeRepository
 * @since : 25. 12. 21. 오후 9:28 일요일
 */
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    @Query("SELECT c FROM Challenge c WHERE c.endDate < :date")
        // @SQLRestriction 덕분에 is_deleted=false 자동 적용됨
    List<Challenge> findExpiredChallenges(@Param("date") LocalDate date);

    @Query(value = "SELECT * FROM challenge", nativeQuery = true)
    List<Challenge> findAllWithDeleted();

}