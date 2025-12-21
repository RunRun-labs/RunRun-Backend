package com.multi.runrunbackend.domain.challenge.repository;

import com.multi.runrunbackend.domain.challenge.entity.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author : kimyongwon
 * @description : 챌린지 엔티티에 대한 처리를 담당하는 Repository 인터페이스
 * @filename : ChallengeRepository
 * @since : 25. 12. 21. 오후 9:28 일요일
 */
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

}